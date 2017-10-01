package vkm.vkm

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        swipeCatcher.activity = this
        swipeCatcher.left = HistoryActivity::class.java
        swipeCatcher.right = SearchActivity::class.java

        dangerousCommandsVisibility(false)

        enableDownloadAll.isChecked = StateManager.enableDownloadAll
        enableDownloadAll.setOnCheckedChangeListener { _, value -> StateManager.enableDownloadAll = value }

        enableSuggestions.isChecked = StateManager.enableTextSuggestions
        enableSuggestions.setOnCheckedChangeListener { _, value -> StateManager.enableTextSuggestions = value }

        showDangerousCommands.setOnCheckedChangeListener { _, value -> dangerousCommandsVisibility(value) }

        clearDownloaded.setOnClickListener { DownloadManager.clearDownloaded() }

        clearQueue.setOnClickListener { DownloadManager.clearQueue() }

        stopDownload.setOnClickListener { DownloadManager.stopDownload("") }

        startDownload.setOnClickListener { DownloadManager.downloadComposition(null) }

        loadLists.setOnClickListener { DownloadManager.loadAll() }

        dumpLists.setOnClickListener { DownloadManager.dumpAll() }

        clearMusicDir.setOnClickListener { DownloadManager.removeAllMusic() }

        removeAllSettings.setOnClickListener { SecurityService.clearAll() }

        rehashDownloaded.setOnClickListener { DownloadManager.rehashAndDump() }

        restoreDownloaded.setOnClickListener { DownloadManager.restoreDownloaded() }

        selectProxy.setOnClickListener { startActivity(Intent(applicationContext, ProxyActivity::class.java)) }
    }

    private fun dangerousCommandsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        (clearMusicDir.parent as View).visibility = visibility
        (removeAllSettings.parent as View).visibility = visibility
        (rehashDownloaded.parent as View).visibility = visibility
        (restoreDownloaded.parent as View).visibility = visibility
        (loadLists.parent as View).visibility = visibility
        (dumpLists.parent as View).visibility = visibility
    }
}
