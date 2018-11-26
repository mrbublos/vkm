package vkm.vkm

import vkm.vkm.utils.Artist

class ArtistTab(callback: SearchTabCallback) : Tab<Artist>(callback, "artists", ListType.Artist) {

    override fun activate(data: List<Artist>?) {
        super.activate(null)
        data?.let {
            dataList = it.toMutableList()
            currentOffset = -1
        }
        onDataFetched(dataList)
    }

    override fun onBottomReached() {
        if (loading || !active) { return }
        loading = true
        if (currentOffset < 0) { return }
        MusicService.trackMusicService.getArtists(filter, currentOffset, ::onDataFetched)
    }

    override fun search(query: String) {
        if (loading || !active) { return }
        if (filter == query) { return }
        loading = true
        filter = query
        currentOffset = 0
        MusicService.trackMusicService.getArtists(filter, 0, ::onDataFetched)
    }

    private fun onDataFetched(artists: MutableList<Artist>) {
        loading = false
        if (currentOffset == 0) { dataList.clear() }
        currentOffset = if (currentOffset < 0 || artists.isEmpty()) -1 else currentOffset + artists.size
        dataList.addAll(artists)
        callback()
    }
}