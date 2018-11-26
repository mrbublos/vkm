package vkm.vkm

typealias SearchTabCallback = () -> Unit

abstract class Tab<T>(var callback: SearchTabCallback, var name: String, val listType: ListType) {

    var page: Int = 0
    var filter: String = ""
    var dataList: MutableList<T> = mutableListOf()
    var lastPopulated: Long = 0
    var active: Boolean = false
    var loading: Boolean = false
    var nextPageLoader: ((page: Int) -> MutableList<T>)? = null

    open val hideSearch = false

    open fun activate(data: List<T>?) {
        active = true
    }

    open fun deactivate() {
        active = false
    }

    open fun destroy() { callback = { } }

    open fun onBottomReached() {}

    abstract fun search(query: String)

    open fun onDataFetched(data: MutableList<T>) {
        loading = false
        if (page == 0) { dataList.clear() }
        if (data.isEmpty()) { page = NO_MORE_PAGES }
        dataList.addAll(data)
        callback()
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