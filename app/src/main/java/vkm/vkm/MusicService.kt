package vkm.vkm

import android.util.Log

import com.github.kittinunf.fuel.httpGet

/**
 * todo decompile off app and get client_id and key
 */
open class MusicService {

    var token: String

    init {
        Log.i("", "MusicService Started")
        token = authorize()
    }

    val apiUrl = "https://api.vk.com"
    val appId = "3682744"


    fun initialize() {

    }

    fun getUserDetails(userId: String) {
        val url = "$apiUrl/method/users.get"
        val params = listOf<Pair<String, Any>>(Pair("user_id", userId))
        url.httpGet(params).responseString { _, resp, result ->
            println("test")
        }
    }


    // TODO make return type non-nullable
    open fun getUserPlaylist(name: String, filter: String = ""): List<Composition> {
        val url = "$apiUrl/method/users.search"
        val params = listOf<Pair<String, Any>>(Pair("q", name))
        url.httpGet(params).responseString { _, resp, result ->
            println("test")
        }

        return listOf()
    }

    open fun getGroupPlaylist(groupId: String, filter: String = ""): List<Composition> {
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

    open fun getGroups(filter: String = ""): List<User> {
        return listOf(User()).filter { it.fullname.contains(filter) }
    }

    open fun getUsers(filter: String = ""): List<User> {
        return listOf(User()).filter { it.fullname.contains(filter) }
    }

    open fun getCompositions(filter: String = ""): List<Composition> {
        return listOf(Composition()).filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    fun authorize(): String {
        val url = "https://oauth.vk.com/token"
        val params = listOf<Pair<String, Any>>(Pair("grant_type", "password"),
                Pair("scope", "nohttps"), Pair("client_id", appId), Pair("client_secret", ""), Pair("username", ""), Pair("password", ""))

        url.httpGet(params).responseString { _, resp, result ->
            println("test")
        }

        return ""
    }

    fun getMock(): MusicServiceMock {
        return MusicServiceMock()
    }
}

class MusicServiceMock : MusicService() {
    override fun getUserPlaylist(name: String, filter: String): List<Composition> {
        return getMockCompositionList("user playlist ").filter { it.name.contains(filter) || it.artist.contains(filter) }
    }

    override fun getGroupPlaylist(groupId: String, filter: String): List<Composition> {
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

    override fun getUsers(filter: String): List<User> {
        return getMockUserList("users ").filter { it.fullname.contains(filter) }
    }

    override fun getGroups(filter: String): List<User> {
        return getMockUserList("groups ").filter { it.fullname.contains(filter) }
    }

    override fun getCompositions(filter: String): List<Composition> {
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
        return listOf(
                User(userId = "id" + 1, fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 2, fullname = "Fullname 2", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 3, fullname = "Fullname 3", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 4, fullname = "Fullname 4", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 5, fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 6, fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 7, fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 8, fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 9, fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg"),
                User(userId = "id" + 10,fullname = "Fullname 1", photoUrl = "http://assets.teenvogue.com/photos/58c703466b185d38dd28d29d/master/pass/Portrait%20Cara%20Delevingne.cpImg.868.jpg")
                )
    }
}
