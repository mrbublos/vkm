package vkm.vkm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_search.*
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.UserListAdapter

class SearchFragment : Fragment() {

    // services
    private var musicService: MusicService = VkMusicService()

    // private vars
    private var filterText: String = ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.activity_search, container, false) as View

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        tabsSwiper.value = mutableListOf("user", "group", "tracks")
        tabsSwiper.setCurrentString(State.currentSearchTab)
        tabsSwiper.onSwiped = { _, tabName ->
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
        button.setOnClickListener { _ ->
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
        if (State.userElementList.isNotEmpty()) {
            setUserList(State.userElementList)
        }
        if (State.groupElementList.isNotEmpty()) {
            setGroupList(State.groupElementList)
        }
        if (State.compositionElementList.isNotEmpty()) {
            setCompositionsList(State.compositionElementList)
        }
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

        if (State.compositionElementList != data) {
            State.compositionElementList.addAll(data)
        }

        if (resultList.adapter == null) {
            resultList.adapter = CompositionListAdapter(this, context, R.layout.composition_list_element, State.compositionElementList, compositionTouchListener)
        } else {
            (resultList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }

        State.currentOffset += data.size
        if (State.compositionElementList.size < State.totalCompositions && data.isNotEmpty()) {
            musicService.getPlaylist(this, State.selectedElement, "", State.currentOffset)
        } else {
            showDownloadAllButton()
        }
    }

    // actions
    private val compositionTouchListener = { composition: Composition, view: View ->
        if (!DownloadManager.getDownloaded().contains(composition)) {
            DownloadManager.downloadComposition(composition)
            val actionButton = view.bind<ImageView>(R.id.imageView)
            actionButton.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
            actionButton.setOnClickListener {}
        }
    }

    private fun selectUserOrGroup(newSelectedElement: User?) {
        selectedUserContainer.visibility = View.GONE
        State.selectedElement = newSelectedElement

        StateManager.compositionElementList.clear()
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
                State.compositionElementList.forEach { DownloadManager.downloadComposition(it) }
                (resultList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
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
