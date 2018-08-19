package vkm.vkm

import vkm.vkm.utils.Composition
import vkm.vkm.utils.CompositionListAdapter

class TracksTab(callback: SearchTabCallback) : Tab<Composition>(callback, "tracks") {

    override fun activate() {
        if (dataList.isNotEmpty()) { onTracksFetched(dataList) }
    }

    override fun deactivate() {}

    override fun onBottomReached() {
        if (currentOffset < 0) { return }
        if (MusicService.trackMusicService.getCompositions(filter, currentOffset, ::onTracksFetched)) {
            currentOffset = 0
            dataList.clear()
        }
    }

    override fun search(query: String) {
        filter = query
        if (MusicService.trackMusicService.getCompositions(filter, 0, ::onTracksFetched)) {
            currentOffset = 0
            dataList.clear()
        }
    }

    private fun onTracksFetched(tracks: MutableList<Composition>) {
        currentOffset = if (tracks.isEmpty()) -1 else dataList.size + tracks.size
        callback(tracks, CompositionListAdapter::class)
    }
}