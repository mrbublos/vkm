package vkm.vkm

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class MusicPlayService : Service() {

    companion object {
        private lateinit var instance: MusicPlayService
    }

    private val mp = MediaPlayer()
    var currentComposition: Composition? = null
    var playList: List<Composition> = mutableListOf()
    private val binder = MusicPlayerController()
    var trackLength = 0
    var trackProgress = 0
    var onPlay: () -> Any? = {}
    var onProgressUpdate: () -> Any? = {}

    private var progressUpdateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder {
        "Service bound".log()
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        "Service created".log()
    }

    fun startTrackProgressTrack(onUpdate: () -> Any? = {}) {
        progressUpdateJob?.cancel()
        onProgressUpdate = onUpdate

        progressUpdateJob = launch(CommonPool) {
            while (true) {
                if (trackLength > 0) { trackProgress = (mp.currentPosition / 1000) / trackLength }
                delay(1000)
                onProgressUpdate()
            }
        }
    }

    fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
        onProgressUpdate = {}
    }

    class MusicPlayerController : Binder() {
        fun getService(): MusicPlayService {
            return instance
        }
    }

    fun play(onUpdate: () -> Any? = {}) {
        launch(CommonPool) {
            playList.takeIf { currentComposition == null }?.firstOrNull()?.let { currentComposition = fetchComposition(it) }

            currentComposition?.let {
                mp.start()
                mp.setOnCompletionListener { next() }
                startTrackProgressTrack(onUpdate)
                onPlay()
            }
        }
    }

    fun pause() {
        mp.takeIf { mp.isPlaying }?.pause()
    }

    fun skipTo(time: Int) {
        mp.seekTo(time)
    }

    fun stop() {
        mp.stop()
        resetTrack()
        currentComposition = null
        stopProgressUpdate()
    }

    fun isPlaying() :Boolean {
        return mp.isPlaying
    }

    fun next() = getSibling(true)
    fun previous() = getSibling(false)

    fun isCurrentTrack(item: Composition) = item.url == currentComposition?.url || item.fileName() == currentComposition?.fileName()

    private fun getSibling(next: Boolean) {
        mp.stop()
        resetTrack()

        if (playList.isEmpty()) {
            stop()
            return
        }

        launch(CommonPool) {
            val index = playList.indexOf(currentComposition)
            currentComposition = fetchComposition(playList.getOrNull(index + if (next) 1 else -1))
            onPlay()
        }
    }

    suspend private fun fetchComposition(composition: Composition?): Composition? {
        if (composition == null) { return null }

        val resource = if (composition.hash.isEmpty()) composition.url else DownloadManager.getDownloadDir().resolve(composition.fileName()).path

        if (composition.url.startsWith("http")) {
            mp.setDataSource(resource)
        } else {
            mp.setDataSource(applicationContext, Uri.parse(resource))
        }

        mp.setOnErrorListener { mp, what, extra ->
            next()
            return@setOnErrorListener true
        }

        mp.loadAsync()
        trackLength = mp.duration
        trackProgress = 0
        return composition
    }


    private fun resetTrack() {
        trackLength = 0
        trackProgress = 0
    }

}
