package vkm.vkm

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.activity_settings, container, false) as View

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dangerousCommandsVisibility(false)

        enableDownloadAll.isChecked = State.enableDownloadAll
        enableDownloadAll.setOnCheckedChangeListener { _, value -> State.enableDownloadAll = value }

        enableSuggestions.isChecked = State.enableTextSuggestions
        enableSuggestions.setOnCheckedChangeListener { _, value -> State.enableTextSuggestions = value }

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

        selectProxy.setOnClickListener { startActivity(Intent(context, ProxyActivity::class.java)) }
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
