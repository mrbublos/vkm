package vkm.vkm

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import vkm.vkm.utils.AsyncPhotoDownloader
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.UserListAdapter

class SearchActivity : AppCompatActivity() {

    // list tabs
    private val userList by bind<ListView>(R.id.tab1)
    private val groupList by bind<ListView>(R.id.tab2)
    private val compositionList by bind<ListView>(R.id.tab3)
    private val swipeCatcher by bind<SwipeCatcher>(R.id.swipeCatcher)

    // active elements
    private val tabHost by bind<TabHost>(R.id.tabhost)
    private val button by bind<Button>(R.id.button)
    private val textContainer by bind<TextView>(R.id.search)
    private val loadingSpinner by bind<ProgressBar>(R.id.loading_spinner)

    // selected user
    private val selectedUserContainer by bind<ConstraintLayout>(R.id.selected_user_container)
    private val selectedUserName by bind<TextView>(R.id.selected_user_name)
    private val selectedUserId by bind<TextView>(R.id.selected_user_id)
    private val selectedUserPhoto by bind<ImageView>(R.id.selected_user_photo)
    private val selectedUserButton by bind<ImageView>(R.id.deselect_user_button)
    private val selectedUserDownloadButton by bind<ImageView>(R.id.download_all_user_button)

    // services
    private val musicService = MusicService()

    // private vars
    private var filterText: String = ""
    private var selectedElement: User? = null
    private var selectedGroup: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(MainActivity.TAG, "Setting content view")
        setContentView(R.layout.activity_search)

        initializeElements()
        initializeTabs()
        initializeButton()
        initializeLists()
    }

    private fun initializeElements() {
        swipeCatcher.left = SettingsActivity::class.java
        swipeCatcher.right = HistoryActivity::class.java
        swipeCatcher.activity = this

        // hiding selected user container
        selectedUserContainer.visibility = View.GONE
        loadingSpinner.visibility = View.GONE
    }

    private fun initializeTabs() {
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

        tabSpec = tabHost.newTabSpec("tracks")
        tabSpec.setIndicator(getString(R.string.tab_composition))
        tabSpec.setContent(R.id.tab3)
        tabHost.addTab(tabSpec)

        tabHost.setCurrentTabByTag(StateManager.currentSearchTab)
    }

    private fun initializeButton() {
        lockUnlockScreen(false)
        button.setOnClickListener { _ ->
            filterText = textContainer.text.toString()
            if (filterText.isEmpty()) { return@setOnClickListener }

            loadingSpinner.visibility = View.VISIBLE

            lockUnlockScreen(true)

            when (tabHost.currentTabTag) {
                "user" -> if (selectedElement != null) {
                    musicService.getPlaylist(this, selectedElement, filterText)
                } else {
                    musicService.getUsers(this, filterText)
                }
                "group" -> if (selectedGroup != null) {
                    musicService.getPlaylist(this, selectedGroup, filterText)
                } else {
                    musicService.getGroups(this, filterText)
                }
                "tracks" -> musicService.getCompositions(this, filterText)
            }

            return@setOnClickListener
        }
    }

    private fun initializeLists() {
        if (StateManager.userElementList.isNotEmpty()) { setUserList(StateManager.userElementList) }
        if (StateManager.groupElementList.isNotEmpty()) { setGroupList(StateManager.groupElementList) }
        if (StateManager.compositionElementList.isNotEmpty()) { setCompositionsList(StateManager.compositionElementList) }
    }

    // callback functions
    fun setUserList(data: List<User>) {
        lockUnlockScreen(false)
        loadingSpinner.visibility = View.GONE
        StateManager.userElementList = data.toMutableList()
        userList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setGroupList(data: List<User>) {
        lockUnlockScreen(false)
        loadingSpinner.visibility = View.GONE
        StateManager.groupElementList = data.toMutableList()
        groupList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setCompositionsList(data: List<Composition>, removeOld: Boolean = false) {
        lockUnlockScreen(false)
        loadingSpinner.visibility = View.GONE

        if (removeOld) {
            StateManager.compositionElementList.clear()
            StateManager.currentOffset = 0
        }
        StateManager.compositionElementList.addAll(data.filter { !it.url.isNullOrEmpty() })
        if (compositionList.adapter == null) {
            compositionList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, StateManager.compositionElementList, elementTouchListener)
        } else {
            (compositionList.adapter as ArrayAdapter<Composition>).notifyDataSetChanged()
        }

        StateManager.currentOffset += data.size
        if (StateManager.compositionElementList.size < StateManager.totalCompositions && data.isNotEmpty()) {
            musicService.getPlaylist(this, selectedElement, "", StateManager.currentOffset)
        } else {
            showDownloadAllButton()
        }
    }

    // actions
    private val elementTouchListener = { composition: Composition, view: View ->
        DownloadManager.downloadComposition(composition)
        view.bind<ImageView>(R.id.imageView).setImageDrawable(getDrawable(R.drawable.ic_downloading))
    }

    fun selectUserOrGroup(newSelectedElement: User?) {
        when (tabHost.currentTabTag) {
            "user" -> {
                selectedElement = newSelectedElement
                musicService.getPlaylist(this, selectedElement, filterText)
            }
            "group" -> {
                selectedElement = newSelectedElement
                musicService.getPlaylist(this, selectedElement, filterText)
            }
        }

        // TODO hide User and Group tabs
        tabHost.currentTab = 2

        newSelectedElement?.let {
            selectedUserContainer.visibility = View.VISIBLE
            selectedUserName.text = selectedElement?.fullname
            selectedUserId.text = selectedElement?.userId

            if (newSelectedElement.photo == null) {
                AsyncPhotoDownloader().execute(newSelectedElement, selectedUserPhoto)
            } else {
                selectedUserPhoto.setImageBitmap(newSelectedElement.photo)
            }

            // hiding download all until we have all tracks downloaded
            selectedUserDownloadButton.visibility = View.GONE
            selectedUserDownloadButton.setOnClickListener { StateManager.compositionElementList.forEach { DownloadManager.downloadComposition(it) } }

            selectedUserButton.setOnClickListener { selectedUserContainer.visibility = View.GONE }
        }
    }

    private fun showDownloadAllButton() {
        selectedUserDownloadButton.visibility = if (StateManager.enableDownloadAll) View.VISIBLE else View.GONE
    }

    private fun lockUnlockScreen(lock: Boolean) {
        textContainer.isFocusable = !lock
        textContainer.isClickable = !lock
        button.isFocusable = !lock
        button.isClickable = !lock
    }
}
