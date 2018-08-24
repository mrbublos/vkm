package vkm.vkm

import vkm.vkm.adapters.CompositionListAdapter
import vkm.vkm.utils.Composition

class TracksTab(callback: SearchTabCallback) : Tab<Composition>(callback, "tracks") {

    override fun activate(data: List<Composition>?) {
        data?.let {
            dataList = it.toMutableList()
            currentOffset = -1
        }
        onTracksFetched(dataList)
    }

    override fun onBottomReached() {
        if (currentOffset < 0) { return }
        MusicService.trackMusicService.getCompositions(filter, currentOffset, ::onTracksFetched)
    }

    override fun search(query: String) {
        filter = query
        currentOffset = 0
        MusicService.trackMusicService.getCompositions(filter, 0, ::onTracksFetched)
    }

    private fun onTracksFetched(tracks: MutableList<Composition>) {
        if (currentOffset == 0) { dataList.clear() }
        currentOffset = if (currentOffset < 0 || tracks.isEmpty()) -1 else currentOffset + tracks.size
        dataList.addAll(tracks)
        callback(dataList, CompositionListAdapter::class)
    }
}