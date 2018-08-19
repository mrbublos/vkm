package vkm.vkm

import android.text.InputType
import android.view.View
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.*
import android.widget.BaseAdapter
import android.widget.ListAdapter
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.composition_list_element.view.*
import vkm.vkm.utils.*
import kotlin.reflect.KClass

class SearchFragment : VkmFragment() {

    // private vars
    private var filterText: String = ""
    private var currentElement = 0
    private var tabs = listOf<Tab<out Any>>(TracksTab(::setDataList))

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
            State.currentSearchTab = index
            tabs[index].activate()
            tabs[prev].deactivate()
        }
    }

    private fun initializeButton() {
        lockScreen(false)
        searchButton.setOnClickListener { _ ->
            filterText = search.text.toString()
            if (filterText.isEmpty()) { return@setOnClickListener }

            showSpinner(true)
            lockScreen(true)
            tabs[State.currentSearchTab].search(filterText)
            return@setOnClickListener
        }
    }

    private fun setDataList(data: MutableList<out Any>, adaptorClass: KClass<out ListAdapter>) {
        lockScreen(false)
        showSpinner(false)

        resultList.adapter = when (adaptorClass) {
            CompositionListAdapter::class -> CompositionListAdapter(this, R.layout.composition_list_element, data as MutableList<Composition>, compositionAction)
            else -> throw Exception()
        }

        resultList.setSelection(currentElement)
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
}
