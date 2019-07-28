package vkm.vkm

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.media.session.PlaybackState.*
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.*
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import vkm.vkm.utils.*
import java.io.File

class MusicPlayService : Service() {

    companion object {
        private lateinit var instance: MusicPlayService
    }

    val displayedComposition: MutableLiveData<Composition> = MutableLiveData()
    val loadingComposition: MutableLiveData<Composition> = MutableLiveData()
    val state: MutableLiveData<String> = MutableLiveData()
    val progress: MutableLiveData<Long> = MutableLiveData()

    private var currentComposition: Composition? = null
    var playList: List<Composition> = mutableListOf()

    var trackLength = 0L

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

    private val playerControlsCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() { play() }
        override fun onPause() { pause() }
        override fun onSkipToNext() { next() }
        override fun onSkipToPrevious() { previous() }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) { return START_NOT_STICKY }

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
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

        mediaSession = MediaSessionCompat(baseContext, "vkm")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(playerControlsCallback)

        state.observeForever {
            val state = when (it) {
                "stopped" -> PlaybackState.STATE_STOPPED
                "playing" -> PlaybackState.STATE_PLAYING
                "loading" -> PlaybackState.STATE_BUFFERING
                "paused" -> PlaybackState.STATE_PAUSED
                else -> PlaybackState.STATE_STOPPED
            }
            updateMediaSession(state)
            createNotification()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdate()
        becomeNoisyListener.unregister()
        notificationManager.cancelAll()
        mediaSession.release()
        mp.release()
    }

    private fun updateMediaSession(state: Int) {
        val mediaMetadata = mediaMetadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentComposition?.artist ?: "No Artist")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentComposition?.name ?: "No Title")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, trackLength)
                .build()
        val playbackState = playbackStateBuilder
                .setState(state, 0, 1f)
                .setActions(ACTION_PLAY_PAUSE or ACTION_SKIP_TO_NEXT)
                .build()
        mediaSession.setMetadata(mediaMetadata)
        mediaSession.setPlaybackState(playbackState)
    }

    private fun createNotification(): Notification {
        val nextPendingIntent = PendingIntent.getService(this, 1, Intent(this, MusicPlayService::class.java).setAction("next"), PendingIntent.FLAG_UPDATE_CURRENT)
        val pausePendingIntent = PendingIntent.getService(this, 3, Intent(this, MusicPlayService::class.java).setAction("pause"), PendingIntent.FLAG_UPDATE_CURRENT)


        val playerView = RemoteViews(packageName, R.layout.notification_player)
        playerView.setTextViewText(R.id.name, currentComposition?.name ?: "")
        playerView.setTextViewText(R.id.artist, currentComposition?.artist ?: "")
        playerView.setImageViewResource(R.id.pause, if (isPlaying()) R.drawable.ic_pause_player_black else R.drawable.ic_play_player_black)
        playerView.setOnClickPendingIntent(R.id.pause, pausePendingIntent)
        playerView.setOnClickPendingIntent(R.id.nextTrack, nextPendingIntent)

        val publicNotification = NotificationCompat.Builder(this, createNotificationChannel())
                .setSmallIcon(R.drawable.ic_vkm_main)
                .setVisibility(VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
                .build()

        return NotificationCompat.Builder(this, createNotificationChannel())
                .setSmallIcon(R.drawable.ic_vkm_main)
                .setAutoCancel(false)
                .setPriority(PRIORITY_MAX)
                .setCategory(CATEGORY_SERVICE)
                .setVisibility(VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setCustomContentView(playerView)
                .setPublicVersion(publicNotification)
                .build()
    }

    private fun startProgressTracking() {
        progressUpdateJob?.cancel()
        progressUpdateJob = GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                val length = trackLength
                if (length > 0 && mp.isPlaying) {
                    GlobalScope.launch(Dispatchers.Main) { progress.value = mp.currentPosition * 100 / length }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    class MusicPlayerController : Binder() {
        fun getService(): MusicPlayService = instance
    }

    fun play(composition: Composition? = null) {
        stop()
        GlobalScope.launch {
            val compositionToPlay = composition ?: (currentComposition ?: playList.firstOrNull())

            "Starting to play ${compositionToPlay?.fileName()}".log()
            playList.takeIf { compositionToPlay != null }?.let {
                currentComposition = try {
                    fetchComposition(compositionToPlay)
                } catch (e: Exception) {
                    "Failed to load track ${compositionToPlay?.fileName()}".logE(e)
                    null
                }
                "Composition fetched ${currentComposition?.fileName()}".log()
            }

            currentComposition?.let {
                "Starting media player for ${currentComposition?.fileName()}".log()
                mp.start()
                _onPlay()
                GlobalScope.launch(Dispatchers.Main) { displayedComposition.value = currentComposition }
                mp.setOnCompletionListener { next() }
                startProgressTracking()
            }
        }
    }

    fun pause() {
        mp.takeIf { it.isPlaying }?.pause()
        GlobalScope.launch(Dispatchers.Main) { state.value = "paused" }
        becomeNoisyListener.unregister()
        audioManager.abandonAudioFocus(audioFocusListener)
        startForeground(2, createNotification())
    }

    fun skipTo(time: Int) = mp.seekTo(time)

    fun stop() {
        GlobalScope.launch(Dispatchers.Main) { state.value = "stopped" }
        if(mp.isPlaying) { mp.stop() }
        resetTrack()
        currentComposition = null
        stopProgressUpdate()
        becomeNoisyListener.unregister()
        audioManager.abandonAudioFocus(audioFocusListener)
        startForeground(2, createNotification())
        stopForeground(false)
    }

    fun isPlaying() = mp.isPlaying

    fun next() = getSibling(true)
    fun previous() = getSibling(false)

    private fun getSibling(next: Boolean) {
        resetTrack()

        if (playList.isEmpty()) { return }

        GlobalScope.launch(Dispatchers.IO) {
            var index = playList.indexOf(currentComposition)
            var steps = 0
            do {
                index = (playList.size + index + if (next) 1 else -1) % playList.size
                steps++
                currentComposition = try {
                    fetchComposition(playList.getOrNull(index))
                } catch (e: Exception) {
                    "Error fetching next composition".logE(e)
                    null
                }
            } while (currentComposition == null && steps < 20)

            currentComposition?.let { play(currentComposition) }
        }
    }

    private suspend fun fetchComposition(composition: Composition?): Composition? {
        "Fetching composition ${composition?.fileName()}".log()
        if (composition == null) { return null }

        GlobalScope.launch(Dispatchers.Main) {
            loadingComposition.value = composition
            state.value = "loading"
        }

        MusicService.trackMusicService.preprocess(composition)
        val resource = if (composition.hash.isEmpty()) composition.url else DownloadManager.getDownloadDir().resolve(composition.fileName())

        mp.reset()
        if (composition.url.startsWith("http")) {
            mp.setDataSource(resource as String)
        } else {
            mp.setDataSource(applicationContext, Uri.fromFile(resource as File))
        }

        mp.setOnErrorListener { _, _, _ ->
            // TODO handle error
            next()
            return@setOnErrorListener true
        }

        mp.loadAsync()
        GlobalScope.launch(Dispatchers.Main) {
            loadingComposition.value = null
            progress.value = 0
        }
        trackLength = mp.duration.toLong()

        return composition
    }


    private fun resetTrack() {
        trackLength = 0
        GlobalScope.launch(Dispatchers.Main) { progress.value = 0 }
    }

    private fun _onPlay() {
        GlobalScope.launch(Dispatchers.Main) { state.value = "playing" }
        val result = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { return; }

        mediaSession.isActive = true
        becomeNoisyListener.register()
        startForeground(2, createNotification())
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("vkm_music_service","Vkm Music service", NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }

        return "vkm_music_service"
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