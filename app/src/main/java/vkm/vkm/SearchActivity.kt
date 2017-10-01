package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_search.*
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.UserListAdapter

class SearchActivity : AppCompatActivity() {

    // services
    private var musicService: MusicService = VkMusicService()

    // private vars
    private var filterText: String = ""

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

        selectUserOrGroup(StateManager.selectedElement)
        spinner(false)

        MusicPlayer.context = this

        search.inputType = if (StateManager.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun initializeTabs() {
        // TODO consider switching to tabLayout
        tabHost.setup()

        var tabSpec = tabHost.newTabSpec("user")
        tabSpec.setIndicator(getString(R.string.tab_user))
        tabSpec.setContent(R.id.userList)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("group")
        tabSpec.setIndicator(getString(R.string.tab_group))
        tabSpec.setContent(R.id.groupList)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("tracks")
        tabSpec.setIndicator(getString(R.string.tab_composition))
        tabSpec.setContent(R.id.compositionList)
        tabHost.addTab(tabSpec)

        tabHost.setCurrentTabByTag(StateManager.currentSearchTab)
        tabHost.setOnTabChangedListener { tabId -> StateManager.currentSearchTab = tabId }
    }

    private fun initializeButton() {
        screen(false)
        button.setOnClickListener { _ ->
            filterText = search.text.toString()
            if (filterText.isEmpty()) {
                return@setOnClickListener
            }

            spinner(true)
            screen(true)

            when (tabHost.currentTabTag) {
                "user" -> {
                    if (StateManager.selectedElement != null) {
                        musicService.getPlaylist(this, StateManager.selectedElement, filterText)
                        StateManager.compositionElementList.clear()
                        StateManager.currentOffset = 0
                    } else {
                        musicService.getUsers(this, filterText)
                    }
                }
                "group" -> {
                    if (StateManager.selectedElement != null) {
                        musicService.getPlaylist(this, StateManager.selectedElement, filterText)
                        StateManager.compositionElementList.clear()
                        StateManager.currentOffset = 0
                    } else {
                        musicService.getGroups(this, filterText)
                    }
                }
                "tracks" -> {
                    StateManager.compositionElementList.clear()
                    StateManager.currentOffset = 0
                    musicService.getCompositions(this, filterText)
                }
            }

            return@setOnClickListener
        }
    }

    private fun initializeLists() {
        if (StateManager.userElementList.isNotEmpty()) {
            setUserList(StateManager.userElementList)
        }
        if (StateManager.groupElementList.isNotEmpty()) {
            setGroupList(StateManager.groupElementList)
        }
        if (StateManager.compositionElementList.isNotEmpty()) {
            setCompositionsList(StateManager.compositionElementList)
        }
    }

    // callback functions
    fun setUserList(data: List<User>) {
        screen(false)
        spinner(false)
        StateManager.userElementList = data.toMutableList()
        userList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setGroupList(data: List<User>) {
        screen(false)
        spinner(false)
        StateManager.groupElementList = data.toMutableList()
        groupList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setCompositionsList(data: List<Composition>) {
        screen(false)
        spinner(false)

        if (StateManager.compositionElementList != data) {
            StateManager.compositionElementList.addAll(data)
        }

        if (compositionList.adapter == null) {
            compositionList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, StateManager.compositionElementList, compositionTouchListener)
        } else {
            (compositionList.adapter as ArrayAdapter<Composition>).notifyDataSetChanged()
        }

        StateManager.currentOffset += data.size
        if (StateManager.compositionElementList.size < StateManager.totalCompositions && data.isNotEmpty()) {
            musicService.getPlaylist(this, StateManager.selectedElement, "", StateManager.currentOffset)
        } else {
            showDownloadAllButton()
        }
    }

    // actions
    private val compositionTouchListener = { composition: Composition, view: View ->
        if (!DownloadManager.getDownloaded().contains(composition)) {
            DownloadManager.downloadComposition(composition)
            val actionButton = view.bind<ImageView>(R.id.imageView)
            actionButton.setImageDrawable(getDrawable(R.drawable.ic_downloading))
            actionButton.setOnClickListener {}
        }
    }

    fun selectUserOrGroup(newSelectedElement: User?) {
        selectedUserContainer.visibility = View.GONE
        StateManager.selectedElement = newSelectedElement

        musicService.getPlaylist(this, newSelectedElement, filterText)

        // TODO hide User and Group tabs
        tabHost.currentTab = 2

        newSelectedElement?.let {
            selectedUserContainer.visibility = View.VISIBLE
            selectedUserName.text = it.fullname
            selectedUserId.text = it.userId

            if (it.photo == null) {
                UserListAdapter.schedulePhotoDownload(selectedUserPhoto, it)
            } else {
                selectedUserPhoto.setImageBitmap(it.photo)
            }

            // hiding download all until we have all tracks downloaded
            selectedUserDownloadAllButton.visibility = View.GONE
            selectedUserDownloadAllButton.setOnClickListener {
                spinner(true)
                screen(true)
                selectedUserDownloadAllButton.visibility = View.GONE
                StateManager.compositionElementList.forEach { DownloadManager.downloadComposition(it) }
                (compositionList.adapter as ArrayAdapter<Composition>).notifyDataSetChanged()
                screen(false)
                spinner(false)
            }

            deselectUserButton.setOnClickListener {
                selectedUserContainer.visibility = View.GONE
                StateManager.selectedElement = null
            }
        }
    }

    private fun showDownloadAllButton() {
        selectedUserDownloadAllButton.visibility = if (StateManager.enableDownloadAll) View.VISIBLE else View.GONE
    }

    override fun onStop() {
        super.onStop()
        MusicPlayer.stop()
    }

    private fun screen(locked: Boolean) {
        button.isFocusable = !locked
        button.isClickable = !locked
    }

    private fun spinner(show: Boolean) {
        loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }
}
