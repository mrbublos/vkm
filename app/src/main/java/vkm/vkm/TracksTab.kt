package vkm.vkm

import vkm.vkm.utils.Composition

class TracksTab(callback: SearchTabCallback) : Tab<Composition>(callback, "tracks", ListType.Composition) {

    override fun search(query: String): Boolean {
        if (loading || !active) { return false }
        if (filter == query) { return false }

        filter = query
        page = 0

        nextPageLoader = { page -> MusicService.trackMusicService.getCompositions(filter, page) }
        loadNewPage()
        return true
    }
}