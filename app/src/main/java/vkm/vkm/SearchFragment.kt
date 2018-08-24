package vkm.vkm

import android.text.InputType
import android.view.View
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.ListAdapter
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.composition_list_element.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import vkm.vkm.adapters.AlbumListAdapter
import vkm.vkm.adapters.CompositionListAdapter
import vkm.vkm.utils.Album
import vkm.vkm.utils.Composition
import vkm.vkm.utils.VkmFragment
import vkm.vkm.utils.toast
import kotlin.reflect.KClass

class SearchFragment : VkmFragment() {

    // private vars
    private var filterText: String = ""
    private var currentElement = 0
    private var tabs = listOf(TracksTab(::setDataList), NewAlbumsTab(::setDataList), ChartTab(::setDataList))
    private val currentTab: Tab<*>
    get() = tabs[State.currentSearchTab]

    init { layout = R.layout.activity_search }

    override fun init() {
        initializeElements()
        initializeTabs()
        initializeButton()
    }

    private fun initializeElements() {
        showSpinner(false)
        search.inputType = if (State.enableTextSuggestions) InputType.TYPE_CLASS_TEXT else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        resultList.setOnScrollListener(object : AbsListView.OnScrollListener {
            private var resultVisibleIndex = 0
            private var resultVisible = 0
            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                resultVisibleIndex = firstVisibleItem
                resultVisible = visibleItemCount
            }

            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                val tab = tabs[State.currentSearchTab]
                if (scrollState == SCROLL_STATE_IDLE && resultVisibleIndex + resultVisible >= tab.dataList.size) {
                    currentElement = resultVisibleIndex + resultVisible
                    tab.onBottomReached()
                }
            }
        })
    }

    private fun initializeTabs() {
        searchTabsSwiper.value = tabs.map { it.name }.toMutableList()
        searchTabsSwiper.setCurrentString(tabs[State.currentSearchTab].name)
        searchTabsSwiper.onSwiped = { index, _, prev ->
            lockScreen(true)
            showSpinner(true)

            State.currentSearchTab = index
            tabs[index].activate(null)
            tabs[prev].deactivate()
            searchPanel.visibility = if (tabs[index].hideSearch) View.GONE else View.VISIBLE
        }
    }

    private fun initializeButton() {
        lockScreen(false)
        searchButton.setOnClickListener { _ ->
            filterText = search.text.toString()
            showSpinner(true)
            lockScreen(true)
            tabs[State.currentSearchTab].search(filterText)
            return@setOnClickListener
        }
    }

    private fun setDataList(data: MutableList<out Any>, adaptorClass: KClass<out ListAdapter>) {
        val me = this
        launch(UI) {
            lockScreen(false)
            showSpinner(false)

            resultList.adapter = when (adaptorClass) {
                CompositionListAdapter::class -> CompositionListAdapter(me, R.layout.composition_list_element, data as MutableList<Composition>, compositionAction)
                AlbumListAdapter::class -> AlbumListAdapter(me, R.layout.album_list_element, data as MutableList<Album>, ::albumAction)
                else -> throw Exception()
            }

            resultList.setSelection(currentElement)
        }
    }

    // actions

    private fun lockScreen(locked: Boolean) {
        searchButton.isFocusable = !locked
        searchButton.isClickable = !locked
    }

    private fun showSpinner(show: Boolean) {
        resultList.visibility = if (!show) View.VISIBLE else View.GONE
        loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }

    private val compositionAction = { composition: Composition, view: View ->
        if (!DownloadManager.getDownloaded().contains(composition)) {
            DownloadManager.downloadComposition(composition)
            val actionButton = view.imageView
            actionButton.setImageDrawable(context!!.getDrawable(R.drawable.ic_downloading))
            actionButton.setOnClickListener {}
        }
    }

    private fun albumAction(album: Album, view: View) {
        lockScreen(true)
        showSpinner(true)

        switchTo("tracks")
        launch(CommonPool) {
            val fetchCompositions = album.compositionFetcher
            if (fetchCompositions == null) {
                "Error loading album tracks".toast(getContext())
                switchTo("new")
                launch(UI) {
                    lockScreen(false)
                    showSpinner(false)
                }
                return@launch
            }

            fetchCompositions()
            var retries = 0
            while (album.compositions == null && retries++ < 10) {
                delay(1000)
            }
            if (album.compositions == null || album.compositions!!.isEmpty()) {
                "Error loading album tracks".toast(getContext())
                switchTo("new")
            } else {
                (currentTab as Tab<Composition>).activate(album.compositions)
            }

            launch(UI) {
                lockScreen(false)
                showSpinner(false)
            }
        }
    }

    private fun switchTo(name: String) {
        searchTabsSwiper.setCurrentString(name)
        State.currentSearchTab = getTabIndex(name)
        searchPanel.visibility = if (tabs[State.currentSearchTab].hideSearch) View.INVISIBLE else View.VISIBLE
    }

    private fun getTabIndex(name: String): Int {
        return tabs.map { it.name }.indexOf(name)
    }
}
