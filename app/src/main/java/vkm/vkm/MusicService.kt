package vkm.vkm

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.*
import java.util.concurrent.atomic.AtomicBoolean

interface MusicService {
    companion object {
        val isLoading = AtomicBoolean(false)
        val trackMusicService: YMusicService = YMusicService()
    }

    fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String = "", offset: Int = 0): Boolean
    fun getGroups(fragment: SearchFragment, filter: String = "", offset: Int = 0): Boolean
    fun getUsers(fragment: SearchFragment, filter: String = "", offset: Int = 0): Boolean
    fun getCompositions(filter: String = "", offset: Int = 0, callback: (tracks: MutableList<Composition>) -> Unit): Boolean
    fun getNewAlbums(callback: (tracks: MutableList<Album>) -> Unit): Boolean
    fun getChart(callback: (tracks: MutableList<Composition>) -> Unit): Boolean
    suspend fun preprocess(composition: Composition) {}
}

open class YMusicService : MusicService {

    override fun getChart(callback: (tracks: MutableList<Composition>) -> Unit): Boolean {
        if (isLoading()) { return false }
        launch(CommonPool) {
            val compositions = YMusicParsers.parseChart(YMusicApi.getChart())
            loadingFinished()
            launch(UI) { callback(compositions) }
        }
        return true
    }

    override fun getNewAlbums(callback: (tracks: MutableList<Album>) -> Unit): Boolean {
        if (isLoading()) { return false }
        launch(CommonPool) {
            val albumIds = YMusicParsers.parseNewReleases(YMusicApi.getNewReleases())
            val albums = YMusicParsers.parseAlbums(YMusicApi.getAlbums(albumIds))
            albums.forEach {
                it.compositionFetcher = {
                    launch(CommonPool) {
                        it.compositions = YMusicParsers.parseAlbum(YMusicApi.getAlbum(it.id))
                    }
                }
            }
            loadingFinished()
            launch(UI) { callback(albums) }
        }
        return true
    }

    // TODO implement
    override fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String, offset: Int): Boolean { return false }

    // TODO implement
    override fun getGroups(fragment: SearchFragment, filter: String, offset: Int): Boolean { return false }

    // TODO implement
    override fun getUsers(fragment: SearchFragment, filter: String, offset: Int): Boolean { return false }

    override suspend fun preprocess(composition: Composition) = YMusicApi.preprocessUrl(composition)

    override fun getCompositions(filter: String, offset: Int, callback: (tracks: MutableList<Composition>) -> Unit): Boolean {
        if (isLoading()) { return false }
        launch(CommonPool) {
            if (filter.isEmpty()) {
                loadingFinished()
                launch(UI) { callback(mutableListOf()) }
                return@launch
            }

            val result = YMusicApi.search(filter, offset)
            val tracks = YMusicParsers.parseCompositionList(result)
            loadingFinished()
            launch(UI) { callback(tracks) }
        }
        return true
    }

    private fun loadingFinished() {
        MusicService.isLoading.set(false)
    }

    private fun isLoading() = !MusicService.isLoading.compareAndSet(false, true)

}

