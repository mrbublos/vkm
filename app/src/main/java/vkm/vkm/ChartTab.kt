package vkm.vkm

import vkm.vkm.utils.Composition

class ChartTab(callback: SearchTabCallback) : Tab<Composition>(callback, "chart", ListType.Composition) {

    override val hideSearch = true

    override fun activate(data: List<Composition>?) {
        super.activate(data)
        if (dataList.isEmpty() || System.currentTimeMillis() - lastPopulated < 1000 * 60 * 60) {
            search("")
        }
    }

    override fun search(query: String): Boolean {
        if (loading || !active) { return false }
        loading = true
        lastPopulated = System.currentTimeMillis()
        page = 0

        nextPageLoader = { page -> MusicService.trackMusicService.getChart(page) }
        loadNewPage()
        return true
    }
}