package vkm.vkm

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.SeekBar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.*

object MusicPlayer: SeekBar.OnSeekBarChangeListener {
    private var mp: MediaPlayer? = null
    lateinit var context: Context
    var resource: String? = null
    var trackLength = 0
    var currentlyAnimatedView: View? = null
    var isLoading = false

    // TODO make slider a fragment

    fun isPlaying(): Boolean {
        return mp?.isPlaying ?: false
    }

    fun play(resource: String, viewToPlay: View?, onStop: () -> Unit = {}, onStart: () -> Unit) {
        try {
            "Starting ${resource} playing".log()
            initMediaPlayer()
            this.resource = resource
            mp?.apply {
                reset()
                if (resource.startsWith("http")) {
                    setDataSource(resource)
                } else {
                    setDataSource(context, Uri.parse(DownloadManager.getDownloadDir().resolve(resource).path))
                }
                currentlyAnimatedView = viewToPlay
                setOnPreparedListener { player ->
                    isLoading = false
                    player.setOnCompletionListener { onStop.invoke() }
                    trackLength = player.duration
                    onStart.invoke()
                    runSeekBarUpdate(currentlyAnimatedView)
                    player.start()
                }

                setOnErrorListener { player, what, extra ->
                    isLoading = false
                    trackLength = 0
                    onStop.invoke()
                    return@setOnErrorListener true
                }

                isLoading = true
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("vkm", "Unable to play track", e)
            "Unable to play track".toast(context)
            trackLength = 0
        }
    }

    fun pause() {
        mp?.takeIf { mp?.isPlaying == true }?.pause()
    }

    fun stop(soft: Boolean = false) {
        if (isPlaying()) {
            mp?.stop()
            if (!soft) { destroy() }
            resource = null
            currentlyAnimatedView = null
            "Stopping music player".log()
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

    fun runSeekBarUpdate(view: View?) {
        launch(UI) {
            while (isPlaying() && currentlyAnimatedView == view) {
                view?.bind<SeekBar>(R.id.seekBar)?.progress = (mp?.currentPosition ?: 0) / 1000
                delay(1000)
            }
        }
    }

    fun isCurrentTrack(item: Composition) = item.url == MusicPlayer.resource || item.fileName() == MusicPlayer.resource
}