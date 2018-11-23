package vkm.vkm

import vkm.vkm.adapters.ArtistListAdapter
import vkm.vkm.adapters.CompositionListAdapter
import vkm.vkm.utils.Artist
import vkm.vkm.utils.Composition

class ArtistTab(callback: SearchTabCallback) : Tab<Artist>(callback, "artists") {

    override fun activate(data: List<Artist>?) {
        data?.let {
            dataList = it.toMutableList()
            currentOffset = -1
        }
        onTracksFetched(dataList)
    }

    override fun onBottomReached() {
        if (currentOffset < 0) { return }
        MusicService.trackMusicService.getArtists(filter, currentOffset, ::onTracksFetched)
    }

    override fun search(query: String) {
        filter = query
        currentOffset = 0
        MusicService.trackMusicService.getArtists(filter, 0, ::onTracksFetched)
    }

    private fun onTracksFetched(artists: MutableList<Artist>) {
        if (currentOffset == 0) { dataList.clear() }
        currentOffset = if (currentOffset < 0 || artists.isEmpty()) -1 else currentOffset + artists.size
        dataList.addAll(artists)
        callback(dataList, ArtistListAdapter::class)
    }
}