package vkm.vkm

import android.text.InputType
import android.view.View
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.composition_list_element.view.*
import vkm.vkm.utils.*

class SearchFragment : VkmFragment() {

    // services
    private var musicService: MusicService = VkMusicService()

    // private vars
    private var filterText: String = ""
    private var currentElement = 0

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

        resultList.setOnScrollListener(object : AbsListView.OnScrollListener {
            private var resultVisibleIndex = 0
            private var resultVisible = 0
            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                resultVisibleIndex = firstVisibleItem
                resultVisible = visibleItemCount
            }

            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                if (State.currentSearchTab != "tracks") { return }
                val size = State.compositionElementList.size
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && resultVisibleIndex + resultVisible >= size && State.currentOffset < State.totalCompositions) {
                    currentElement = resultVisibleIndex + resultVisible
                    musicService.getCompositions(this@SearchFragment, filterText, State.currentOffset)
                }
            }
        })
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
                        if (musicService.getPlaylist(this, State.selectedElement, filterText)) {
                            currentElement = 0
                            State.compositionElementList.clear()
                            State.currentOffset = 0
                        }
                    } else {
                        musicService.getUsers(this, filterText)
                    }
                }
                "group" -> {
                    if (State.selectedElement != null) {
                        if (musicService.getPlaylist(this, State.selectedElement, filterText)) {
                            currentElement = 0
                            State.compositionElementList.clear()
                            State.currentOffset = 0
                        }
                    } else {
                        musicService.getGroups(this, filterText)
                    }
                }
                "tracks" -> {
                    if (musicService.getCompositions(this, filterText)) {
                        currentElement = 0
                        State.compositionElementList.clear()
                        State.currentOffset = 0
                        resultList.adapter = null
                    }
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

    fun setCompositionsList(data: List<Composition>, isPlaylist: Boolean = false) {
        screen(false)
        spinner(false)

        State.currentOffset += data.size
        val filteredData = data.filter { it.url.isNotEmpty() }

        // to prevent duplications when restoring list
        if (State.compositionElementList != data) { State.compositionElementList.addAll(filteredData) }

        resultList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, State.compositionElementList, compositionAction)
        resultList.setSelection(currentElement)

        // if no data returned, so whether we have not found anything or whether no more elements available in the search
        if (data.isEmpty()) { State.currentOffset = State.totalCompositions }

        if (isPlaylist) {
            // fetching complete playlist, because user can download it all at once
            if (State.compositionElementList.size < State.totalCompositions && filteredData.isNotEmpty()) {
                musicService.getPlaylist(this, State.selectedElement, "", State.currentOffset)
            } else {
                showDownloadAllButton()
            }
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
        resultList.visibility = if (!show) View.VISIBLE else View.GONE
        loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }
}
