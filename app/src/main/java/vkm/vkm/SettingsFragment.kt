package vkm.vkm

import android.content.Intent
import android.view.View
import kotlinx.android.synthetic.main.activity_settings.*
import vkm.vkm.utils.VkmFragment

class SettingsFragment : VkmFragment() {

    init { layout = R.layout.activity_settings }

    override fun init() {
        dangerousCommandsVisibility(false)

        enableDownloadAll.isChecked = State.enableDownloadAll
        enableDownloadAll.setOnCheckedChangeListener { _, value -> State.enableDownloadAll = value }

        enableSuggestions.isChecked = State.enableTextSuggestions
        enableSuggestions.setOnCheckedChangeListener { _, value -> State.enableTextSuggestions = value }

        enableDeveloperMode.isChecked = State.developerMode
        enableDeveloperMode.setOnCheckedChangeListener { _, value -> State.developerMode = value }

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

//        selectProxy.setOnClickListener { startActivity(Intent(context, ProxyActivity::class.java)) }
    }

    private fun dangerousCommandsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        listOf(clearMusicDir, removeAllSettings, rehashDownloaded, restoreDownloaded, loadLists, dumpLists).forEach {
            (it.parent as View).visibility = visibility
        }
    }
}
