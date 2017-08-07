package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.ListView
import android.widget.TabHost
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.SwipeManager

class HistoryActivity : AppCompatActivity() {

    private val tabHost by bind<TabHost>(R.id.tabhost)

    // list tabs
    private val downloadedList by bind<ListView>(R.id.tab1)
    private val inProgressList by bind<ListView>(R.id.tab2)
    private val queueList by bind<ListView>(R.id.tab3)
    private val musicService = MusicService()

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
            "downloaded" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getMock().getDownloaded())
            "queue" -> queueList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getMock().getInProgress())
            "inProgress" -> inProgressList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getMock().getInProgress())
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        SwipeManager.manageSwipe(event, this, SearchActivity::class.java)
        return super.onTouchEvent(event)
    }
}