package vkm.vkm

import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.ListAdapter
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
import kotlin.reflect.KClass

class SearchFragment : VkmFragment() {

    // private vars
    private var filterText: String = ""
    private var currentElement = 0
    private var tabs: List<Tab<*>> = listOf()
    private val currentTab: Tab<*>
    get() = tabs[State.currentSearchTab]

    init { layout = activity_search }

    override fun init() {
        tabs = listOf(TracksTab(::setDataList), NewAlbumsTab(::setDataList), ChartTab(::setDataList), ArtistTab(::setDataList))
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
                val tab = tabs[State.currentSearchTab]
                if (scrollState == SCROLL_STATE_IDLE && resultVisibleIndex + resultVisible >= tab.dataList.size) {
                    currentElement = resultVisibleIndex + resultVisible
                    tab.onBottomReached()
                }
            }
        })
    }

    private fun initializeTabs() {
        searchTabsSwiper.value = tabs.asSequence().map { it.name }.toMutableList()
        searchTabsSwiper.setCurrentString(tabs[State.currentSearchTab].name)
        tabs[State.currentSearchTab].activate(null)

        searchTabsSwiper.onSwiped = { index, _, prev ->
            lockScreen(true)
            showSpinner(true)

            State.currentSearchTab = index
            tabs[index].activate(null)
            tabs[prev].deactivate()
            searchPanel.visibility = if (tabs[index].hideSearch) GONE else VISIBLE
        }
    }

    private fun initializeButton() {
        lockScreen(false)
        searchButton.setOnClickListener { _ ->
            filterText = search.text.toString()
            showSpinner(true)
            lockScreen(true)
            tabs[State.currentSearchTab].search(filterText)
            currentElement = 0
            return@setOnClickListener
        }
    }

    private fun setDataList(data: MutableList<out Any>, adaptorClass: KClass<out ListAdapter>) {
        val me = this
        launch(UI) {
            lockScreen(false)
            showSpinner(false)

            resultList.adapter = when (adaptorClass) {
                CompositionListAdapter::class -> CompositionListAdapter(me, composition_list_element, data as MutableList<Composition>, compositionAction)
                AlbumListAdapter::class -> AlbumListAdapter(me, album_list_element, data as MutableList<Album>, ::compositionContainerAction)
                ArtistListAdapter::class -> ArtistListAdapter(me, album_list_element, data as MutableList<Artist>, ::compositionContainerAction)
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
        resultList.visibility = if (!show) VISIBLE else GONE
        loadingSpinner.visibility = if (show) VISIBLE else GONE
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
        showSpinner(true)

        val fromTab = State.currentSearchTab

        switchTo("tracks")
        launch(CommonPool) {
            val fetchCompositions = item.compositionFetcher
            if (fetchCompositions == null) {
                "Error loading item tracks".toast(getContext())
                switchTo(fromTab)
                launch(UI) {
                    lockScreen(false)
                    showSpinner(false)
                }
                return@launch
            }

            fetchCompositions()
            var retries = 0
            while (item.compositions == null && retries++ < 10) { delay(1000) }
            val compositions = item.compositions
            if (compositions == null || compositions.isEmpty()) {
                "Error loading item tracks".toast(getContext())
                switchTo(fromTab)
            } else {
                (this@SearchFragment.currentTab as Tab<Composition>).activate(compositions)
            }

            launch(UI) {
                lockScreen(false)
                showSpinner(false)
            }
        }
    }

    private fun switchTo(name: String) {
        searchTabsSwiper.setCurrentString(name)
        switchTo(getTabIndex(name))
    }

    private fun switchTo(index: Int) {
        State.currentSearchTab = index
        searchPanel.visibility = if (tabs[State.currentSearchTab].hideSearch) GONE else VISIBLE
    }

    private fun getTabIndex(name: String): Int {
        return tabs.asSequence().map { it.name }.indexOf(name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabs[State.currentSearchTab].deactivate()
        tabs.forEach { it.destroy() }
    }
}
