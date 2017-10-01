package vkm.vkm

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.widget.ImageView
import android.widget.TabHost
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_history.*
import vkm.vkm.utils.CompositionListAdapter

class HistoryActivity : AppCompatActivity() {

    // list tabs
    private var stopLiveUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        swipeCatcher.left = SearchActivity::class.java
        swipeCatcher.right = SettingsActivity::class.java
        swipeCatcher.activity = this

        stopLiveUpdating = false

        MusicPlayer.context = this

        search.inputType = if (StateManager.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        initializeTabs()
        updateProgress()
        initializeButton()
    }

    private fun updateProgress() {
        Handler().postDelayed({
            if (stopLiveUpdating || (DownloadManager.getInProgress().firstOrNull() == null && DownloadManager.getQueue().isEmpty())) { return@postDelayed }
            Handler(Looper.getMainLooper()).post({
                setInProgress(DownloadManager.getInProgress().firstOrNull())
            })
            updateProgress()
        }, 1000)
    }

    private fun initializeTabs() {
        tabHost.setup()

        var tabSpec = tabHost.newTabSpec("downloaded")
        tabSpec.setIndicator(getString(R.string.tab_downloaded))
        tabSpec.setContent(R.id.userList)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("queue")
        tabSpec.setIndicator(getString(R.string.tab_queue))
        tabSpec.setContent(R.id.groupList)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("inProgress")
        tabSpec.setIndicator(getString(R.string.tab_in_progress))
        tabSpec.setContent(R.id.compositionList)
        tabHost.addTab(tabSpec)

        setInProgress(DownloadManager.getInProgress().firstOrNull())
        tabHost.setCurrentTabByTag("inProgress")
        tabHost.setOnTabChangedListener(TabHost.OnTabChangeListener(this::handleTabSwitch))
    }

    private fun initializeButton() {
        button.setOnClickListener {
            val text = search.text.toString()

            when (tabHost.currentTabTag) {
                "downloaded" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getDownloaded().filter { it.matches(text) })
                "queue" -> waitingForDownloadList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getQueue().filter { it.matches(text) }, removeFromQueue)
                "inProgress" -> {}
            }
        }
    }

    private fun setInProgress(composition: Composition?) {
        inProgressList.bind<ImageView>(R.id.imageView).visibility = View.GONE
        inProgressList.bind<ImageView>(R.id.audioControl).visibility = View.GONE
        if (composition == null) {
            inProgressList.bind<TextView>(R.id.artist).text = null
            inProgressList.bind<TextView>(R.id.name).text = null
            downloadProgress.visibility = View.GONE
        } else {
            inProgressList.bind<TextView>(R.id.artist).text = composition.artist
            inProgressList.bind<TextView>(R.id.name).text = composition.name
            downloadProgress.visibility = View.VISIBLE
            downloadProgress.progress = DownloadManager.downloadedPercent
            downloadProgress.secondaryProgress = DownloadManager.downloadedPercent
        }
    }

    override fun onPause() {
        super.onPause()
        stopLiveUpdating = true
    }

    override fun onResume() {
        super.onResume()
        stopLiveUpdating = false
        updateProgress()
    }

    override fun onStop() {
        super.onStop()
        MusicPlayer.stop()
    }

    private fun handleTabSwitch(tabId: String) {
        when (tabId) {
            "downloaded" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getDownloaded(), remove)
            "queue" -> waitingForDownloadList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getQueue(), removeFromQueue)
            "inProgress" -> {
                stopLiveUpdating = false
                setInProgress(DownloadManager.getInProgress().firstOrNull())
            }
        }
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
                "Unable to remove track".toast(this)
                return@let
            }
        }
    }
}