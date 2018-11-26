package vkm.vkm

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.*
import java.util.concurrent.atomic.AtomicBoolean

interface MusicService {
    companion object {
        val isLoading = AtomicBoolean(false)
        val trackMusicService: YMusicService = YMusicService()
    }

    fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String = "", page: Int = 0): Boolean
    fun getGroups(fragment: SearchFragment, filter: String = "", page: Int = 0): Boolean
    fun getUsers(fragment: SearchFragment, filter: String = "", page: Int = 0): Boolean
    fun getCompositions(filter: String = "", page: Int = 0, callback: (tracks: MutableList<Composition>) -> Unit): Boolean
    fun getNewAlbums(page: Int, callback: (tracks: MutableList<Album>) -> Unit): Boolean
    fun getChart(page: Int, callback: (tracks: MutableList<Composition>) -> Unit): Boolean
    fun getArtists(filter: String = "", page: Int = 0, callback: (artists: MutableList<Artist>) -> Unit): Boolean
    suspend fun preprocess(composition: Composition) {}
}

open class YMusicService : MusicService {

    override fun getChart(page: Int, callback: (tracks: MutableList<Composition>) -> Unit): Boolean {
        if (isLoading()) { return false }
        launch(CommonPool) {
            val compositions = YMusicParsers.parseChart(YMusicApi.getChart(page))
            loadingFinished()
            callback(compositions)
        }
        return true
    }

    override fun getNewAlbums(page: Int, callback: (data: MutableList<Album>) -> Unit): Boolean {
        if (isLoading()) { return false }
        launch(CommonPool) {
            val albumIds = YMusicParsers.parseNewReleases(YMusicApi.getNewReleases(page))
            val albums = YMusicParsers.parseAlbums(YMusicApi.getAlbums(albumIds))
            albums.forEach {
                it.compositionFetcher = {
                    launch(CommonPool) {
                        it.compositions = YMusicParsers.parseAlbum(YMusicApi.getAlbum(it.id))
                    }
                }
            }
            loadingFinished()
            callback(albums)
        }
        return true
    }

    // TODO implement
    override fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String, page: Int): Boolean { return false }

    // TODO implement
    override fun getGroups(fragment: SearchFragment, filter: String, page: Int): Boolean { return false }

    // TODO implement
    override fun getUsers(fragment: SearchFragment, filter: String, page: Int): Boolean { return false }

    override suspend fun preprocess(composition: Composition) = YMusicApi.preprocessUrl(composition)

    override fun getCompositions(filter: String, page: Int, callback: (tracks: MutableList<Composition>) -> Unit): Boolean {
        if (isLoading()) { return false }
        launch(CommonPool) {
            if (filter.isEmpty()) {
                loadingFinished()
                callback(mutableListOf())
                return@launch
            }

            val result = YMusicApi.search(filter, page, "track")
            val tracks = YMusicParsers.parseCompositionList(result)
            loadingFinished()
            callback(tracks)
        }
        return true
    }

    override fun getArtists(filter: String, page: Int, callback: (artists: MutableList<Artist>) -> Unit): Boolean {
        if (isLoading()) { return false }
        launch(CommonPool) {
            if (filter.isEmpty()) {
                loadingFinished()
                callback(mutableListOf())
                return@launch
            }

            val result = YMusicApi.search(filter, page, "artist")
            val artists = YMusicParsers.parseArtists(result)
            artists.forEach { artist ->
                artist.compositionFetcher = {
                    launch(CommonPool) {
                        artist.compositions = YMusicParsers.parseArtistTracks(YMusicApi.getArtistTracks(artist.id))
                    }
                }
            }
            loadingFinished()
            callback(artists)
        }
        return true
    }

    private fun loadingFinished() {
        MusicService.isLoading.set(false)
    }

    private fun isLoading() = !MusicService.isLoading.compareAndSet(false, true)

}

