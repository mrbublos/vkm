package vkm.vkm

import vkm.vkm.utils.*
import java.util.concurrent.atomic.AtomicBoolean

interface MusicService {
    companion object {
        val isLoading = AtomicBoolean(false)
        val trackMusicService: YMusicService = YMusicService()
    }

    suspend fun getCompositions(filter: String = "", page: Int = 0): List<Composition>
    suspend fun getNewAlbums(page: Int): List<Album>
    suspend fun getChart(page: Int): List<Composition>
    suspend fun getArtists(filter: String = "", page: Int = 0): List<Artist>
    suspend fun preprocess(composition: Composition) {}
}

open class YMusicService : MusicService {

    override suspend fun getChart(page: Int): List<Composition> {
        if (isLoading()) { return listOf() }
        val compositions = YMusicParsers.parseChart(YMusicApi.getChart(page))
        loadingFinished()
        return compositions
    }

    override suspend fun getNewAlbums(page: Int): List<Album> {
        if (isLoading()) { return listOf() }
        val albumIds = YMusicParsers.parseNewReleases(YMusicApi.getNewReleases(page))
        val albums = YMusicParsers.parseAlbums(YMusicApi.getAlbums(albumIds))
        albums.forEach { album ->
            album.compositionFetcher = { page ->
                if (page > 0)
                    listOf()
                else
                YMusicParsers.parseAlbum(YMusicApi.getAlbum(album.id))
            }
        }
        loadingFinished()
        return albums
    }

    override suspend fun preprocess(composition: Composition) = YMusicApi.preprocessUrl(composition)

    override suspend fun getCompositions(filter: String, page: Int): List<Composition> {
        if (isLoading() || filter.isEmpty()) {
            loadingFinished()
            return mutableListOf()
        }

        val result = YMusicApi.search(filter, page, "track")
        val tracks = YMusicParsers.parseCompositionList(result)
        loadingFinished()
        return tracks
    }

    override suspend fun getArtists(filter: String, page: Int): List<Artist> {
        if (isLoading() || filter.isEmpty()) {
            loadingFinished()
            return listOf()
        }

        val result = YMusicApi.search(filter, page, "artist")
        val artists = YMusicParsers.parseArtists(result)
        artists.forEach { artist ->
            artist.compositionFetcher = { page ->
                if (page > 0)
                    listOf()
                else
                YMusicParsers.parseArtistTracks(YMusicApi.getArtistTracks(page, artist.id))
            }
        }
        loadingFinished()
        return artists
    }

    private fun loadingFinished() {
        MusicService.isLoading.set(false)
    }

    private fun isLoading() = !MusicService.isLoading.compareAndSet(false, true)

}

