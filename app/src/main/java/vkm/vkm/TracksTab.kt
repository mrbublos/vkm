package vkm.vkm

import vkm.vkm.utils.Composition

class TracksTab(callback: SearchTabCallback) : Tab<Composition>(callback, "tracks", ListType.Composition) {

    override fun activate(data: List<Composition>?) {
        super.activate(null)
        data?.let {
            dataList = it.toMutableList()
            page = NO_MORE_PAGES
        }
        onDataFetched(dataList)
    }

    override fun onBottomReached() {
        if (loading) { return }
        if (page == NO_MORE_PAGES) { return }

        loading = true
        MusicService.trackMusicService.getCompositions(filter, ++page, this::onDataFetched)
    }

    override fun search(query: String) {
        if (loading || !active) { return }
        if (filter == query) { return }

        loading = true
        filter = query
        page = 0
        MusicService.trackMusicService.getCompositions(filter, 0, this::onDataFetched)
    }
}