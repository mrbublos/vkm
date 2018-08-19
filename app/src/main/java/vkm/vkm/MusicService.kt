package vkm.vkm

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.Composition
import vkm.vkm.utils.User
import vkm.vkm.utils.YMusicApi
import vkm.vkm.utils.YMusicParsers
import java.util.concurrent.atomic.AtomicBoolean

interface MusicService {
    companion object {
        val isLoading = AtomicBoolean(false)
        val trackMusicService: MusicService = YMusicService()
    }

    fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String = "", offset: Int = 0): Boolean
    fun getGroups(fragment: SearchFragment, filter: String = "", offset: Int = 0): Boolean
    fun getUsers(fragment: SearchFragment, filter: String = "", offset: Int = 0): Boolean
    fun getCompositions(filter: String = "", offset: Int = 0, callback: (tracks: MutableList<Composition>) -> Unit): Boolean
    suspend fun preprocess(composition: Composition) {}
}

open class YMusicService : MusicService {

    // TODO implement
    override fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String, offset: Int): Boolean { return false }

    // TODO implement
    override fun getGroups(fragment: SearchFragment, filter: String, offset: Int): Boolean { return false }

    // TODO implement
    override fun getUsers(fragment: SearchFragment, filter: String, offset: Int): Boolean { return false }

    override suspend fun preprocess(composition: Composition) = YMusicApi.preprocessUrl(composition)

    override fun getCompositions(filter: String, offset: Int, callback: (tracks: MutableList<Composition>) -> Unit): Boolean {
        if (!MusicService.isLoading.compareAndSet(false, true)) { return false }
        if (!State.developerMode) {
            launch(CommonPool) {
                val result = YMusicApi.search(filter, offset)
                MusicService.isLoading.set(false)
                val tracks = YMusicParsers.parseCompositionList(result)
                launch(UI) { callback(tracks) }
            }
        }
        return true
    }
}

