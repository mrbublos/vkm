package vkm.vkm

import vkm.vkm.utils.Composition
import vkm.vkm.utils.Proxy
import vkm.vkm.utils.User

object State {
    var developerMode = false
    var enableDownloadAll = true
    var enableTextSuggestions = true
    var trackProvider = "ym"
    var userProvider = "vk"
    var groupProvider = "vk"
    val compositionElementList = mutableListOf<Composition>()
    var userElementList = mutableListOf<User>()
    var groupElementList = mutableListOf<User>()
    var totalCompositions = 0
    var currentOffset = 0
    var currentSearchTab = "tracks"
    var currentHistoryTab = "inProgress"
    var selectedElement: User? = null
    var proxy = "";
}