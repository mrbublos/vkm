package vkm.vkm

import android.view.View
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.VkmFragment
import vkm.vkm.utils.logE
import vkm.vkm.utils.toast

class SettingsFragment : VkmFragment() {

    init { layout = R.layout.activity_settings }

    override fun init() {
        dangerousCommandsVisibility(false)

        enableSuggestions.isChecked = State.enableTextSuggestions
        enableSuggestions.setOnCheckedChangeListener { _, value -> State.enableTextSuggestions = value }

        showDangerousCommands.setOnCheckedChangeListener { _, value -> dangerousCommandsVisibility(value) }

        clearDownloaded.setOnClickListener { DownloadManager.clearDownloaded() }

        clearQueue.setOnClickListener { DownloadManager.clearQueue() }

        useProxy.setOnCheckedChangeListener { _, value -> State.enableTextSuggestions = value }

        stopDownload.setOnClickListener { DownloadManager.stopDownload() }

        startDownload.setOnClickListener { DownloadManager.downloadComposition(null) }

        clearMusicDir.setOnClickListener { DownloadManager.removeAllMusic() }

        rehashDownloaded.setOnClickListener { DownloadManager.rehash() }

        restoreDownloaded.setOnClickListener {
            val me = this
            restoreDownloaded.visibility = View.GONE
            launch(CommonPool) {
                try {
                    DownloadManager.restoreDownloaded()
                    "List of downloaded files restored".toast(me.context)
                } catch (e: Exception) {
                    "".logE(e)
                    "Error restoring downloaded files ${e.message}".toast(me.context)
                }
                launch(UI) { restoreDownloaded.visibility = View.VISIBLE }
            }
        }
    }

    private fun dangerousCommandsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        listOf(clearMusicDir, rehashDownloaded, restoreDownloaded).forEach {
            (it.parent as View).visibility = visibility
        }
    }
}
