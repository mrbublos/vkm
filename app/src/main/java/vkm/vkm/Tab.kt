package vkm.vkm

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

typealias SearchTabCallback = () -> Unit

abstract class Tab<T>(callback: SearchTabCallback, var name: String, val listType: ListType) {

    var currentOffset: Int by TabStateDelegate<Int, T>()
    var filter: String by TabStateDelegate<String, T>()
    var callback: SearchTabCallback by TabStateDelegate<SearchTabCallback, T>()
    var dataList: MutableList<T> by TabStateDelegate<MutableList<T>, T>()
    var lastPopulated: Long by TabStateDelegate<Long, T>()
    var active: Boolean by TabStateDelegate<Boolean, T>()
    var loading: Boolean by TabStateDelegate<Boolean, T>()

    open val hideSearch = false

    init {
        State.tabState.putIfAbsent(name, ConcurrentHashMap())
        val tabState = State.tabState[name]
        currentOffset = tabState?.get("currentOffset") as Int? ?: 0
        filter = tabState?.get("filter") as String? ?: ""
        dataList = tabState?.get("dataList") as MutableList<T>? ?: mutableListOf()
        lastPopulated = tabState?.get("lastPopulated") as Long? ?: System.currentTimeMillis()
        active = tabState?.get("active") as Boolean? ?: false
        this.callback = callback
        loading = false
    }

    open fun activate(data: List<T>?) {
        active = true
    }

    open fun deactivate() {
        active = false
    }

    open fun destroy() { callback = { } }

    open fun onBottomReached() {}

    abstract fun search(query: String)
}

class TabStateDelegate<T, R> {
    operator fun getValue(thisRef: Tab<R>, property: KProperty<*>): T {
        val properties = State.tabState[thisRef.name]
        val value = properties!![property.name]
        return value as T
    }

    operator fun setValue(thisRef: Tab<R>, property: KProperty<*>, value: T) {
        val properties = State.tabState[thisRef.name]
        if (properties == null) { State.tabState[thisRef.name] = ConcurrentHashMap() }
        State.tabState[thisRef.name]!![property.name] = value as Any
    }
}

enum class ListType {
    Composition,
    Album,
    Artist
}