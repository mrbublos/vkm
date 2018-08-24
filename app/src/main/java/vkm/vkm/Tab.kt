package vkm.vkm

import android.widget.ListAdapter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

typealias SearchTabCallback = (data: MutableList<out Any>, adaptorClass: KClass<out ListAdapter>) -> Unit

abstract class Tab<T> {

    var name: String
    var currentOffset: Int by TabStateDelegate<Int, T>()
    var filter: String by TabStateDelegate<String, T>()
    var callback: SearchTabCallback by TabStateDelegate<SearchTabCallback, T>()
    var dataList: MutableList<T> by TabStateDelegate<MutableList<T>, T>()
    var lastPopulated: Long by TabStateDelegate<Long, T>()

    open val hideSearch = false

    constructor(callback: SearchTabCallback, name: String) {
        // TODO rework this crap, we do not need delegates, if we recreate vars every time
        this.name = name
        currentOffset = 0
        filter = ""
        dataList = mutableListOf()
        lastPopulated = System.currentTimeMillis()
        this.callback = callback
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