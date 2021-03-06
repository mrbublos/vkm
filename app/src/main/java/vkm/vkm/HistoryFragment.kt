package vkm.vkm

import android.text.InputType
import android.view.View
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.composition_list_element.view.*
import kotlinx.coroutines.*
import vkm.vkm.adapters.CompositionListAdapter
import vkm.vkm.utils.*

class HistoryFragment : VkmFragment() {

    private var progressUpdateJob: Job? = null

    init { layout = R.layout.activity_history }

    override fun init() {
        search.inputType = if (State.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        initializeTabs()
        initializeButton()
    }

    private fun initializeTabs() {
        tabsSwiper.value = mutableListOf("downloaded", "queue", "inProgress")
        tabsSwiper.setCurrentString("inProgress")
        initInProgressTab(true)

        tabsSwiper.onSwiped = { _, tabName, _ ->
            State.currentHistoryTab = tabName
            val text = search.text.toString()
            initInProgressTab(false)
            progressUpdateJob?.cancel()

            when (State.currentHistoryTab) {
                "downloaded" -> resultList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getDownloaded().filter { it.matches(text) }, remove)
                "queue" -> resultList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getQueue().filter { it.matches(text) }, removeFromQueue)
                "inProgress" -> {
                    initInProgressTab(true)
                    startProgressUpdate()
                }
            }
        }
    }

    private fun startProgressUpdate() {
        progressUpdateJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val compositionBeingDownloaded = DownloadManager.getInProgress().firstOrNull()
                GlobalScope.launch(Dispatchers.Main) {
                    compositionInProgress.imageView.visibility = View.GONE
                    compositionInProgress.name.text = compositionBeingDownloaded?.name
                    compositionInProgress.artist.text = compositionBeingDownloaded?.artist
                    downloadProgress.progress = DownloadManager.downloadedPercent
                    downloadProgress.secondaryProgress = DownloadManager.downloadedPercent
                }
                delay(1000)
            }
        }
    }

    private fun initInProgressTab(show: Boolean) {
        resultList.visibility = if (!show) View.VISIBLE else View.GONE
        resumeDownloadButton.visibility = if (show) View.VISIBLE else View.GONE

        val showInProgress = show && DownloadManager.getInProgress().isNotEmpty()
        downloadProgress.visibility = if (showInProgress) View.VISIBLE else View.GONE
        compositionInProgress.visibility = if (showInProgress) View.VISIBLE else View.GONE
    }

    private fun initializeButton() {
        searchButton.setOnClickListener {
            val text = search.text.toString()

            when (State.currentHistoryTab) {
                "downloaded" -> resultList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getDownloaded().filter { it.matches(text) }, remove)
                "queue" -> resultList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getQueue().filter { it.matches(text) }, removeFromQueue)
            }
        }

        resumeDownloadButton.setOnClickListener { DownloadManager.downloadComposition(null) }
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
                DownloadManager.removeDownloaded(composition)
            } catch (e: Exception) {
                "Unable to remove track".toast(context)
                return@let
            }
        }
    }
}