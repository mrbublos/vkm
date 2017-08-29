package vkm.vkm

import android.os.AsyncTask
import android.util.Log
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.int
import com.beust.klaxon.string
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import java.net.Proxy

class VkParsers(private val activity: SearchActivity) {
    val parseUserList = { result: JsonObject? ->
        if (result == null) {
            activity.setUserList(listOf())
        } else {
            val items = result["response"] as JsonArray<*>
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

    val parseGroupList = { result: JsonObject? ->
        if (result == null) {
            activity.setGroupList(listOf())
        } else {
            val items = result["response"] as JsonArray<*>
            val groups = items.filter {
                it is JsonObject
            }.map {
                val group = it as JsonObject
                val newGroup = User(userId = "" + group.int("gid")!!,
                        fullname = group.string("name")!!,
                        isGroup = true,
                        photoUrl = group.string("photo")!!)
                newGroup
            }
            activity.setGroupList(groups)
        }
    }

    val parsePlaylist = { result: JsonObject? ->
        if (result == null) {
            activity.setCompositionsList(listOf())
        } else {
            val items = (result["response"] as JsonObject)["items"] as JsonArray<*>
            val compositions = items.filter {
                it is JsonObject
            }.map {
                val composition = it as JsonObject
                val compositionObject = Composition(id = "" + composition.int("id")!!,
                        name = composition.string("title")!!,
                        ownerId = "" + composition.int("owner_id")!!,
                        artist = composition.string("artist")!!,
                        url = composition.string("url")!!)
                compositionObject
            }
            activity.setCompositionsList(compositions)
        }
    }

    val parseCompositionList = { result: JsonObject? ->
        if (result == null) {
            activity.setCompositionsList(listOf())
        } else {
            val items = (result["response"] as JsonObject)["items"] as JsonArray<*>
            val compositions = items.filter {
                it is JsonObject && !it.string("url")!!.isEmpty()
            }.map {
                val composition = it as JsonObject
                val compositionObject = Composition(id = "" + composition.int("id")!!,
                        name = composition.string("title")!!,
                        ownerId = "" + composition.int("owner_id")!!,
                        artist = composition.string("artist")!!,
                        url = composition.string("url")!!)
                compositionObject
            }
            activity.setCompositionsList(compositions)
        }
    }
}

class VkApiCallTask(private val callback: (data: JsonObject?) -> Unit, private val addSignature: Boolean = false, private val recursionLevel: Int = 0): AsyncTask<Pair<String, MutableList<Pair<String, String>>>, Int, JsonObject?>() {
    private val _apiUrl = "https://api.vk.com"
    private val _userAgent = "VKAndroidApp/4.13-1183 (Android 7.1.1; SDK 25; x86; unknown Android SDK built for x86_64; en)"
    private var _params: MutableList<Pair<String, String>> = mutableListOf()
    private var _method: String = ""
    private val proxyAddress = PropertyContainer.proxies[3]
//    private val proxy: Proxy? = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyAddress.first, proxyAddress.second.toInt()))
    private val proxy: Proxy? = null

    init {
        proxy?.let { FuelManager.instance.proxy = proxy }
    }

    override fun doInBackground(vararg input: Pair<String, MutableList<Pair<String, String>>>): JsonObject? {
        val parameters = input[0].component2()
        _params.addAll(parameters)
        _method = input[0].component1()
        val path = "/method/$_method"

        parameters.add("access_token" to SecurityService.vkAccessToken!!)

        // should be the last computed parameter
        if (addSignature) { addSignature(path, parameters) }
        val httpGet = "$_apiUrl$path".httpGet(parameters)
        httpGet.httpHeaders.put("User-Agent", _userAgent)
        Log.v("vkAPI",  "Sending request " + httpGet.cUrlString())
        val (_, _, result) = httpGet.responseString()
        Log.v("vkAPI", "Response received " + result.component1())
        return result.component1()?.toJson()
    }

    override fun onPostExecute(result: JsonObject?) {
        when (result?.containsKey("error")) {
            false -> callback.invoke(result)
            else -> {
                if (result == null) {
                    callback.invoke(null)
                    return
                }

                if ((result?.get("error") as JsonObject)["error_code"] == 25 && _method != "auth.refreshToken" && recursionLevel == 0) {
                    // token confirmation required, refreshing token
                    VkApiCallTask({ refreshTokenResult ->
                        SecurityService.vkAccessToken = (refreshTokenResult!!["response"] as JsonObject)["token"] as String

                        // repeating the call
                        VkApiCallTask(callback, addSignature, 1).execute(_method to _params)
                    }).execute("auth.refreshToken" to mutableListOf("v" to "5.68",
                            "receipt" to SecurityService.receipt))
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
        params.add("sig" to string.md5())
        Log.v("vkAPI", "Signature is " + string.md5())
    }
}
