package vkm.vkm

import vkm.vkm.adapters.ArtistListAdapter
import vkm.vkm.utils.Artist

class ArtistTab(callback: SearchTabCallback) : Tab<Artist>(callback, "artists") {

    override fun activate(data: List<Artist>?) {
        data?.let {
            dataList = it.toMutableList()
            currentOffset = -1
        }
        onDataFetched(dataList)
    }

    override fun onBottomReached() {
        if (currentOffset < 0) { return }
        MusicService.trackMusicService.getArtists(filter, currentOffset, ::onDataFetched)
    }

    override fun search(query: String) {
        if (filter == query) { return }
        filter = query
        currentOffset = 0
        MusicService.trackMusicService.getArtists(filter, 0, ::onDataFetched)
    }

    private fun onDataFetched(artists: MutableList<Artist>) {
        if (!active) { return }
        if (currentOffset == 0) { dataList.clear() }
        currentOffset = if (currentOffset < 0 || artists.isEmpty()) -1 else currentOffset + artists.size
        dataList.addAll(artists)
        callback(dataList, ArtistListAdapter::class)
    }
}