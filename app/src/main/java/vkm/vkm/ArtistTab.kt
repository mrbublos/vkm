package vkm.vkm

import vkm.vkm.utils.Artist

class ArtistTab(callback: SearchTabCallback) : Tab<Artist>(callback, "artists", ListType.Artist) {

    override fun search(query: String): Boolean {
        if (loading || !active) { return false }
        if (filter == query) { return false }
        loading = true
        filter = query
        page = 0
        nextPageLoader = { page -> MusicService.trackMusicService.getArtists(filter, page) }
        loadNewPage()
        return false
    }
}