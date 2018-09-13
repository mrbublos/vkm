package vkm.vkm

import java.util.concurrent.ConcurrentHashMap

object State {
    var enableTextSuggestions = true
    var currentSearchTab = 0
    var currentHistoryTab = "inProgress"
    var useProxy = true
    var tabState = ConcurrentHashMap<String, ConcurrentHashMap<String, in Any>>()
}