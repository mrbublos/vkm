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
import java.lang.ref.WeakReference

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
        swipeCatcher.activity = WeakReference(this)

        selectUserOrGroup(StateManager.selectedElement)
        spinner(false)

        MusicPlayer.context = this

        search.inputType = if (StateManager.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun initializeTabs() {
        tabsSwiper.value = mutableListOf("user", "group", "tracks")
        tabsSwiper.setCurrentString(StateManager.currentSearchTab)
        tabsSwiper.onSwiped = { _, tabName ->
            StateManager.currentSearchTab = tabName
            when (StateManager.currentSearchTab) {
                "user" -> setUserList(StateManager.userElementList)
                "group" -> setGroupList(StateManager.groupElementList)
                "tracks" -> setCompositionsList(StateManager.compositionElementList)
            }
        }
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

            when (StateManager.currentSearchTab) {
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
                    resultList.adapter = null
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
        resultList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setGroupList(data: List<User>) {
        screen(false)
        spinner(false)
        StateManager.groupElementList = data.toMutableList()
        resultList.adapter = UserListAdapter(this, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setCompositionsList(data: List<Composition>) {
        screen(false)
        spinner(false)

        if (StateManager.compositionElementList != data) {
            StateManager.compositionElementList.addAll(data)
        }

        if (resultList.adapter == null) {
            resultList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, StateManager.compositionElementList, compositionTouchListener)
        } else {
            (resultList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
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

        tabsSwiper.setCurrentString("tracks")

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
                (resultList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
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
