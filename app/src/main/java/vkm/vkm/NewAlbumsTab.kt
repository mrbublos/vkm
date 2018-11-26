package vkm.vkm

import vkm.vkm.utils.Album

class NewAlbumsTab(callback: SearchTabCallback) : Tab<Album>(callback, "new", ListType.Album) {

    override val hideSearch = true

    override fun activate(data: List<Album>?) {
        super.activate(null)
        data?.let {  dataList = data.toMutableList() }
        if (dataList.isNotEmpty() && System.currentTimeMillis() - lastPopulated < 1000 * 60 * 60) {
            onAlbumsFetched(dataList)
            return
        }
        search("")
    }

    override fun search(query: String) {
        if (loading || !active) { return }
        loading = true
        lastPopulated = System.currentTimeMillis()
        MusicService.trackMusicService.getNewAlbums(::onAlbumsFetched)
    }

    private fun onAlbumsFetched(albums: MutableList<Album>) {
        loading = false
        dataList = albums
        callback()
    }
}