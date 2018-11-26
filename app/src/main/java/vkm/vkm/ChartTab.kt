package vkm.vkm

import vkm.vkm.utils.Composition

class ChartTab(callback: SearchTabCallback) : Tab<Composition>(callback, "chart", ListType.Composition) {

    override val hideSearch = true

    override fun activate(data: List<Composition>?) {
        super.activate(null)
        data?.let { dataList = data.toMutableList() }
        if (dataList.isNotEmpty() && System.currentTimeMillis() - lastPopulated < 1000 * 60 * 60) {
            onChartFetched(dataList)
            return
        }
        search("")
    }

    override fun search(query: String) {
        if (loading || !active) { return }
        loading = true
        lastPopulated = System.currentTimeMillis()
        MusicService.trackMusicService.getChart(::onChartFetched)
    }

    private fun onChartFetched(compositions: MutableList<Composition>) {
        loading = false
        dataList = compositions
        callback()
    }
}