package vkm.vkm

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.*
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

    private var isLoading = AtomicBoolean(false)
    private var isPaused = AtomicBoolean(false)

    private val mp = MediaPlayer()
    private val binder = MusicPlayerController()
    private var progressUpdateJob: Job? = null
    private val playbackStateBuilder = PlaybackStateCompat.Builder()
    private val mediaMetadataBuilder = MediaMetadataCompat.Builder()

    private val becomeNoisyListener = BecomeNoisyListener(this)
    private val audioFocusListener = AudioFocusChangeListener(this)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiLock: WifiManager.WifiLock

    private val playerControlsCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() { play() }
        override fun onPause() { pause() }
        override fun onSkipToNext() { next() }
        override fun onSkipToPrevious() { previous() }
    }

    override fun onBind(intent: Intent?): IBinder {
        "Service bound".log()
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        // TODO audio_becoming_noisy handle
        // wifi lock
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
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,"VkmPlayerWifiLock")
        wifiLock.setReferenceCounted(false)

        mediaSession = MediaSessionCompat(baseContext, "vkm")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(playerControlsCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancelAll()
        mediaSession.release()
        mp.release()
    }

    private fun updateMediaSession(state: Int) {
        val mediaMetadata = mediaMetadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentComposition?.artist ?: "No Artist")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentComposition?.name ?: "No Title")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, trackLength.toLong())
                .build()
        val playbackState = playbackStateBuilder
                .setState(state, 0, 1f)
                .setActions(ACTION_PLAY_PAUSE or ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS)
                .build()
        mediaSession.setMetadata(mediaMetadata)
        mediaSession.setPlaybackState(playbackState)
    }

//    private fun createNotification(): Notification {
//        val nextPendingIntent = PendingIntent.getService(this, 1, Intent(this, MusicPlayService::class.java).setAction("next"), PendingIntent.FLAG_UPDATE_CURRENT)
//        val pausePendingIntent = PendingIntent.getService(this, 3, Intent(this, MusicPlayService::class.java).setAction("pause"), PendingIntent.FLAG_UPDATE_CURRENT)
//
//
//        val playerView = RemoteViews(packageName, R.layout.notification_player)
//        playerView.setTextViewText(R.id.name, currentComposition?.name ?: "test")
//        playerView.setTextViewText(R.id.artist, currentComposition?.artist ?: "test")
//        playerView.setImageViewResource(R.id.pause, if (isPlaying()) R.drawable.ic_pause_player_black else R.drawable.ic_play_player_black)
//        playerView.setOnClickPendingIntent(R.id.pause, pausePendingIntent)
//        playerView.setOnClickPendingIntent(R.id.nextTrack, nextPendingIntent)
//
//        val publicNotification = NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.ic_vkm_main)
//                .setVisibility(VISIBILITY_PUBLIC)
//                .setStyle(NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
//                .build()
//
//        return NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.ic_vkm_main)
//                .setAutoCancel(false)
//                .setPriority(PRIORITY_MAX)
//                .setCategory(CATEGORY_SERVICE)
//                .setVisibility(VISIBILITY_PUBLIC)
//                .setOngoing(true)
//                .setCustomContentView(playerView)
//                .setPublicVersion(publicNotification)
//                .build()
//    }

    private fun startTrackProgressTrack(onUpdate: () -> Unit = {}) {
        progressUpdateJob?.cancel()
        onProgressUpdate = onUpdate

        progressUpdateJob = GlobalScope.launch(Dispatchers.IO) {
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
        GlobalScope.launch(Dispatchers.IO) {
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
                _onLoaded()
            }

            currentComposition?.let {
                "Starting media player for ${currentComposition?.fileName()}".log()
                _onPlay()
                mp.start()
                mp.setOnCompletionListener { next() }
                startTrackProgressTrack(onProgressUpdate)
            }
        }
    }

    fun pause() {
        if (isPaused.compareAndSet(false, true)) {
            mp.takeIf { it.isPlaying }?.pause()
            _onPause()
//            createNotification()
        }
    }

    fun skipTo(time: Int) = mp.seekTo(time)

    fun stop() {
        mp.stop()
        resetTrack()
        currentComposition = null
        stopProgressUpdate()
        _onStop()
    }

    fun loading() = isLoading.get()
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

        GlobalScope.launch(Dispatchers.IO) {
            var index = playList.indexOf(currentComposition)
            var steps = 0
            do {
                index = (playList.size + index + if (next) 1 else -1) % playList.size
                steps++
                currentComposition = try {
                    updateMediaSession(PlaybackStateCompat.STATE_BUFFERING)
                    fetchComposition(playList.getOrNull(index))
                } catch (e: Exception) {
                    "Error fetching next composition".logE(e)
                    null
                }
                "Next sibling is ${currentComposition?.fileName()}".log()
            } while (currentComposition == null && steps < 20)

            currentComposition?.let { play(currentComposition) }
        }
    }

    private suspend fun fetchComposition(composition: Composition?): Composition? {
        "Fetching composition ${composition?.fileName()}".log()
        if (composition == null) { return null }
        MusicService.trackMusicService.preprocess(composition)

        val resource = if (composition.hash.isEmpty()) composition.url else DownloadManager.getDownloadDir().resolve(composition.fileName())

        mp.reset()
        var internetLockRequired = false
        if (composition.url.startsWith("http")) {
            internetLockRequired = true
            mp.setDataSource(resource as String)
        } else {
            mp.setDataSource(applicationContext, Uri.fromFile(resource as File))
        }

        mp.setOnErrorListener { _, _, _ ->
            // TODO handle error
            next()
            return@setOnErrorListener true
        }

        if (internetLockRequired) {
            wifiLock.acquire()
            "Wifi lock acquired ${wifiLock.isHeld}".log()
        }
        mp.loadAsync()
        if (internetLockRequired) { wifiLock.release() }
        trackLength = mp.duration
        trackProgress = 0
        return composition
    }


    private fun resetTrack() {
        trackLength = 0
        trackProgress = 0
    }

    private fun _onPlay() {
        val result = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { return; }

        if (loading()) updateMediaSession(PlaybackState.STATE_BUFFERING) else updateMediaSession(PlaybackState.STATE_PLAYING)
        mediaSession.isActive = true
        becomeNoisyListener.register()
//        startForeground(2, createNotification())
        onPlay()
    }

    private fun _onStop() {
        updateMediaSession(PlaybackState.STATE_STOPPED)
//        createNotification()
        becomeNoisyListener.unregister()
        audioManager.abandonAudioFocus(audioFocusListener)
        stopForeground(false)
        onStop()
    }

    private fun _onLoaded() {
//        createNotification()
        onLoaded()
    }

    private fun _onPause() {
        updateMediaSession(PlaybackState.STATE_PAUSED)
//        createNotification()
        becomeNoisyListener.unregister()
        audioManager.abandonAudioFocus(audioFocusListener)
        stopForeground(false)
        onPause()
    }
}

class AudioFocusChangeListener(private val service: MusicPlayService) : AudioManager.OnAudioFocusChangeListener {
    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange != AudioManager.AUDIOFOCUS_GAIN) service.pause()
    }
}

class BecomeNoisyListener(private val service: MusicPlayService) : BroadcastReceiver() {
    private var registered = false

    fun register() = registration(true)
    fun unregister() = registration(false)

    private fun registration(register: Boolean) {
        if (!(registered xor register)) { return }
        when (register) {
            true -> service.registerReceiver(this, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            false -> service.unregisterReceiver(this)
        }
        registered = register
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> service.pause()
        }
    }
}