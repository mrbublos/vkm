package vkm.vkm

import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.composition_list_element.view.*
import vkm.vkm.utils.*

class SearchFragment : VkmFragment() {

    // services
    private var musicService: MusicService = VkMusicService()

    // private vars
    private var filterText: String = ""

    init { layout = R.layout.activity_search }

    override fun init() {
        initializeElements()
        initializeTabs()
        initializeButton()
        initializeLists()
    }

    private fun initializeElements() {
        selectUserOrGroup(State.selectedElement)
        spinner(false)
        search.inputType = if (State.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun initializeTabs() {
        searchTabsSwiper.value = mutableListOf("user", "group", "tracks")
        searchTabsSwiper.setCurrentString(State.currentSearchTab)
        searchTabsSwiper.onSwiped = { _, tabName ->
            State.currentSearchTab = tabName
            when (State.currentSearchTab) {
                "user" -> setUserList(State.userElementList)
                "group" -> setGroupList(State.groupElementList)
                "tracks" -> setCompositionsList(State.compositionElementList)
            }
        }
    }

    private fun initializeButton() {
        screen(false)
        searchButton.setOnClickListener { _ ->
            filterText = search.text.toString()
            if (filterText.isEmpty()) { return@setOnClickListener }

            spinner(true)
            screen(true)

            when (State.currentSearchTab) {
                "user" -> {
                    if (State.selectedElement != null) {
                        musicService.getPlaylist(this, State.selectedElement, filterText)
                        State.compositionElementList.clear()
                        State.currentOffset = 0
                    } else {
                        musicService.getUsers(this, filterText)
                    }
                }
                "group" -> {
                    if (State.selectedElement != null) {
                        musicService.getPlaylist(this, State.selectedElement, filterText)
                        State.compositionElementList.clear()
                        State.currentOffset = 0
                    } else {
                        musicService.getGroups(this, filterText)
                    }
                }
                "tracks" -> {
                    State.compositionElementList.clear()
                    State.currentOffset = 0
                    musicService.getCompositions(this, filterText)
                    resultList.adapter = null
                }
            }

            return@setOnClickListener
        }
    }

    private fun initializeLists() {
        if (State.userElementList.isNotEmpty()) { setUserList(State.userElementList) }
        if (State.groupElementList.isNotEmpty()) { setGroupList(State.groupElementList) }
        if (State.compositionElementList.isNotEmpty()) { setCompositionsList(State.compositionElementList) }
    }

    // callback functions
    fun setUserList(data: List<User>) {
        screen(false)
        spinner(false)
        State.userElementList = data.toMutableList()
        resultList.adapter = UserListAdapter(context, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setGroupList(data: List<User>) {
        screen(false)
        spinner(false)
        State.groupElementList = data.toMutableList()
        resultList.adapter = UserListAdapter(context, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setCompositionsList(data: List<Composition>) {
        screen(false)
        spinner(false)

        if (State.compositionElementList != data) { State.compositionElementList.addAll(data) }

        resultList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, State.compositionElementList, compositionAction)

        State.currentOffset += data.size
        if (State.compositionElementList.size < State.totalCompositions && data.isNotEmpty()) {
            musicService.getPlaylist(this, State.selectedElement, "", State.currentOffset)
        } else {
            showDownloadAllButton()
        }
    }

    // actions
    private val compositionAction = { composition: Composition, view: View ->
        if (!DownloadManager.getDownloaded().contains(composition)) {
            DownloadManager.downloadComposition(composition)
            val actionButton = view.imageView
            actionButton.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
            actionButton.setOnClickListener {}
        }
    }

    private fun selectUserOrGroup(newSelectedElement: User?) {
        selectedUserContainer.visibility = View.GONE
        State.selectedElement = newSelectedElement

        State.compositionElementList.clear()
        musicService.getPlaylist(this, newSelectedElement, filterText)

        searchTabsSwiper.setCurrentString("tracks")
        State.currentSearchTab = "tracks"

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
                State.compositionElementList.forEach { DownloadManager.downloadComposition(it) }
                (resultList.adapter as BaseAdapter).notifyDataSetChanged()
                screen(false)
                spinner(false)
            }

            deselectUserButton.setOnClickListener {
                selectedUserContainer.visibility = View.GONE
                State.selectedElement = null
            }
        }
    }

    private fun showDownloadAllButton() {
        selectedUserDownloadAllButton.visibility = if (State.enableDownloadAll) View.VISIBLE else View.GONE
    }

    private fun screen(locked: Boolean) {
        searchButton.isFocusable = !locked
        searchButton.isClickable = !locked
    }

    private fun spinner(show: Boolean) {
        loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }
}
