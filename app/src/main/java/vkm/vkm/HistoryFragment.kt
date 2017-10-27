package vkm.vkm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.composition_list_element.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.*

class HistoryFragment : Fragment() {

    private var progressUpdateJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.activity_history, container, false) as View

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        search.inputType = if (State.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        initializeTabs()
        initializeButton()
    }

    private fun initializeTabs() {
        tabsSwiper.value = mutableListOf("downloaded", "queue", "inProgress")
        tabsSwiper.setCurrentString("inProgress")
        initInProgressTab(true)

        tabsSwiper.onSwiped = { _, tabName ->
            State.currentHistoryTab = tabName
            val text = search.text.toString()
            initInProgressTab(false)
            progressUpdateJob?.cancel()

            when (State.currentHistoryTab) {
                "downloaded" -> resultList.adapter = CompositionListAdapter(this, context, R.layout.composition_list_element, DownloadManager.getDownloaded().filter { it.matches(text) }, remove)
                "queue" -> resultList.adapter = CompositionListAdapter(this, context, R.layout.composition_list_element, DownloadManager.getQueue().filter { it.matches(text) }, removeFromQueue)
                "inProgress" -> {
                    initInProgressTab(true)
                    startProgressUpdate()
                }
            }
        }
    }

    private fun startProgressUpdate() {
        progressUpdateJob = launch(CommonPool) {
            while (true) {
                launch(UI) { setInProgress(DownloadManager.getInProgress().firstOrNull()) }
                delay(1000)
            }
        }
    }

    private fun initInProgressTab(show: Boolean) {
        resultList.visibility = if (!show) View.VISIBLE else View.GONE
        downloadProgress.visibility = if (show && !DownloadManager._inProgress.isEmpty()) View.VISIBLE else View.GONE
        compositionInProgress.visibility = if (show) View.VISIBLE else View.GONE
        resumeDownloadButton.visibility = if (show) View.VISIBLE else View.GONE
        audioControl.visibility = if (!show) View.VISIBLE else View.GONE
        imageView.visibility = if (!show) View.VISIBLE else View.GONE
    }

    private fun initializeButton() {
        searchButton.setOnClickListener {
            val text = search.text.toString()

            when (State.currentHistoryTab) {
                "downloaded" -> resultList.adapter = CompositionListAdapter(this, context, R.layout.composition_list_element, DownloadManager.getDownloaded().filter { it.matches(text) }, remove)
                "queue" -> resultList.adapter = CompositionListAdapter(this, context, R.layout.composition_list_element, DownloadManager.getQueue().filter { it.matches(text) }, removeFromQueue)
            }
        }

        resumeDownloadButton.setOnClickListener { DownloadManager.downloadComposition(null) }
    }

    private fun setInProgress(composition: Composition?) {
        if (composition == null) {
            downloadProgress.visibility = View.GONE
        } else {
            downloadProgress.visibility = View.VISIBLE
            downloadProgress.progress = DownloadManager.downloadedPercent
            downloadProgress.secondaryProgress = DownloadManager.downloadedPercent
        }
    }

    override fun onPause() {
        super.onPause()
        progressUpdateJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        startProgressUpdate()
    }

    override fun onStop() {
        super.onStop()
        MusicPlayer.stop()
        progressUpdateJob?.cancel()
    }

    private val removeFromQueue = { composition: Composition?, view: View ->
        composition?.let {
            DownloadManager.removeFromQueue(composition)
            (view.parent as View).visibility = View.GONE
        }
    }

    private val remove = { composition: Composition?, view: View ->
        composition?.let {
            try {
                composition.localFile()?.delete()
                DownloadManager._downloadedList.remove(composition)
                (view.parent as View).visibility = View.GONE
            } catch (e: Exception) {
                "Unable to remove track".toast(context)
                return@let
            }
        }
    }
}