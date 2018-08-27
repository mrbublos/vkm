package vkm.vkm

import android.widget.ListAdapter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

typealias SearchTabCallback = (data: MutableList<out Any>, adaptorClass: KClass<out ListAdapter>) -> Unit

abstract class Tab<T> (callback: SearchTabCallback, var name: String) {

    var currentOffset: Int by TabStateDelegate<Int, T>()
    var filter: String by TabStateDelegate<String, T>()
    var callback: SearchTabCallback by TabStateDelegate<SearchTabCallback, T>()
    var dataList: MutableList<T> by TabStateDelegate<MutableList<T>, T>()
    var lastPopulated: Long by TabStateDelegate<Long, T>()

    open val hideSearch = false

    init {
        State.tabState.putIfAbsent(name, ConcurrentHashMap())
        val tabState = State.tabState[name]
        currentOffset = tabState?.get("currentOffset") as Int? ?: 0
        filter = tabState?.get("filter") as String? ?: ""
        dataList = tabState?.get("dataList") as MutableList<T>? ?: mutableListOf()
        lastPopulated = tabState?.get("lastPopulated") as Long? ?: System.currentTimeMillis()
        this.callback = tabState?.get("callback") as SearchTabCallback? ?: callback
    }

    open fun activate(data: List<T>?) {}

    open fun deactivate() {}

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