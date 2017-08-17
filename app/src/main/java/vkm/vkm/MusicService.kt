package vkm.vkm

import android.os.AsyncTask
import android.util.Log
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.int
import com.beust.klaxon.string
import com.github.kittinunf.fuel.httpGet

/**
 * todo decompile off app and get client_id and key
 */
open class MusicService {

    var token: String?

    init {
        Log.i(MainActivity.TAG, "MusicService Started")
        token = SecurityService.vkAccessToken
    }

    open fun getUserPlaylist(activity: SearchActivity, name: String?, filter: String = ""): List<Composition> {
        return listOf()
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
        callApi("groups.search", mutableListOf(Pair("q", filter), Pair("fields", "photo_50"))) { result ->
            val items = result?.get("response") as JsonArray<Any?>
            val groups = items.filter {
                it is JsonObject
            }.map {
                val group = it as JsonObject
                val newGroup = User(userId = "" + group.int("gid")!!,
                        fullname = group.string("name")!!,
                        photoUrl = group.string("photo_50")!!)
                newGroup
            }
            activity.setGroupList(groups)
        }
    }

    open fun getUsers(activity: SearchActivity, filter: String = "") {
        // TODO add paging, error handling
        callApi("users.search", mutableListOf(Pair("q", filter), Pair("fields", "photo_50, has_photo"))) { result ->
            val items = result?.get("response") as JsonArray<Any?>
            val users = items.filter {
                it is JsonObject
            }.map {
                val user = it as JsonObject
                val newUser = User(userId = "" + user.int("uid")!!,
                        fullname = user.string("first_name") + " " + user.string("last_name"))
                if (user.int("has_photo") == 1) {
                    newUser.photoUrl = user.string("photo_50")!!
                }
                newUser
            }
            activity.setUserList(users)
        }
    }

    open fun getCompositions(activity: SearchActivity, filter: String = ""): List<Composition> {
        return listOf(Composition()).filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    fun getMock(): MusicServiceMock {
        return MusicServiceMock()
    }

    fun callApi(path: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        VkApiCallTask(callback).execute(Pair(path, params))
    }
}

class VkApiCallTask(val callback: (data: JsonObject?) -> Unit): AsyncTask<Pair<String, MutableList<Pair<String, String>>>, Int, JsonObject?>() {
    val apiUrl = "https://api.vk.com"

    override fun doInBackground(vararg input: Pair<String, MutableList<Pair<String, String>>>): JsonObject? {
        val parameters = input[0].component2()
        parameters.add(Pair("access_token", SecurityService.vkAccessToken!!))
        val (_, _, result) = "$apiUrl/method/${input[0].component1()}".httpGet(parameters).responseString()
        return result.component1()?.toJson()
    }

    override fun onPostExecute(result: JsonObject?) {
        callback.invoke(result)
    }
}


class MusicServiceMock : MusicService() {
    override fun getUserPlaylist(activity: SearchActivity, name: String?, filter: String): List<Composition> {
        return getMockCompositionList("user playlist ").filter { it.name.contains(filter) || it.artist.contains(filter) }
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
//        return getMockUserList("groups ").filter { it.fullname.contains(filter) }
    }

    override fun getCompositions(activity: SearchActivity, filter: String): List<Composition> {
        return getMockCompositionList("compositions ").filter { it.name.contains(filter) || it.artist.contains(filter) }

}

    fun getMockCompositionList(id: String = ""): List<Composition> {
        return listOf(Composition(id + "name", "url", "artist", 0, "", "1:00"),
                Composition(id + "name1", "url", "artist1", 0, "", "1:00"),
                Composition(id + "name2", "url", "artist2", 0, "", "1:00"),
                Composition(id + "name3", "url", "artist3", 0, "", "1:00"),
                Composition(id + "name4", "url", "artist4", 0, "", "1:00"),
                Composition(id + "name5", "url", "artist5", 0, "", "1:00"),
                Composition(id + "name6", "url", "artist6", 0, "", "1:00"),
                Composition(id + "name7", "url", "artist7", 0, "", "1:00"),
                Composition(id + "name8", "url", "artist8", 0, "", "1:00"),
                Composition(id + "name9", "url", "artist9", 0, "", "1:00"))
    }

    fun getMockUserList(id: String = ""): List<User> {
        val prefix = Math.random()
        return listOf(
                User(userId = "" + prefix + id + 1, fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 2, fullname = "Fullname 2", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 3, fullname = "Fullname 3", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 4, fullname = "Fullname 4", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 5, fullname = "Fullname 5", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 6, fullname = "Fullname 6", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 7, fullname = "Fullname 7", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 8, fullname = "Fullname 8", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 9, fullname = "Fullname 9", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "" + prefix + id + 10,fullname = "Fullname 0", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg")
                )
    }
}
