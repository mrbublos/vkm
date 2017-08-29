package vkm.vkm

import android.util.Log
import com.beust.klaxon.JsonObject

open class MusicService {

    init {
        Log.i(MainActivity.TAG, "MusicService Started")
    }

    open fun getPlaylist(activity: SearchActivity, userOrGroup: User?, filter: String = "") {
        if (userOrGroup == null) { return }
        val prefix = if (userOrGroup.isGroup) "-" else ""
        val params = mutableListOf(
                "v" to "5.68",
                "lang" to "en",
                "https" to "1",
                "owner_id" to prefix + userOrGroup.userId,
                "count" to "200",
                "extended" to "1",
                "shuffle" to "0")
        callApi(true,"audio.get", params, VkParsers(activity).parsePlaylist)
//        getMock().getPlaylist(activity, userOrGroup)
    }

    open fun getDownloaded(filter: String = ""): List<Composition> {
        return DownloadManager.getDownloaded().filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    open fun getInProgress(filter: String = ""): List<Composition> {
        return DownloadManager.getInProgress().filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    open fun getInQueue(filter: String = ""): List<Composition> {
        return DownloadManager.getQueue().filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    open fun getGroups(activity: SearchActivity, filter: String = "") {
        // TODO paging, error handling
        val params = mutableListOf("q" to filter, "fields" to "has_photo", "count" to "20")
        callApi("groups.search", params, VkParsers(activity).parseGroupList)
//        getMock().getGroups(activity)
    }

    open fun getUsers(activity: SearchActivity, filter: String = "") {
        // TODO add paging, error handling
        val params = mutableListOf("q" to filter, "fields" to "photo_50, has_photo", "count" to "20")
        callApi("users.search", params, VkParsers(activity).parseUserList)
//        getMock().getUsers(activity)
    }

    open fun getCompositions(activity: SearchActivity, filter: String = "") {
        val params = mutableListOf("q" to filter,
                "count" to "100",
                "v" to "5.68",
                "https" to "1",
                "lang" to "en",
                "search_own" to "0",
                "performer_only" to "0")
        callApi(true, "audio.search", params, VkParsers(activity).parseCompositionList)
//        getMock().getCompositions(activity, filter)
    }

    fun getMock(): MusicServiceMock {
        return MusicServiceMock()
    }


    private fun callApi(method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        VkApiCallTask(callback).execute(method to params)
    }

    private fun callApi(addSignature: Boolean = false, method: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        VkApiCallTask(callback, addSignature).execute(method to params)
    }
}

class MusicServiceMock : MusicService() {
    override fun getPlaylist(activity: SearchActivity, userOrGroup: User?, filter: String) {
        Log.v("vkMOCK", "Running getUserPlaylistMock")
        val mock = activity.assets.open("getUserPlayList.json").readAll()
        VkParsers(activity).parsePlaylist.invoke(mock.toJson())
    }

    override fun getGroups(activity: SearchActivity, filter: String) {
        Log.v("vkMOCK", "Running getGroups")
        val mock = activity.assets.open("getGroups.json").readAll()
        VkParsers(activity).parseGroupList.invoke(mock.toJson())
    }

    override fun getUsers(activity: SearchActivity, filter: String) {
        Log.v("vkMOCK", "Running getUsers")
        val mock = activity.assets.open("getUsers.json").readAll()
        VkParsers(activity).parseUserList.invoke(mock.toJson())
    }

    override fun getCompositions(activity: SearchActivity, filter: String) {
        Log.v("vkMOCK", "Running getCompositions")
        val mock = activity.assets.open("getCompositionList.json").readAll()
        VkParsers(activity).parseCompositionList.invoke(mock.toJson())
    }
}
