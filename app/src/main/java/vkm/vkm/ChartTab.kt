package vkm.vkm

import vkm.vkm.adapters.CompositionListAdapter
import vkm.vkm.utils.Composition

class ChartTab(callback: SearchTabCallback) : Tab<Composition>(callback, "chart") {

    override val hideSearch = true

    override fun activate(data: List<Composition>?) {
        data?.let { dataList = data.toMutableList() }
        if (dataList.isNotEmpty() && System.currentTimeMillis() - lastPopulated < 1000 * 60 * 60) {
            onChartFetched(dataList)
            return
        }
        search("")
    }

    override fun search(query: String) {
        lastPopulated = System.currentTimeMillis()
        MusicService.trackMusicService.getChart(::onChartFetched)
    }

    private fun onChartFetched(compositions: MutableList<Composition>) {
        dataList = compositions
        callback(dataList, CompositionListAdapter::class)
    }
}