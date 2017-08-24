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

    open fun getGroupPlaylist(activity: SearchActivity, groupId: String?, filter: String = ""): List<Composition> {
        return listOf()
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
//        callApi("groups.search", mutableListOf("q" to filter, "fields" to "photo_50"), VkParsers(activity).parseGroupList)
        getMock().getGroups(activity)
    }

    open fun getUsers(activity: SearchActivity, filter: String = "") {
        // TODO add paging, error handling
//        callApi("users.search", mutableListOf("q" to filter, "fields" to "photo_50, has_photo"), VkParsers(activity).parseUserList)
        getMock().getUsers(activity)
    }

    open fun getCompositions(activity: SearchActivity, filter: String = "") {
        callApi("audio.search", mutableListOf("q" to filter, "fields" to "photo_50, has_photo"), VkParsers(activity).parseUserList)
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
        val mock = activity.assets.open("getUserPlayList.json").toString()
        VkParsers(activity).parseUserPlaylist.invoke(mock.toJson())
    }

    override fun getGroupPlaylist(activity: SearchActivity, groupId: String?, filter: String): List<Composition> {
        return getMockCompositionList("group playlist ").filter { it.name.contains(filter) || it.artist.contains(filter) }
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
        val mock = activity.assets.open("getGroups.json").toString()
        VkParsers(activity).parseGroupList.invoke(mock.toJson())
    }

    override fun getUsers(activity: SearchActivity, filter: String) {
        val mock = activity.assets.open("getUsers.json").toString()
        VkParsers(activity).parseUserList.invoke(mock.toJson())
    }

    override fun getCompositions(activity: SearchActivity, filter: String) {
        return activity.setCompositionsList(getMockCompositionList("compositions ").filter { it.name.contains(filter) || it.artist.contains(filter) })
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
