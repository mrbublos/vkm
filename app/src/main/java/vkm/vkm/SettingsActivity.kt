package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Switch

class SettingsActivity : AppCompatActivity() {

    private val clearDownloaded by bind<Button>(R.id.clear_downloaded_button)
    private val clearQueue by bind<Button>(R.id.clear_queue_button)
    private val stopDownload by bind<Button>(R.id.stop_download_button)
    private val startDownload by bind<Button>(R.id.start_download_button)
    private val loadLists by bind<Button>(R.id.load_lists_button)
    private val dumpLists by bind<Button>(R.id.dump_lists_button)
    private val clearMusicDir by bind<Button>(R.id.clear_music_dir)
    private val removeSettings by bind<Button>(R.id.remove_all_settings)
    private val rehashDownloaded by bind<Button>(R.id.rehash_downloaded_button)
    private val restoreDownloaded by bind<Button>(R.id.restore_downloaded_button)
    private val swipeCatcher by bind<SwipeCatcher>(R.id.swipeCatcher)
    private val enableDownloadAllSwitch by bind<Switch>(R.id.enable_download_all_button)
    private val showDangerousCommands by bind<Switch>(R.id.show_danger_commands)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        swipeCatcher.activity = this
        swipeCatcher.left = HistoryActivity::class.java
        swipeCatcher.right = SearchActivity::class.java

        dangerousCommandsVisibility(false)

        enableDownloadAllSwitch.isChecked = StateManager.enableDownloadAll
        enableDownloadAllSwitch.setOnCheckedChangeListener { _, value -> StateManager.enableDownloadAll = value }

        showDangerousCommands.setOnCheckedChangeListener { _, value -> dangerousCommandsVisibility(value) }

        clearDownloaded.setOnClickListener { DownloadManager.clearDownloaded() }

        clearQueue.setOnClickListener { DownloadManager.clearQueue() }

        stopDownload.setOnClickListener { DownloadManager.stopDownload("") }

        startDownload.setOnClickListener { DownloadManager.downloadComposition(null) }

        loadLists.setOnClickListener { DownloadManager.loadAll() }

        dumpLists.setOnClickListener { DownloadManager.dumpAll() }

        clearMusicDir.setOnClickListener { DownloadManager.removeAllMusic() }

        removeSettings.setOnClickListener { SecurityService.clearAll() }

        rehashDownloaded.setOnClickListener { DownloadManager.rehashAndDump() }

        restoreDownloaded.setOnClickListener { DownloadManager.restoreDownloaded() }
    }

    private fun dangerousCommandsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        (clearMusicDir.parent as View).visibility = visibility
        (removeSettings.parent as View).visibility = visibility
        (rehashDownloaded.parent as View).visibility = visibility
        (restoreDownloaded.parent as View).visibility = visibility
        (loadLists.parent as View).visibility = visibility
        (dumpLists.parent as View).visibility = visibility
    }
}
