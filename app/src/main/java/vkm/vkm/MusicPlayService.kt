package vkm.vkm

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex

class MusicPlayService: Service() {

    companion object {
        lateinit var instance: MusicPlayService
    }

    val mp = MediaPlayer()
    var currentComposition: Composition? = null
    var playList: List<Composition> = mutableListOf()
    val binder = MusicPlayerController()
    var trackLength = 0
    var trackProgress = 0


    override fun onBind(intent: Intent?): IBinder {
        "Service bound".log()
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        "Service created".log()
    }

    class MusicPlayerController: Binder() {
        fun getService(): MusicPlayService { return instance }
    }

    fun play() {
        launch(CommonPool) {
            if (currentComposition == null) {
                currentComposition = playList.firstOrNull()
                currentComposition?.let { compositionToPlay ->
                    val resource = if (compositionToPlay.hash.isEmpty()) compositionToPlay.url else DownloadManager.getDownloadDir().resolve(compositionToPlay.fileName()).path

                    if (compositionToPlay.url.startsWith("http")) {
                        mp.setDataSource(resource)
                    } else {
                        mp.setDataSource(applicationContext, Uri.parse(resource))
                    }
                    mp.loadAsync()
                    trackLength = mp.duration
                }
            } else {
                mp.start()
            }

            currentComposition?.let {

                mp.start()
            }
        }
    }



    private fun resetTrack() {
        trackLength = 0
        trackProgress = 0
    }

}
