package vkm.vkm

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import vkm.vkm.utils.CompositionListAdapter

class HistoryActivity : AppCompatActivity() {

    private val tabHost by bind<TabHost>(R.id.tabhost)

    // list tabs
    private val downloadedList by bind<ListView>(R.id.tab1)
    private val queueList by bind<ListView>(R.id.tab2)
    private val inProgressList by bind<LinearLayout>(R.id.tab3)
    private val swipeCatcher by bind<SwipeCatcher>(R.id.swipeCatcher)
    private val progressBar by bind<ProgressBar>(R.id.downloadProgress)
    private val button by bind<Button>(R.id.button)
    private val filterText by bind<EditText>(R.id.search)
    private var stopLiveUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        swipeCatcher.left = SearchActivity::class.java
        swipeCatcher.right = SettingsActivity::class.java
        swipeCatcher.activity = this

        stopLiveUpdating = false

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
        tabSpec.setContent(R.id.tab1)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("queue")
        tabSpec.setIndicator(getString(R.string.tab_queue))
        tabSpec.setContent(R.id.tab2)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("inProgress")
        tabSpec.setIndicator(getString(R.string.tab_in_progress))
        tabSpec.setContent(R.id.tab3)
        tabHost.addTab(tabSpec)

        setInProgress(DownloadManager.getInProgress().firstOrNull())
        tabHost.setCurrentTabByTag("inProgress")
        tabHost.setOnTabChangedListener(TabHost.OnTabChangeListener(this::handleTabSwitch))
    }

    private fun initializeButton() {
        button.setOnClickListener {
            val text = filterText.text.toString()

            when (tabHost.currentTabTag) {
                "downloaded" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getDownloaded().filter { it.matches(text) })
                "queue" -> queueList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getQueue().filter { it.matches(text) }, removeFromQueue)
                "inProgress" -> {}
            }
        }
    }

    fun setInProgress(composition: Composition?) {
        inProgressList.bind<ImageView>(R.id.imageView).visibility = View.GONE
        if (composition == null) {
            inProgressList.bind<TextView>(R.id.artist).text = null
            inProgressList.bind<TextView>(R.id.name).text = null
            progressBar.visibility = View.GONE
        } else {
            inProgressList.bind<TextView>(R.id.artist).text = composition.artist
            inProgressList.bind<TextView>(R.id.name).text = composition.name
            progressBar.visibility = View.VISIBLE
            progressBar.progress = DownloadManager.downloadedPercent
            progressBar.secondaryProgress = DownloadManager.downloadedPercent
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

    private fun handleTabSwitch(tabId: String) {
        when (tabId) {
            "downloaded" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getDownloaded())
            "queue" -> queueList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, DownloadManager.getQueue(), removeFromQueue)
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
}