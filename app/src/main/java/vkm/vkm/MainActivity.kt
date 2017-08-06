package vkm.vkm

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TabHost
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val tabHost by bind<TabHost>(R.id.tabhost)

    // list tabs
    private val downloadedList by bind<ListView>(R.id.tab1)
    private val inProgressList by bind<ListView>(R.id.tab2)
    private val queueList by bind<ListView>(R.id.tab3)
    private val musicService = MusicService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        tabSpec.setContent(R.id.tab1)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("inProgress")
        tabSpec.setIndicator(getString(R.string.tab_in_progress))
        tabSpec.setContent(R.id.tab1)
        tabHost.addTab(tabSpec)

        tabHost.setCurrentTabByTag("inProgress")
        tabHost.setOnTabChangedListener(TabHost.OnTabChangeListener(this::handleTabSwitch))
    }


    fun handleTabSwitch(tabId: String) {
        when (tabId) {
            "downloaded" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.list_element, musicService.getMock().getDownloaded())
            "queue" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.list_element, musicService.getMock().getInProgress())
            "inProgress" -> downloadedList.adapter = CompositionListAdapter(this, R.layout.list_element, musicService.getMock().getInProgress())
        }
    }
}

class CompositionListAdapter(context: Context, resource: Int, data: List<Composition>) : ArrayAdapter<Composition>(context, resource, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var view = convertView

        if (view == null) { view = LayoutInflater.from(context).inflate(R.layout.list_element, null) }

        val item = getItem(position)

        if (item != null) {
            (view?.findViewById(R.id.name) as TextView).text = item.name
            (view.findViewById(R.id.artist) as TextView).text = item.artist
        }

        return view
    }
}