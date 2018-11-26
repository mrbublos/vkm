package vkm.vkm

import vkm.vkm.utils.Artist

class ArtistTab(callback: SearchTabCallback) : Tab<Artist>(callback, "artists", ListType.Artist) {

    override fun activate(data: List<Artist>?) {
        super.activate(null)
        data?.let {
            dataList = it.toMutableList()
            page = NO_MORE_PAGES
        }
        onDataFetched(dataList)
    }

    override fun onBottomReached() {
        if (loading || !active) { return }
        if (page == NO_MORE_PAGES) { return }
        loading = true
        MusicService.trackMusicService.getArtists(filter, ++page, ::onDataFetched)
    }

    override fun search(query: String) {
        if (loading || !active) { return }
        if (filter == query) { return }
        loading = true
        filter = query
        page = 0
        MusicService.trackMusicService.getArtists(filter, 0, ::onDataFetched)
    }
}