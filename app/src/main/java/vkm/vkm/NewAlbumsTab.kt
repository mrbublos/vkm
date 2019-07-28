package vkm.vkm

import vkm.vkm.utils.Album

class NewAlbumsTab(callback: SearchTabCallback) : Tab<Album>(callback, "new", ListType.Album) {

    override val hideSearch = true

    override fun activate(data: List<Album>?) {
        super.activate(data)
        if (dataList.isEmpty() || System.currentTimeMillis() - lastPopulated > 1000 * 60 * 60) {
            search("")
        }
    }

    override fun search(query: String): Boolean {
        if (loading || !active) { return false }

        lastPopulated = System.currentTimeMillis()
        page = 0

        nextPageLoader = { page -> MusicService.trackMusicService.getNewAlbums(page) }
        loadNewPage()
        return true
    }
}