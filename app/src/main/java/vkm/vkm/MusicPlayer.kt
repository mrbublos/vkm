package vkm.vkm

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.SeekBar

object MusicPlayer: SeekBar.OnSeekBarChangeListener {
    private var mp: MediaPlayer? = null
    lateinit var context: Context
    var resource: String? = null
    var trackLength = 0
    var currentSeekBar: SeekBar? = null

    // TODO make slider a fragment

    fun isPlaying(): Boolean {
        return mp?.isPlaying ?: false
    }

    fun play(resource: String, seekBar: SeekBar?, onStop: () -> Unit = {}): Int? {
        try {
            initMediaPlayer()
            this.resource = resource
            mp?.apply {
                reset()
                if (resource.startsWith("http")) {
                    setDataSource(resource)
                } else {
                    setDataSource(context, Uri.parse(DownloadManager.getDownloadDir().resolve(resource).path))
                }
                prepare()
                setOnCompletionListener { onStop.invoke() }
                currentSeekBar = seekBar
                runSeekBarUpdate(seekBar)
                start()
            }
        } catch (e: Exception) {
            Log.e("vkm", "Unable to play track", e)
            "Unable to play track".toast(context)
            trackLength = 0
            return trackLength
        }
        trackLength = mp?.duration ?: 0
        return trackLength
    }

    fun pause() {
        mp?.takeIf { mp?.isPlaying == true }?.pause()
    }

    fun stop(soft: Boolean = false) {
        if (isPlaying()) {
            mp?.stop()
            (MusicPlayer.currentSeekBar?.parent?.parent as View?)?.bind<View>(R.id.audioControl)?.callOnClick()
            currentSeekBar = null
            if (!soft) { destroy() }
        }
    }

    private fun destroy() {
        try {
            mp?.release()
            mp = null
        } catch (e: Exception) {
            Log.e("vkm", "Error releasing media player", e)
        }
    }

    private fun initMediaPlayer() {
        if (mp == null) {
            mp = MediaPlayer()
            mp?.setOnCompletionListener { stop() }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        seekBar?.takeIf { fromUser && isPlaying() }?.let {
            mp?.seekTo(progress * 1000)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    private fun runSeekBarUpdate(seekBar: SeekBar?) {
        Handler().postDelayed({
                seekBar?.takeIf { MusicPlayer.isPlaying() && seekBar == currentSeekBar }?.let {
                    it.progress = (MusicPlayer.mp?.currentPosition ?: 0) / 1000
                    runSeekBarUpdate(seekBar)
                }
            }, 1000)
    }
}