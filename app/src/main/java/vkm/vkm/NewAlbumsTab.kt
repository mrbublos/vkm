package vkm.vkm

import vkm.vkm.utils.Album

class NewAlbumsTab(callback: SearchTabCallback) : Tab<Album>(callback, "new", ListType.Album) {

    override val hideSearch = true

    override fun activate(data: List<Album>?) {
        super.activate(null)
        data?.let {  dataList = data.toMutableList() }
        if (dataList.isNotEmpty() && System.currentTimeMillis() - lastPopulated < 1000 * 60 * 60) {
            onDataFetched(dataList)
            return
        }
        search("")
    }

    override fun search(query: String) {
        if (loading || !active) { return }
        loading = true
        lastPopulated = System.currentTimeMillis()
        page = 0
        MusicService.trackMusicService.getNewAlbums(page, this::onDataFetched)
    }

    override fun onBottomReached() {
        if (loading) { return }
        if (page == NO_MORE_PAGES) { return }

        loading = true
        MusicService.trackMusicService.getNewAlbums(++page, this::onDataFetched)
    }

    override fun onDataFetched(data: MutableList<Album>) {
        loading = false
        dataList = data
        callback()
    }
}