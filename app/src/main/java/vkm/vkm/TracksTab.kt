package vkm.vkm

import vkm.vkm.utils.Composition

class TracksTab(callback: SearchTabCallback) : Tab<Composition>(callback, "tracks", ListType.Composition) {

    override fun activate(data: List<Composition>?) {
        super.activate(null)
        data?.let {
            dataList = it.toMutableList()
            currentOffset = -1
        }
        onTracksFetched(dataList)
    }

    override fun onBottomReached() {
        if (loading) { return }
        if (currentOffset < 0) { return }

        loading = true
        MusicService.trackMusicService.getCompositions(filter, currentOffset, ::onTracksFetched)
    }

    override fun search(query: String) {
        if (loading || !active) { return }
        if (filter == query) { return }

        loading = true
        filter = query
        currentOffset = 0
        MusicService.trackMusicService.getCompositions(filter, 0, ::onTracksFetched)
    }

    private fun onTracksFetched(tracks: MutableList<Composition>) {
        loading = false
        if (currentOffset == 0) { dataList.clear() }
        currentOffset = if (currentOffset < 0 || tracks.isEmpty()) -1 else currentOffset + tracks.size
        dataList.addAll(tracks)
        callback()
    }
}