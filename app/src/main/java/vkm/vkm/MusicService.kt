package vkm.vkm

import com.beust.klaxon.JsonObject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

interface MusicService {
    companion object {
        fun getInstance(): MusicService {
            if (State.useVk) {
                return VkMusicService()
            }
            if (State.useSpotify) {
                return SpotifyMusicService()
            }
            return VkMusicService()
        }
    }

    fun getPlaylist(activity: SearchActivity, userOrGroup: User?, filter: String = "", offset: Int = 0)
    fun getGroups(activity: SearchActivity, filter: String = "", offset: Int = 0)
    fun getUsers(activity: SearchActivity, filter: String = "", offset: Int = 0)
    fun getCompositions(activity: SearchActivity, filter: String = "", offset: Int = 0)
}

open class VkMusicService : MusicService {

    override fun getPlaylist(activity: SearchActivity, userOrGroup: User?, filter: String, offset: Int) {
        if (userOrGroup == null) {
            return
        }
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

        if (!State.useMock) {
            callApi(true, "audio.get", params, VkParsers(activity).parsePlaylist)
        } else {
            getMock().getPlaylist(activity, userOrGroup, "", 0)
        }
    }

    override fun getGroups(activity: SearchActivity, filter: String, offset: Int) {
        val params = mutableListOf("q" to filter,
                "fields" to "has_photo",
                "count" to "20",
                "offset" to offset.toString())

        if (!State.useMock) {
            callApi("groups.search", params, VkParsers(activity).parseGroupList)
        } else {
            getMock().getGroups(activity, "", 0)
        }
    }

    override fun getUsers(activity: SearchActivity, filter: String, offset: Int) {
        val params = mutableListOf("q" to filter,
                "fields" to "photo_50, has_photo",
                "count" to "200",
                "offset" to offset.toString())

        if (!State.useMock) {
            callApi("users.search", params, VkParsers(activity).parseUserList)
        } else {
            getMock().getUsers(activity, "", 0)
        }
    }

    override fun getCompositions(activity: SearchActivity, filter: String, offset: Int) {
        val params = mutableListOf("q" to filter,
                "count" to "100",
                "v" to "5.68",
                "https" to "1",
                "lang" to "en",
                "search_own" to "0",
                "performer_only" to "0")

        if (!State.useMock) {
            callApi(true, "audio.search", params, VkParsers(activity).parseCompositionList)
        } else {
            getMock().getCompositions(activity, filter, 0)
        }
    }

    private fun getMock(): MusicServiceMock = MusicServiceMock()

    private fun callApi(method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        launch(CommonPool) {
            val result = VkApi.callVkMethod(params, method)
            launch(UI) { callback.invoke(result) }
        }
    }

    private fun callApi(addSignature: Boolean = false, method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        launch(CommonPool) {
            val result = VkApi.callVkMethod(params, method, addSignature)
            launch(UI) { callback.invoke(result) }
        }
    }
}

open class SpotifyMusicService : MusicService {

    override fun getPlaylist(activity: SearchActivity, userOrGroup: User?, filter: String, offset: Int) {
        if (userOrGroup == null) {
            return
        }
        val params = mutableListOf("shuffle" to "0")

        if (!State.useMock) {
            callApi("audio.get", params, SpotifyParsers(activity).parsePlaylist)
        } else {
            getMock().getPlaylist(activity, userOrGroup, "", 0)
        }
    }

    override fun getGroups(activity: SearchActivity, filter: String, offset: Int) {}

    override fun getUsers(activity: SearchActivity, filter: String, offset: Int) {
        val params = mutableListOf("q" to filter,
                "offset" to offset.toString())

        if (!State.useMock) {
            callApi("users.search", params, SpotifyParsers(activity).parseUserList)
        } else {
            getMock().getUsers(activity, "", 0)
        }
    }

    override fun getCompositions(activity: SearchActivity, filter: String, offset: Int) {
        val params = mutableListOf("q" to filter.replace(" ", "+"),
                "type" to "track,artist",
                "limit" to "50",
                "offset" to "$offset")

        if (!State.useMock) {
            callApi("/v1/search", params, SpotifyParsers(activity).parseCompositionList)
        } else {
            getMock().getCompositions(activity, filter, 0)
        }
    }

    private fun getMock(): MusicServiceMock {
        return MusicServiceMock()
    }


    private fun callApi(method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        SpotifyApi.callApi(method, params, callback)
    }
}


class MusicServiceMock : MusicService {
    override fun getPlaylist(activity: SearchActivity, userOrGroup: User?, filter: String, offset: Int) {
        "Running getUserPlaylistMock".log()
        val mock = activity.assets.open("getUserPlayList.json").readAll()
        VkParsers(activity).parsePlaylist.invoke(mock.toJson())
    }

    override fun getGroups(activity: SearchActivity, filter: String, offset: Int) {
        "Running getGroups".log()
        val mock = activity.assets.open("getGroups.json").readAll()
        VkParsers(activity).parseGroupList.invoke(mock.toJson())
    }

    override fun getUsers(activity: SearchActivity, filter: String, offset: Int) {
        "Running getUsers".log()
        val mock = activity.assets.open("getUsers.json").readAll()
        VkParsers(activity).parseUserList.invoke(mock.toJson())
    }

    override fun getCompositions(activity: SearchActivity, filter: String, offset: Int) {
        "Running getCompositions".log()
        val mock = activity.assets.open("getCompositionList.json").readAll()
        VkParsers(activity).parseCompositionList.invoke(mock.toJson())
    }
}
