package vkm.vkm

import vkm.vkm.adapters.AlbumListAdapter
import vkm.vkm.utils.Album

class NewAlbumsTab(callback: SearchTabCallback) : Tab<Album>(callback, "new") {

    override val hideSearch = true

    override fun activate(data: List<Album>?) {
        data?.let {  dataList = data.toMutableList() }
        if (dataList.isNotEmpty() && System.currentTimeMillis() - lastPopulated < 1000 * 60 * 60) {
            onAlbumsFetched(dataList)
            return
        }
        search("")
    }

    override fun search(query: String) {
        lastPopulated = System.currentTimeMillis()
        MusicService.trackMusicService.getNewAlbums(::onAlbumsFetched)
    }

    private fun onAlbumsFetched(albums: MutableList<Album>) {
        dataList = albums
        callback(dataList, AlbumListAdapter::class)
    }
}