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
    val selectedUserDownloadAllButton by bind<ImageView>(R.id.download_all_user_button)

    // services
    val musicService = MusicService()

    // private vars
    var filterText: String = ""
    var selectedElement: User? = null
    var selectedGroup: User? = null
    var totalCompositions = 0
    var currentOffset = 0

    val sw: SwipeManager by lazy { SwipeManager(this) }
    val compositionElementList = mutableListOf<Composition>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(MainActivity.TAG, "Setting content view")
        setContentView(R.layout.activity_search)

        initializeElements()
        initializeTabs()
        initializeButton()
    }

    private fun initializeElements() {
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

        tabHost.setCurrentTabByTag("tracks")
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

    fun setCompositionsList(data: List<Composition>, removeOld: Boolean = false) {
        lockUnlockScreen(false)
        loadingSpinner.visibility = View.GONE

        if (removeOld) {
            compositionElementList.clear()
            currentOffset = 0
        }
        compositionElementList.addAll(data.filter { !it.url.isNullOrEmpty() })
        if (compositionList.adapter == null) {
            compositionList.adapter = CompositionListAdapter(this, R.layout.composition_list_element, compositionElementList, elementTouchListener)
        } else {
            (compositionList.adapter as ArrayAdapter<Composition>).notifyDataSetChanged()
        }

        currentOffset += data.size
        if (compositionElementList.size < totalCompositions && data.isNotEmpty()) { musicService.getPlaylist(this, selectedElement, "", currentOffset)}
    }

    val elementTouchListener = { composition: Composition, view: View ->
        DownloadManager.downloadComposition(composition)
        view.bind<ImageView>(R.id.imageView).setImageDrawable(getDrawable(R.drawable.ic_downloading))
    }

    private val selectUserOrGroup = { newSelectedElement: User? ->
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

            selectedUserButton.setOnTouchListener { _, event ->
                selectedUserContainer.visibility = View.GONE
                return@setOnTouchListener super.onTouchEvent(event)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        sw.mDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun lockUnlockScreen(lock: Boolean) {
        textContainer.isFocusable = !lock
        textContainer.isClickable = !lock
        button.isFocusable = !lock
        button.isClickable = !lock
    }
}
