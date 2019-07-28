package vkm.vkm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

typealias SearchTabCallback = () -> Unit

abstract class Tab<T>(var refreshTab: SearchTabCallback, var name: String, val listType: ListType) {

    var page: Int = 0
    var filter: String = ""
    var dataList: MutableList<T> = mutableListOf()
    var lastPopulated: Long = 0
    var active: Boolean = false
    var loading: Boolean = false
    var nextPageLoader: suspend (page: Int) -> List<T> = { listOf() }

    open val hideSearch = false

    open fun activate(data: List<T>?) {
        active = true
        data?.let {
            dataList = it.toMutableList()
            page = NO_MORE_PAGES
        }
        setData(dataList)
    }

    open fun activate(nextPageLoader: suspend (page: Int) -> List<T>) {
        active = true
        this.nextPageLoader = nextPageLoader
        page = 0
        loadNewPage()
    }

    open fun deactivate() {
        active = false
    }

    open fun destroy() { refreshTab = { } }

    open fun onBottomReached() {
        if (loading) { return }
        if (page == NO_MORE_PAGES) { return }

        loading = true
        page++
        loadNewPage()
    }

    abstract fun search(query: String): Boolean

    open fun setData(data: List<T>) {
        loading = false
        if (page == 0) { dataList.clear() }
        if (data.isEmpty()) { page = NO_MORE_PAGES }
        dataList.addAll(data)
        refreshTab()
    }

    open fun loadNewPage() {
        GlobalScope.launch(Dispatchers.IO) {
            loading = true
            setData(nextPageLoader(page))
        }
    }

    companion object {
        const val NO_MORE_PAGES = -1
    }
}

enum class ListType {
    Composition,
    Album,
    Artist
}