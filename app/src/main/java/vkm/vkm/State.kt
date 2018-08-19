package vkm.vkm

import java.util.concurrent.ConcurrentHashMap

object State {
    var developerMode = false
    var enableDownloadAll = true
    var enableTextSuggestions = true
    var currentSearchTab = 0
    var currentHistoryTab = "inProgress"
    var tabState = ConcurrentHashMap<String, ConcurrentHashMap<String, in Any>>()
}