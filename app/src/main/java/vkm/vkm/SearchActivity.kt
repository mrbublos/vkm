package vkm.vkm

import android.os.Bundle
import android.support.constraint.ConstraintLayout
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
    val loadingSpinner by bind<ProgressBar>(R.id.loading_spinner)

    // selected user
    val selectedUserContainer by bind<ConstraintLayout>(R.id.selected_user_container)
    val selectedUserName by bind<TextView>(R.id.selected_user_name)
    val selectedUserId by bind<TextView>(R.id.selected_user_id)
    val selectedUserPhoto by bind<ImageView>(R.id.selected_user_photo)
    val selectedUserButton by bind<ImageView>(R.id.deselect_user_button)

    // services
    val musicService = MusicService()

    // private vars
    var filterText: String = ""
    var selectedUser: User? = null
    var selectedGroup: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(MainActivity.TAG, "Setting content view")
        setContentView(R.layout.activity_search)

        initializeElements()
        initializeTabs()
        initializeButton()

        // temp call for debugging
        musicService.getUserPlaylist(this, "6")
    }

    fun initializeElements() {
        // hiding selected user container
        selectedUserContainer.visibility = View.GONE
        loadingSpinner.visibility = View.GONE
    }

    fun initializeTabs() {
        // TODO consider switching to tabLayout
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
    }

    fun initializeButton() {
        lockUnlockScreen(false)
        button.setOnTouchListener { _, event ->
            filterText = textContainer.text.toString()
            loadingSpinner.visibility = View.VISIBLE

            lockUnlockScreen(true)

            when (tabHost.currentTabTag) {
                "user" -> if (selectedUser != null) {
                    musicService.getUserPlaylist(this, selectedUser?.userId!!)
                } else {
                    musicService.getUsers(this, filterText)
                }
                "group" -> if (selectedGroup != null) {
                    musicService.getGroupPlaylist(this, filterText!!)
                } else {
                    musicService.getGroups(this, filterText)
                }
                "composition" -> musicService.getCompositions(this, filterText)
            }

            return@setOnTouchListener super.onTouchEvent(event)
        }
    }

    // callback functions
    fun setUserList(data: List<User>) {
        lockUnlockScreen(false)
        loadingSpinner.visibility = View.GONE
        userList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, selectUserOrGroup)
    }

    fun setGroupList(data: List<User>) {
        lockUnlockScreen(false)
        loadingSpinner.visibility = View.GONE
        groupList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, selectUserOrGroup)
    }

    fun setCompositionsList(data: List<Composition>) {
        lockUnlockScreen(false)
        loadingSpinner.visibility = View.GONE
        compositionList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, data)
    }

    val selectUserOrGroup = { newSelectedElement: User? ->
        when (tabHost.currentTabTag) {
            "user" -> {
                selectedUser = newSelectedElement
                musicService.getUserPlaylist(this, selectedUser?.userId!!)
            }
            "group" -> {
                selectedGroup = newSelectedElement
                musicService.getGroupPlaylist(this, selectedGroup?.userId!!)
            }
        }

        // TODO hide User and Group tabs
        tabHost.currentTab = 2

        newSelectedElement?.let {
            selectedUserContainer.visibility = View.VISIBLE
            selectedUserName.text = selectedUser?.fullname
            selectedUserId.text = selectedUser?.userId

            if (newSelectedElement.photo == null) {
                AsyncPhotoDownloader().execute(newSelectedElement, selectedUserPhoto)
            } else {
                selectedUserPhoto.setImageBitmap(newSelectedElement.photo)
            }

            selectedUserButton.setOnTouchListener { _, event ->
                selectedUserContainer.visibility = View.GONE

                // hiding User and Group tabs
                tabHost.getChildAt(0).visibility = View.VISIBLE
                tabHost.getChildAt(1).visibility = View.VISIBLE

                return@setOnTouchListener super.onTouchEvent(event)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        SwipeManager.manageSwipe(event, this, HistoryActivity::class.java)
        return super.onTouchEvent(event)
    }

    fun lockUnlockScreen(lock: Boolean) {
        textContainer.isFocusable = !lock
        textContainer.isClickable = !lock
        button.isFocusable = !lock
        button.isClickable = !lock
    }
}
