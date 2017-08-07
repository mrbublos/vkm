package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import vkm.vkm.utils.AsyncPhotoDownloader
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.SwipeManager
import vkm.vkm.utils.UserListAdapter

class SearchActivity : AppCompatActivity() {

    // list tabs
    val userList by bind<ListView>(R.id.tab1)
    val groupList by bind<ListView>(R.id.tab2)
    val compositionList by bind<ListView>(R.id.tab3)

    // active elements
    val tabHost by bind<TabHost>(R.id.tabhost)
    val button by bind<Button>(R.id.button)
    val textContainer by bind<TextView>(R.id.search)

    // selected user
    val selectedUserContainer by bind<RelativeLayout>(R.id.selected_user_container)
    val selectedUserName by bind<TextView>(R.id.selected_user_container)
    val selectedUserId by bind<TextView>(R.id.selected_user_container)
    val selectedUserPhoto by bind<ImageView>(R.id.selected_user_container)
    val selectedUserButton by bind<ImageView>(R.id.selected_user_container)

    // services
    val musicService = MusicService().getMock()

    // private vars
    var filterText: String = ""
    var state = "search"
    var selectedUser: User = User()
    var selectedGroup: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("tag", "Setting content view")
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
                "user" -> userList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getUsers(), selectUserOrGroup)
                "group" -> groupList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getGroups(), selectUserOrGroup)
                "composition" -> compositionList.adapter = CompositionListAdapter(this, R.layout.user_list_element, musicService.getCompositions())
            }
            return@setOnTouchListener super.onTouchEvent(event)
        }
    }

    fun handleTabSwitch(tabId: String) {
        if (state == "user") {
            when (tabId) {
                "user" -> userList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getUsers(filterText), selectUserOrGroup)
                "group" -> groupList.adapter = UserListAdapter(this, R.layout.user_list_element, musicService.getGroups(filterText), selectUserOrGroup)
                "composition" -> groupList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getCompositions(filterText))
            }
        } else {
            when (tabId) {
                "user" -> userList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getUserPlaylist(selectedUser.userId, filterText))
                "group" -> groupList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getGroupPlaylist(selectedUser.userId, filterText))
                "composition" -> groupList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, musicService.getCompositions(filterText))
            }
        }
    }

    val selectUserOrGroup = {
        var _unit: User? = null
        when (tabHost.currentTabTag) {
            "user" -> _unit = selectedUser
            "group" -> _unit = selectedGroup
        }

        _unit?.let {
            selectedUserContainer.visibility = View.VISIBLE
            selectedUserName.text = selectedUser.fullname
            selectedUserId.text = selectedUser.userId
            AsyncPhotoDownloader().execute(selectedUser.photoUrl, selectedUserPhoto)
            selectedUserButton.setOnTouchListener { _, event ->
                state = "composition"
                selectedUserContainer.visibility = View.GONE
                handleTabSwitch(tabHost.currentTabTag)
                return@setOnTouchListener super.onTouchEvent(event)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        SwipeManager.manageSwipe(event, this, HistoryActivity::class.java)
        return super.onTouchEvent(event)
    }
}
