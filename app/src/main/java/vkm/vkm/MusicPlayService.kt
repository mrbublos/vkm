package vkm.vkm

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.NotificationCompat
import android.widget.RemoteViews
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class MusicPlayService : Service() {

    companion object {
        private lateinit var instance: MusicPlayService
    }

    var currentComposition: Composition? = null
    var playList: List<Composition> = mutableListOf()
    var trackLength = 0
    var trackProgress = 0

    var onPlay: () -> Unit = {}
    var onStop: () -> Unit = {}
    var onPause: () -> Unit = {}
    var onLoaded: () -> Unit = {}
    private var onProgressUpdate: () -> Unit = {}

    var isLoading = AtomicBoolean(false)
    private var isPaused = AtomicBoolean(false)

    private val mp = MediaPlayer()
    private val binder = MusicPlayerController()
    private var progressUpdateJob: Job? = null
    private val playbackStateBuilder = PlaybackStateCompat.Builder()
    private val mediaMetadataBuilder = MediaMetadataCompat.Builder()
    lateinit private var mediaSession: MediaSessionCompat
    lateinit var notificationManager: NotificationManager

    private val playerControlsCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() { play() }
        override fun onPause() { play() }
        override fun onSkipToNext() { next() }
        override fun onSkipToPrevious() { previous() }
    }

    override fun onBind(intent: Intent?): IBinder {
        "Service bound".log()
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) { return START_NOT_STICKY }

        when (intent.action) {
            "next" -> next()
            "previous" -> previous()
            "pause" -> if (isPlaying()) pause() else play()
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        "Service created".log()
        mediaSession = MediaSessionCompat(baseContext, "vkm")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancelAll()
    }

    private fun updateMediaSession() {
        val mediaMetadata = mediaMetadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentComposition?.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentComposition?.name)
                .build()
        val playbackState = playbackStateBuilder
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build()
        mediaSession.setMetadata(mediaMetadata)
        mediaSession.setPlaybackState(playbackState)
    }

    private fun createNotification() {
//        if (currentComposition == null) { return }

        val nextPendingIntent = PendingIntent.getService(this, 1, Intent(this, MusicPlayService::class.java).setAction("next"), PendingIntent.FLAG_UPDATE_CURRENT)
        val prevPendingIntent = PendingIntent.getService(this, 2, Intent(this, MusicPlayService::class.java).setAction("previous"), PendingIntent.FLAG_UPDATE_CURRENT)
        val pausePendingIntent = PendingIntent.getService(this, 3, Intent(this, MusicPlayService::class.java).setAction("pause"), PendingIntent.FLAG_UPDATE_CURRENT)


        val playerView = RemoteViews(packageName, R.layout.notification_player)
        playerView.setTextViewText(R.id.name, currentComposition?.name ?: "test")
        playerView.setTextViewText(R.id.artist, currentComposition?.artist ?: "test")
        playerView.setImageViewResource(R.id.pause, if (isPlaying()) R.drawable.ic_pause_player_black else R.drawable.ic_play_player_black )
        playerView.setOnClickPendingIntent(R.id.pause, pausePendingIntent)
        playerView.setOnClickPendingIntent(R.id.nextTrack, nextPendingIntent)

        val notification = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_vkm_main)
                .setAutoCancel(false)
                .setPriority(PRIORITY_MAX)
                .setCategory(CATEGORY_SERVICE)
                .setVisibility(VISIBILITY_PUBLIC)
                .setContent(playerView)
//                .addAction(R.drawable.ic_previous_player, "Previous", prevPendingIntent) // #0
//                .addAction(R.drawable.ic_pause_player, "Pause", pausePendingIntent)  // #1
//                .addAction(R.drawable.ic_next_player, "Next", nextPendingIntent)
//                .setStyle(NotificationCompat.MediaStyle()
//                        .setMediaSession(mediaSession.sessionToken))
                .build()

        notificationManager.notify(1, notification)
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
        fun getService(): MusicPlayService = instance
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
                createNotification()
                onPlay()
            }
        }
    }

    fun pause() {
        mp.takeIf { mp.isPlaying }?.pause()
        isPaused.compareAndSet(false, true)
        onPause()
        createNotification()
    }

    fun skipTo(time: Int) = mp.seekTo(time)

    fun stop() {
        mp.stop()
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
                currentComposition = try {
                    fetchComposition(playList.getOrNull(index))
                } catch (e: Exception) {
                    null
                }
                "Next sibling is ${currentComposition?.fileName()}".log()
            } while (currentComposition == null && steps < 20)

            if (currentComposition != null) {
                play(currentComposition)
                onPlay()
                createNotification()
            }
        }
    }

    suspend private fun fetchComposition(composition: Composition?): Composition? {
        "Fetching composition ${composition?.fileName()}".log()
        if (composition == null) {
            return null
        }

        val resource = if (composition.hash.isEmpty()) composition.url else DownloadManager.getDownloadDir().resolve(composition.fileName())

        mp.reset()
        if (composition.url.startsWith("http")) {
            mp.setDataSource(resource as String)
        } else {
            mp.setDataSource(applicationContext, Uri.fromFile(resource as File))
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
