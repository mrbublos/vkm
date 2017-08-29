package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import android.widget.TabHost
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.SwipeManager

class HistoryActivity : AppCompatActivity() {

    val tabHost by bind<TabHost>(R.id.tabhost)

    // list tabs
    val downloadedList by bind<ListView>(R.id.tab1)
    val inProgressList by bind<ListView>(R.id.tab2)
    val queueList by bind<ListView>(R.id.tab3)
    val musicService = MusicService()

    val sw: SwipeManager by lazy { SwipeManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        initializeTabs()
    }

    fun initializeTabs() {
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

        tabHost.setCurrentTabByTag("inProgress")
        tabHost.setOnTabChangedListener(TabHost.OnTabChangeListener(this::handleTabSwitch))
    }


    fun handleTabSwitch(tabId: String) {
        when (tabId) {
            "downloaded" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getDownloaded())
            "queue" -> queueList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getInProgress(), removeFromQueue)
            "inProgress" -> inProgressList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getInProgress())
        }
    }

    private val removeFromQueue = { composition: Composition?, view: View ->
        composition?.let {
            DownloadManager.removeFromQueue(composition)
            view.visibility = View.GONE
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        sw.mDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
}