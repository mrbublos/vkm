package vkm.vkm

import com.beust.klaxon.JsonObject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.*
import java.util.concurrent.atomic.AtomicBoolean

interface MusicService {
    companion object {
        val isLoading = AtomicBoolean(false)
        val trackMusicService: MusicService = getInstance(State.trackProvider)
        val groupMusicService: MusicService = getInstance(State.groupProvider)
        val userMusicService: MusicService = getInstance(State.userProvider)

        private fun getInstance(provider: String): MusicService {
            return when (provider) {
                "vk" -> VkMusicService()
                "ym" -> YMusicService()
                "sp" -> SpotifyMusicService()
                else -> VkMusicService()
            }
        }
    }

    fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String = "", offset: Int = 0): Boolean
    fun getGroups(fragment: SearchFragment, filter: String = "", offset: Int = 0): Boolean
    fun getUsers(fragment: SearchFragment, filter: String = "", offset: Int = 0): Boolean
    fun getCompositions(fragment: SearchFragment, filter: String = "", offset: Int = 0): Boolean
    suspend fun preprocess(composition: Composition) {}
}

open class VkMusicService : MusicService {

    override fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String, offset: Int): Boolean {
        if (userOrGroup == null || !MusicService.isLoading.compareAndSet(false, true)) { return false }
        val prefix = if (userOrGroup.isGroup) "-" else ""
        val params = mutableListOf(
                "v" to "5.68",
                "lang" to "en",
                "https" to "1",
                "owner_id" to prefix + userOrGroup.userId,
                "count" to "200",
                "extended" to "1",
                "offset" to offset.toString(),
                "shuffle" to "0")

        if (!State.developerMode) {
            callApi(true, "audio.get", params, VkParsers(fragment).parsePlaylist)
        } else {
            getMock().getPlaylist(fragment, userOrGroup, "", 0)
        }
        return true
    }

    override fun getGroups(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        if (!MusicService.isLoading.compareAndSet(false, true)) { return false }
        val params = mutableListOf("q" to filter,
                "fields" to "has_photo",
                "count" to "20",
                "offset" to offset.toString())

        if (!State.developerMode) {
            callApi("groups.search", params, VkParsers(fragment).parseGroupList)
        } else {
            getMock().getGroups(fragment, "", 0)
        }
        return true
    }

    override fun getUsers(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        if (!MusicService.isLoading.compareAndSet(false, true)) { return false }
        val params = mutableListOf("q" to filter,
                "fields" to "photo_50, has_photo",
                "count" to "200",
                "offset" to offset.toString())

        if (!State.developerMode) {
            callApi("users.search", params, VkParsers(fragment).parseUserList)
        } else {
            getMock().getUsers(fragment, "", 0)
        }
        return true
    }

    override fun getCompositions(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        if (!MusicService.isLoading.compareAndSet(false, true)) { return false }
        val params = mutableListOf("q" to filter,
                "count" to "100",
                "v" to "5.68",
                "https" to "1",
                "lang" to "en",
                "search_own" to "0",
                "offset" to offset.toString(),
                "performer_only" to "0")

        if (!State.developerMode) {
            callApi(true, "audio.search", params, VkParsers(fragment).parseCompositionList)
        } else {
            getMock().getCompositions(fragment, filter, 0)
        }
        return true
    }

    private fun getMock(): MusicServiceMock {
        MusicService.isLoading.set(false)
        return MusicServiceMock()
    }

    private fun callApi(method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        launch(CommonPool) {
            val result = VkApi.callVkMethod(true, params, method)
            MusicService.isLoading.set(false)
            if (result == null) { "Error connecting to server".logE() }
            launch(UI) { callback.invoke(result) }
        }
    }

    private fun callApi(addSignature: Boolean = false, method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        launch(CommonPool) {
            val result = VkApi.callVkMethod(true, params, method, addSignature)
            MusicService.isLoading.set(false)
            if (result == null) { "Error connecting to server".logE() }
            launch(UI) { callback.invoke(result) }
        }
    }
}

open class YMusicService : MusicService {

    // TODO implement
    override fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String, offset: Int): Boolean { return false }

    // TODO implement
    override fun getGroups(fragment: SearchFragment, filter: String, offset: Int): Boolean { return false }

    // TODO implement
    override fun getUsers(fragment: SearchFragment, filter: String, offset: Int): Boolean { return false }

    override suspend fun preprocess(composition: Composition) = YMusicApi.preprocessUrl(composition)

    override fun getCompositions(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        if (!MusicService.isLoading.compareAndSet(false, true)) { return false }
        if (!State.developerMode) {
            launch(CommonPool) {
                val result = YMusicApi.search(filter, offset)
                MusicService.isLoading.set(false)
                launch(UI) { YMusicParsers(fragment).parseCompositionList(result) }
            }
        } else {
            getMock().getCompositions(fragment, filter, 0)
        }
        return true
    }

    private fun getMock(): MusicServiceMock {
        MusicService.isLoading.set(false)
        return MusicServiceMock()
    }
}

open class SpotifyMusicService : MusicService {

    override fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String, offset: Int): Boolean {
        if (userOrGroup == null)  {return false }
        val params = mutableListOf("shuffle" to "0")

        if (!State.developerMode) {
            callApi("audio.get", params, SpotifyParsers(fragment).parsePlaylist)
        } else {
            getMock().getPlaylist(fragment, userOrGroup, "", 0)
        }
        return true
    }

    override fun getGroups(fragment: SearchFragment, filter: String, offset: Int): Boolean = false

    override fun getUsers(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        val params = mutableListOf("q" to filter,
                "offset" to offset.toString())

        if (!State.developerMode) {
            callApi("users.search", params, SpotifyParsers(fragment).parseUserList)
        } else {
            getMock().getUsers(fragment, "", 0)
        }
        return true
    }

    override fun getCompositions(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        val params = mutableListOf("q" to filter.replace(" ", "+"),
                "type" to "track,artist",
                "limit" to "50",
                "offset" to "$offset")

        if (!State.developerMode) {
            callApi("/v1/search", params, SpotifyParsers(fragment).parseCompositionList)
        } else {
            getMock().getCompositions(fragment, filter, 0)
        }
        return true
    }

    private fun getMock(): MusicServiceMock {
        return MusicServiceMock()
    }


    private fun callApi(method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        SpotifyApi.callApi(method, params, callback)
    }
}


class MusicServiceMock : MusicService {
    override fun getPlaylist(fragment: SearchFragment, userOrGroup: User?, filter: String, offset: Int): Boolean {
        "Running getUserPlaylistMock".log()
        val mock = fragment.context.assets.open("getUserPlayList.json").readAll()
        VkParsers(fragment).parsePlaylist.invoke(mock.toJson())
        return true
    }

    override fun getGroups(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        "Running getGroups".log()
        val mock = fragment.context.assets.open("getGroups.json").readAll()
        VkParsers(fragment).parseGroupList.invoke(mock.toJson())
        return true
    }

    override fun getUsers(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        "Running getUsers".log()
        val mock = fragment.context.assets.open("getUsers.json").readAll()
        VkParsers(fragment).parseUserList.invoke(mock.toJson())
        return true
    }

    override fun getCompositions(fragment: SearchFragment, filter: String, offset: Int): Boolean {
        "Running getCompositions".log()
        val mock = fragment.context.assets.open("getCompositionList.json").readAll()
        VkParsers(fragment).parseCompositionList.invoke(mock.toJson())
        return true
    }
}
