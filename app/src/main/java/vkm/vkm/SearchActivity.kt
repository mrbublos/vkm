package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TabHost
import android.widget.TextView
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.SwipeManager
import vkm.vkm.utils.UserListAdapter

class SearchActivity : AppCompatActivity() {

    // list tabs
    private val userList by bind<ListView>(R.id.tab1)
    private val groupList by bind<ListView>(R.id.tab2)
    private val compositionList by bind<ListView>(R.id.tab3)

    // active elements
    private val tabHost by bind<TabHost>(R.id.tabhost)
    private val button by bind<Button>(R.id.button)
    private val textContainer by bind<TextView>(R.id.search)

    // services
    private val musicService = MusicService().getMock()

    // private vars
    private var filterText: String = ""
    private var state = "search"
    private var selectedUserId: String = ""
    private var selectedGroupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        initializeTabs()
        initializeButton()
    }

    fun initializeTabs() {
        tabHost.setup()

        var tabSpec = tabHost.newTabSpec("user")
        tabSpec.setIndicator(getString(R.string.tab_user))
        tabSpec.setContent(R.id.tab1)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("group")
        tabSpec.setIndicator(getString(R.string.tab_group))
        tabSpec.setContent(R.id.tab2)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("composition")
        tabSpec.setIndicator(getString(R.string.tab_composition))
        tabSpec.setContent(R.id.tab3)
        tabHost.addTab(tabSpec)

        tabHost.setCurrentTabByTag("composition")
        tabHost.setOnTabChangedListener(TabHost.OnTabChangeListener(this::handleTabSwitch))
    }

    fun initializeButton() {
        button.setOnTouchListener { _, event ->
            filterText = textContainer.text.toString()
            state = "user"

            when (tabHost.currentTabTag) {
                "user" -> userList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getUsers())
                "group" -> groupList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getGroups())
                "composition" -> compositionList.adapter = CompositionListAdapter(this, R.layout.user_list_element, musicService.getCompositions())
            }
            return@setOnTouchListener super.onTouchEvent(event)
        }
    }

    fun handleTabSwitch(tabId: String) {
        val touchListener =  { view: View ->
            state = "composition"
            handleTabSwitch(tabHost.currentTabTag)
        }

        if (state == "user") {
            when (tabId) {
                "user" -> userList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getUsers(filterText))
                "group" -> groupList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getGroups(filterText))
                "composition" -> groupList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getCompositions(filterText))
            }
        } else {
            when (tabId) {
                "user" -> userList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getUserPlaylist(selectedUserId, filterText))
                "group" -> groupList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getGroupPlaylist(selectedGroupId, filterText))
                "composition" -> groupList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getCompositions(filterText))
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        SwipeManager.manageSwipe(event, this, HistoryActivity::class.java)
        return super.onTouchEvent(event)
    }
}
