package vkm.vkm

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

object MusicPlayer {

    private var mp: MediaPlayer? = null
    lateinit var context: Context

    fun isPlaying(): Boolean {
        return mp?.isPlaying ?: false
    }

    fun play(resource: String, onStop: () -> Unit = {}): Boolean {
        try {
            initMediaPlayer()
            mp?.apply {
                if (resource.startsWith("http")) {
                    setDataSource(resource)
                } else {
                    setDataSource(context, Uri.parse(DownloadManager.getDownloadDir().resolve(resource).path))
                }
                prepare()
                setOnCompletionListener { onStop.invoke() }
                start()
            }
        } catch (e: Exception) {
            Log.e("vkm", "Unable to play track", e)
            "Unable to play track".toast(context)
            return false
        }
        return true
    }

    fun pause() {
        mp?.takeIf { mp?.isPlaying == true }?.pause()
    }

    fun stop() {
        mp?.stop()
        releaseMP()
    }

    private fun releaseMP() {
        try {
            mp?.release()
            mp = null
        } catch (e: Exception) {
            Log.e("vkm", "Error releasing media player", e)
        }
    }

    fun initMediaPlayer() {
        if (mp == null) {
            mp = MediaPlayer()
            mp?.setOnCompletionListener { stop() }
        }
    }
}