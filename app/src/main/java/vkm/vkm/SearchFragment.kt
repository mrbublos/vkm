package vkm.vkm

import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.composition_list_element.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import vkm.vkm.R.layout.*
import vkm.vkm.adapters.AlbumListAdapter
import vkm.vkm.adapters.ArtistListAdapter
import vkm.vkm.adapters.CompositionListAdapter
import vkm.vkm.utils.*
import vkm.vkm.utils.db.Db

class SearchFragment : VkmFragment() {

    // private vars
    private var filterText: String = ""
    private var currentElement = 0
    private var tabs: List<Tab<*>> = listOf()
    private val currentTab: Tab<*>
    get() = tabs[State.currentSearchTab]

    init { layout = activity_search }

    override fun init() {
        tabs = listOf(TracksTab(::drawData), NewAlbumsTab(::drawData), ChartTab(::drawData), ArtistTab(::drawData))
        initializeElements()
        initializeTabs()
        initializeButton()
    }

    private fun initializeElements() {
        showSpinner(false)
        search.inputType = if (State.enableTextSuggestions) TYPE_CLASS_TEXT else TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        resultList.setOnScrollListener(object : AbsListView.OnScrollListener {
            private var resultVisibleIndex = 0
            private var resultVisible = 0
            override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                resultVisibleIndex = firstVisibleItem
                resultVisible = visibleItemCount
            }

            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                if (scrollState == SCROLL_STATE_IDLE && resultVisibleIndex + resultVisible >= currentTab.dataList.size) {
                    currentElement = resultVisibleIndex + resultVisible
                    currentTab.onBottomReached()
                }
            }
        })
    }

    private fun initializeTabs() {
        searchTabsSwiper.value = tabs.asSequence().map { it.name }.toMutableList()
        searchTabsSwiper.setCurrentString(currentTab.name)
        currentTab.activate(null)

        searchTabsSwiper.onSwiped = { index, _, prev ->
            State.currentSearchTab = index
            tabs[index].activate(null)
            lockScreen(true)
            tabs[prev].deactivate()
            searchPanel.visibility = if (currentTab.hideSearch) GONE else VISIBLE
        }
    }

    private fun initializeButton() {
        blockSearch(false)
        searchButton.setOnClickListener {
            filterText = search.text.toString()
            if (currentTab.search(filterText)) { lockScreen(true) }
            currentElement = 0
            return@setOnClickListener
        }
    }

    private fun drawData() {
        val me = this
        launch(UI) {
            lockScreen(false)

            val data = currentTab.dataList
            resultList.adapter = when (currentTab.listType) {
                ListType.Composition -> CompositionListAdapter(me, composition_list_element, data as MutableList<Composition>, compositionAction)
                ListType.Album -> AlbumListAdapter(me, album_list_element, data as MutableList<Album>, ::compositionContainerAction)
                ListType.Artist -> ArtistListAdapter(me, album_list_element, data as MutableList<Artist>, ::compositionContainerAction)
            }

            resultList.setSelection(currentElement)

            // TODO see how slow it is
            getContext()?.let { HttpUtils.storeProxies(Db.instance(it).proxyDao()) }
        }
    }

    // actions
    private fun blockSearch(locked: Boolean) {
        searchButton.isFocusable = !locked
        searchButton.isClickable = !locked
    }

    private fun showSpinner(show: Boolean) {
        resultList.visibility = if (!show) VISIBLE else GONE
        loadingSpinner.visibility = if (show) VISIBLE else GONE
    }

    private fun lockScreen(locked: Boolean) {
        launch(UI) {
            blockSearch(currentTab.loading || locked)
            showSpinner(currentTab.loading || locked)
        }
    }

    private val compositionAction = { composition: Composition, view: View ->
        if (!DownloadManager.getDownloaded().contains(composition)) {
            DownloadManager.downloadComposition(composition)
            val actionButton = view.imageView
            actionButton.setImageDrawable(context!!.getDrawable(R.drawable.ic_downloading))
            actionButton.setOnClickListener {}
        }
    }

    private fun compositionContainerAction(item: CompositionContainer, view: View) {
        lockScreen(true)
        switchTo("tracks")
        (this@SearchFragment.currentTab as Tab<Composition>).activate(item.compositionFetcher)
    }

    private fun switchTo(name: String) {
        searchTabsSwiper.setCurrentString(name)
        switchTo(getTabIndex(name))
    }

    private fun switchTo(index: Int) {
        State.currentSearchTab = index
        searchPanel.visibility = if (currentTab.hideSearch) GONE else VISIBLE
    }

    private fun getTabIndex(name: String): Int {
        return tabs.asSequence().map { it.name }.indexOf(name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentTab.deactivate()
        tabs.forEach { it.destroy() }
    }
}
