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
import vkm.vkm.utils.*
import java.util.concurrent.atomic.AtomicBoolean

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
    var onPlay: () -> Unit = {}
    var onStop: () -> Unit = {}
    var onPause: () -> Unit = {}
    var onLoaded: () -> Unit = {}
    var onProgressUpdate: () -> Unit = {}

    var isLoading = AtomicBoolean(false)
    var isPaused = AtomicBoolean(false)

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

    private fun startTrackProgressTrack(onUpdate: () -> Unit = {}) {
        progressUpdateJob?.cancel()
        onProgressUpdate = onUpdate

        progressUpdateJob = launch(CommonPool) {
            while (true) {
                if (trackLength > 0) {
                    trackProgress = mp.currentPosition * 100 / trackLength
                }
                delay(1000)
                onProgressUpdate()
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
        onProgressUpdate = {}
    }

    class MusicPlayerController : Binder() {
        fun getService(): MusicPlayService {
            return instance
        }
    }

    fun play(composition: Composition? = null, onProgressUpdate: () -> Unit = this.onProgressUpdate) {
        launch(CommonPool) {
            val compositionToPlay = composition ?: (currentComposition ?: playList.firstOrNull())
            isPaused.compareAndSet(true, compositionToPlay.equalsTo(currentComposition))

            "Starting to play ${compositionToPlay?.fileName()}".log()


            playList.takeIf { !isPaused.get() && compositionToPlay != null }?.let {
                isLoading.compareAndSet(false, true)
                currentComposition = try {
                    fetchComposition(compositionToPlay)
                } catch (e: Exception) {
                    "Failed to load track ${compositionToPlay?.fileName()}".logE(e)
                    null
                }
                isLoading.compareAndSet(true, false)
                "Composition fetched ${currentComposition?.fileName()}".log()
                onLoaded()
            }

            currentComposition?.let {
                "Starting media player for ${currentComposition?.fileName()}".log()
                mp.start()
                mp.setOnCompletionListener { next() }
                startTrackProgressTrack(onProgressUpdate)
                onPlay()
            }
        }
    }

    fun pause() {
        mp.takeIf { mp.isPlaying }?.pause()
        isPaused.compareAndSet(false, true)
        onPause()
    }

    fun skipTo(time: Int) = mp.seekTo(time)

    fun stop() {
        mp.stop()
        mp.reset()
        resetTrack()
        currentComposition = null
        stopProgressUpdate()
        onStop()
    }

    fun isPlaying() = mp.isPlaying

    fun next() = getSibling(true)
    fun previous() = getSibling(false)

    fun isCurrentTrack(item: Composition) = (item.url.isNotEmpty() && item.url == currentComposition?.url) || item.fileName() == currentComposition?.fileName()

    private fun getSibling(next: Boolean) {
        mp.stop()
        mp.reset()
        resetTrack()

        if (playList.isEmpty()) {
            stop()
            return
        }

        launch(CommonPool) {
            var index = playList.indexOf(currentComposition)
            var steps = 0
            do {
                index = (playList.size + index + if (next) 1 else -1) % playList.size
                steps++
                currentComposition = try { fetchComposition(playList.getOrNull(index)) } catch (e: Exception) { null }
            } while (currentComposition != null && steps < 20)
            if (currentComposition != null) {
                play(currentComposition)
                onPlay()
            }
        }
    }

    suspend private fun fetchComposition(composition: Composition?): Composition? {
        "Fetching composition ${composition?.fileName()}".log()
        if (composition == null) { return null }

        val resource = if (composition.hash.isEmpty()) composition.url else DownloadManager.getDownloadDir().resolve(composition.fileName()).path

        if (composition.url.startsWith("http")) {
            mp.setDataSource(resource)
        } else {
            mp.setDataSource(applicationContext, Uri.parse(resource))
        }

        mp.setOnErrorListener { _, _, _ ->
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
