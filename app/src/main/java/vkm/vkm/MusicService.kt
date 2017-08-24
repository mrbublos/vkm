package vkm.vkm

import android.util.Log
import com.beust.klaxon.JsonObject

/**
 * todo decompile off app and get client_id and key
 */
open class MusicService {

    init {
        Log.i(MainActivity.TAG, "MusicService Started")
    }

    open fun getUserPlaylist(activity: SearchActivity, userId: String, filter: String = "") {
        val params = mutableListOf(
                "v" to "5.68",
                "lang" to "en",
                "https" to "1",
                "owner_id" to userId,
                "count" to "200",
                "extended" to "1",
                "shuffle" to "0")
//        callApi(true,"audio.get", params, VkParsers(activity).parseUserPlaylist)
        getMock().getUserPlaylist(activity, userId)
    }

    open fun getGroupPlaylist(activity: SearchActivity, groupId: String?, filter: String = "") {
        getMock().getGroupPlaylist(activity, groupId)
    }

    open fun getDownloaded(filter: String = ""): List<Composition> {
        return listOf(Composition()).filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    open fun getInProgress(filter: String = ""): List<Composition> {
        return listOf(Composition()).filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    open fun getInQueue(filter: String = ""): List<Composition> {
        return listOf(Composition()).filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    open fun getGroups(activity: SearchActivity, filter: String = "") {
        // TODO paging, error handling
        val params = mutableListOf("q" to filter, "fields" to "has_photo", "count" to "20")
//        callApi("groups.search", params, VkParsers(activity).parseGroupList)
        getMock().getGroups(activity)
    }

    open fun getUsers(activity: SearchActivity, filter: String = "") {
        // TODO add paging, error handling
        val params = mutableListOf("q" to filter, "fields" to "photo_50, has_photo", "count" to "20")
//        callApi("users.search", params, VkParsers(activity).parseUserList)
        getMock().getUsers(activity)
    }

    open fun getCompositions(activity: SearchActivity, filter: String = "") {
        val params = mutableListOf("q" to filter, "fields" to "photo, has_photo", "sort" to "2", "count" to "100")
//        callApi(true, "audio.search", params, VkParsers(activity).parseCompositionList)
        getMock().getCompositions(activity, filter)
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
    override fun getUserPlaylist(activity: SearchActivity, userId: String, filter: String) {
        Log.v("vkMOCK", "Running getUserPlaylistMock")
        val mock = activity.assets.open("getUserPlayList.json").readAll()
        VkParsers(activity).parseUserPlaylist.invoke(mock.toJson())
    }

    override fun getGroupPlaylist(activity: SearchActivity, groupId: String?, filter: String) {
        Log.v("vkMOCK", "Running getGroupPlaylist")
        val mock = activity.assets.open("getUserPlayList.json").readAll()
        VkParsers(activity).parseUserPlaylist.invoke(mock.toJson())
    }

    override fun getDownloaded(filter: String): List<Composition> {
        return getMockCompositionList("downloaded ").filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    override fun getInProgress(filter: String): List<Composition> {
        return getMockCompositionList("inProgress ").filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    override fun getInQueue(filter: String): List<Composition> {
        return getMockCompositionList("inProgress ").filter { it.name.contains(filter) || it.artist.contains(filter) }
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

    private fun getMockCompositionList(id: String = ""): List<Composition> {
        return listOf(Composition("",id + "name", "url", "artist", 0, "", "1:00"),
                Composition("",id + "name1", "url", "artist1", 0, "", "1:00"),
                Composition("",id + "name2", "url", "artist2", 0, "", "1:00"),
                Composition("",id + "name3", "url", "artist3", 0, "", "1:00"),
                Composition("",id + "name4", "url", "artist4", 0, "", "1:00"),
                Composition("",id + "name5", "url", "artist5", 0, "", "1:00"),
                Composition("",id + "name6", "url", "artist6", 0, "", "1:00"),
                Composition("",id + "name7", "url", "artist7", 0, "", "1:00"),
                Composition("",id + "name8", "url", "artist8", 0, "", "1:00"),
                Composition("",id + "name9", "url", "artist9", 0, "", "1:00"))
    }
}
