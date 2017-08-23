package vkm.vkm

import android.os.AsyncTask
import android.util.Log
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.int
import com.beust.klaxon.string
import com.github.kittinunf.fuel.httpGet
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * todo decompile off app and get client_id and key
 */
open class MusicService {

    init {
        Log.i(MainActivity.TAG, "MusicService Started")
    }

    open fun getUserPlaylist(activity: SearchActivity, name: String, filter: String = ""): List<Composition> {
        val params = mutableListOf(
                "v" to "5.68",
                "lang" to "en",
                "https" to "1",
                "owner_id" to name,
                "count" to "200",
                "extended" to "1",
                "shuffle" to "0")
        callApi(true,"audio.get", params) { result ->
            if (result == null) {
                activity.setCompositionsList(listOf())
                return@callApi
            }

            val items = result["response"] as JsonArray<*>
            val compositions = items.filter {
                it is JsonObject
            }.map {
                val composition = it as JsonObject
                val compositionObject = Composition( id = "" + composition.int("aid")!!,
                        name = composition.string("title")!!,
                        ownerId = composition.string("owner_id")!!,
                        artist = composition.string("artist")!!,
                        url = composition.string("url")!!)
                compositionObject
            }
            activity.setCompositionsList(compositions)
        }
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
        callApi("groups.search", mutableListOf("q" to filter, "fields" to "photo_50")) { result ->
            if (result == null) {
                activity.setGroupList(listOf())
                return@callApi
            }

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
        callApi("users.search", mutableListOf("q" to filter, "fields" to "photo_50, has_photo")) { result ->
            if (result == null) {
                activity.setUserList(listOf())
                return@callApi
            }

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


    private fun callApi(path: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        VkApiCallTask(callback).execute(Pair(path, params))
    }

    private fun callApi(addSignature: Boolean = false, path: String, params: MutableList<Pair<String, String>>, callback: (result: JsonObject?) -> Unit) {
        VkApiCallTask(callback, addSignature).execute(Pair(path, params))
    }
}

class VkApiCallTask(private val callback: (data: JsonObject?) -> Unit, private val addSignature: Boolean = false): AsyncTask<Pair<String, MutableList<Pair<String, String>>>, Int, JsonObject?>() {
    private val _apiUrl = "https://api.vk.com"
    private val _userAgent = "VKAndroidApp/4.13-1183 (Android 7.1.1; SDK 25; x86; unknown Android SDK built for x86_64; en)"
    private var _params: MutableList<Pair<String, String>> = mutableListOf()
    private var _method: String = ""

    override fun doInBackground(vararg input: Pair<String, MutableList<Pair<String, String>>>): JsonObject? {
        val parameters = input[0].component2()
        parameters.add(Pair("access_token", SecurityService.vkAccessToken!!))
        _method = input[0].component1()
        val path = "/method/$_method"
        if (addSignature) { addSignature(path, parameters) }

        _params.addAll(parameters)


        val httpGet = "$_apiUrl$path".httpGet(parameters)
        httpGet.httpHeaders.put("User-Agent", _userAgent)
        Log.v("vkAPI",  "Sending request " + httpGet.cUrlString())
        val (req, resp, result) = httpGet.responseString()
        Log.v("vkAPI", "Response received " + result?.component1())
        return result.component1()?.toJson()
    }

    override fun onPostExecute(result: JsonObject?) {
        when (result?.containsKey("error")) {
            false -> callback.invoke(result)
            else -> {
                if ((result?.get("error") as JsonObject)["error_code"] == 25 && _method != "auth.refreshToken") {
                    // token confirmation required
                    VkApiCallTask({
                        SecurityService.vkAccessToken = (it!!["response"] as JsonObject)["token"] as String
                        // repeating the call
                        VkApiCallTask(callback, addSignature).execute(_method to _params)
                    }, false).execute("auth.refreshToken" to mutableListOf("v" to "5.68",
                            "receipt" to ""))
                } else {
                    Log.e("vkAPI", "Received an error " + (result["error"] as JsonObject)["error_msg"])
                    callback.invoke(null)
                }
            }
        }
    }

    private fun addSignature(path: String, params: MutableList<Pair<String, String>>) {
        val string = path + "?" + params.joinToString("&") { "${it.first}=${it.second}" } + SecurityService.appSecret
        Log.v("", "Signature string " + string)
        val md = MessageDigest.getInstance("MD5")
        val md5String = md.digest(string.toByteArray(StandardCharsets.UTF_8)).toHexString()
        params.add("sig" to md5String)
        Log.v("", "Signature is " + md5String)
    }
}


class MusicServiceMock : MusicService() {
    override fun getUserPlaylist(activity: SearchActivity, name: String, filter: String): List<Composition> {
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
