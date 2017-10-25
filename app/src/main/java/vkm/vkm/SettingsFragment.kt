package vkm.vkm

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_settings.view.*

class SettingsFragment : Fragment() {

    lateinit private var me: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        me = inflater?.inflate(R.layout.activity_settings, container, false) as View
        init()
        return me
    }

    private fun init() {
        dangerousCommandsVisibility(false)

        me.enableDownloadAll.isChecked = State.enableDownloadAll
        me.enableDownloadAll.setOnCheckedChangeListener { _, value -> State.enableDownloadAll = value }

        me.enableSuggestions.isChecked = State.enableTextSuggestions
        me.enableSuggestions.setOnCheckedChangeListener { _, value -> State.enableTextSuggestions = value }

        me.showDangerousCommands.setOnCheckedChangeListener { _, value -> dangerousCommandsVisibility(value) }

        me.clearDownloaded.setOnClickListener { DownloadManager.clearDownloaded() }

        me.clearQueue.setOnClickListener { DownloadManager.clearQueue() }

        me.stopDownload.setOnClickListener { DownloadManager.stopDownload("") }

        me.startDownload.setOnClickListener { DownloadManager.downloadComposition(null) }

        me.loadLists.setOnClickListener { DownloadManager.loadAll() }

        me.dumpLists.setOnClickListener { DownloadManager.dumpAll() }

        me.clearMusicDir.setOnClickListener { DownloadManager.removeAllMusic() }

        me.removeAllSettings.setOnClickListener { SecurityService.clearAll() }

        me.rehashDownloaded.setOnClickListener { DownloadManager.rehashAndDump() }

        me.restoreDownloaded.setOnClickListener { DownloadManager.restoreDownloaded() }

        me.selectProxy.setOnClickListener { startActivity(Intent(context, ProxyActivity::class.java)) }
    }

    private fun dangerousCommandsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        (me.clearMusicDir.parent as View).visibility = visibility
        (me.removeAllSettings.parent as View).visibility = visibility
        (me.rehashDownloaded.parent as View).visibility = visibility
        (me.restoreDownloaded.parent as View).visibility = visibility
        (me.loadLists.parent as View).visibility = visibility
        (me.dumpLists.parent as View).visibility = visibility
    }
}
