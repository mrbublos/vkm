package vkm.vkm

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class SettingsActivity : AppCompatActivity() {

    private val clearDownloaded by bind<Button>(R.id.clear_downloaded_button)
    private val clearQueue by bind<Button>(R.id.clear_queue_button)
    private val stopDownload by bind<Button>(R.id.stop_download_button)
    private val loadLists by bind<Button>(R.id.load_lists_button)
    private val dumpLists by bind<Button>(R.id.dump_lists_button)
    private val clearMusicDir by bind<Button>(R.id.clear_music_dir)
    private val swipeCatcher by bind<SwipeCatcher>(R.id.swipeCatcher)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        swipeCatcher.activity = this
        swipeCatcher.left = HistoryActivity::class.java
        swipeCatcher.right = SearchActivity::class.java

        clearDownloaded.setOnClickListener {
            DownloadManager._downloadedList.clear()
        }

        clearQueue.setOnClickListener {
            DownloadManager._queue.clear()
        }

        stopDownload.setOnClickListener {
            DownloadManager.stopDownload("")
        }

        loadLists.setOnClickListener {
            DownloadManager.loadAll()
        }

        dumpLists.setOnClickListener {
            DownloadManager.dumpAll()
        }

        clearMusicDir.setOnClickListener {
            DownloadManager.removeAllMusic()
        }
    }
}
