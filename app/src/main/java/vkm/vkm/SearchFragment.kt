package vkm.vkm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_search.view.*
import vkm.vkm.utils.CompositionListAdapter
import vkm.vkm.utils.UserListAdapter

class SearchFragment : Fragment() {

    // services
    private var musicService: MusicService = VkMusicService()

    // private vars
    private var filterText: String = ""
    lateinit private var me: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        me = inflater?.inflate(R.layout.activity_search, container, false) as View
        init()
        return me
    }

    private fun init() {
        initializeElements()
        initializeTabs()
        initializeButton()
        initializeLists()
    }

    private fun initializeElements() {
        selectUserOrGroup(State.selectedElement)
        spinner(false)
        me.search.inputType = if (State.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun initializeTabs() {
        me.tabsSwiper.value = mutableListOf("user", "group", "tracks")
        me.tabsSwiper.setCurrentString(State.currentSearchTab)
        me.tabsSwiper.onSwiped = { _, tabName ->
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
        me.button.setOnClickListener { _ ->
            filterText = me.search.text.toString()
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
                    me.resultList.adapter = null
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
        me.resultList.adapter = UserListAdapter(context, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setGroupList(data: List<User>) {
        screen(false)
        spinner(false)
        State.groupElementList = data.toMutableList()
        me.resultList.adapter = UserListAdapter(context, R.layout.composition_list_element, data, this::selectUserOrGroup)
    }

    fun setCompositionsList(data: List<Composition>) {
        screen(false)
        spinner(false)

        if (State.compositionElementList != data) {
            State.compositionElementList.addAll(data)
        }

        if (me.resultList.adapter == null) {
            me.resultList.adapter = CompositionListAdapter(this, context, R.layout.composition_list_element, State.compositionElementList, compositionTouchListener)
        } else {
            (me.resultList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
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
        me.selectedUserContainer.visibility = View.GONE
        State.selectedElement = newSelectedElement

        musicService.getPlaylist(this, newSelectedElement, filterText)

        me.tabsSwiper.setCurrentString("tracks")

        newSelectedElement?.let {
            me.selectedUserContainer.visibility = View.VISIBLE
            me.selectedUserName.text = it.fullname
            me.selectedUserId.text = it.userId

            if (it.photo == null) {
                UserListAdapter.schedulePhotoDownload(me.selectedUserPhoto, it)
            } else {
                me.selectedUserPhoto.setImageBitmap(it.photo)
            }

            // hiding download all until we have all tracks downloaded
            me.selectedUserDownloadAllButton.visibility = View.GONE
            me.selectedUserDownloadAllButton.setOnClickListener {
                spinner(true)
                screen(true)
                me.selectedUserDownloadAllButton.visibility = View.GONE
                State.compositionElementList.forEach { DownloadManager.downloadComposition(it) }
                (me.resultList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                screen(false)
                spinner(false)
            }

            me.deselectUserButton.setOnClickListener {
                me.selectedUserContainer.visibility = View.GONE
                State.selectedElement = null
            }
        }
    }

    private fun showDownloadAllButton() {
        me.selectedUserDownloadAllButton.visibility = if (State.enableDownloadAll) View.VISIBLE else View.GONE
    }

    override fun onStop() {
        super.onStop()
        MusicPlayer.stop()
    }

    private fun screen(locked: Boolean) {
        me.button.isFocusable = !locked
        me.button.isClickable = !locked
    }

    private fun spinner(show: Boolean) {
        me.loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }
}
