package vkm.vkm.utils

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.int
import com.beust.klaxon.string
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import vkm.vkm.SearchFragment
import vkm.vkm.SecurityService
import vkm.vkm.State

class VkParsers(private val fragment: SearchFragment) {
    val parseUserList = { result: JsonObject? ->
        if (result == null) {
            fragment.setUserList(listOf())
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
            fragment.setUserList(users)
        }
    }

    val parseGroupList = { result: JsonObject? ->
        if (result == null) {
            fragment.setGroupList(listOf())
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
            fragment.setGroupList(groups)
        }
    }

    val parsePlaylist = { result: JsonObject? ->
        if (result == null) {
            fragment.setCompositionsList(listOf())
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
            State.totalCompositions = (result["response"] as JsonObject).int("count")!!
            fragment.setCompositionsList(compositions)
        }
    }

    val parseCompositionList = { result: JsonObject? ->
        if (result == null) {
            fragment.setCompositionsList(listOf())
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
            fragment.setCompositionsList(compositions)
            State.totalCompositions = (result["response"] as JsonObject).int("count")!!
        }
    }
}

object VkApi {
    fun performVkLogin(): String {
        val _user = SecurityService.user
        var resultString = "Error logging in"

        _user?.let {
            val url = "https://oauth.v" + "k.com/token"
            val params = listOf(Pair("grant_type", "password"),
                    Pair("client_id", SecurityService.appId),
                    Pair("client_secret", SecurityService.appSecret),
                    Pair("username", _user.userId),
                    Pair("password", _user.password))

            val result = url.httpGet(params).responseString()
            val resp = result.component2()
            val res = result.component3()

            if (resp.statusCode == 200) {
                SecurityService.vkAccessToken = res.component1()?.toJson()?.string("access_token")
                SecurityService.dumpProperties()
                resultString = "ok"
            } else {
                res.component2().toString().logE()
            }
        }

        return resultString
    }

    suspend fun callVkMethod(parameters: MutableList<Pair<String, String>>, method: String, addSignature: Boolean = true): JsonObject? {
        val result = callVkApiMethod(parameters, method, addSignature)
        when (result?.containsKey("error")) {
            false -> return result
            else -> {
                if (result == null) { return null }

                return if ((result["error"] as JsonObject)["error_code"] == 25 && method != "auth.refreshToken") {
                    // token confirmation required, refreshing token
                    try {
                        SecurityService.receipt = getReciept()
                        refreshToken()
                        callVkApiMethod(parameters, method, addSignature)
                    } catch (e: Exception) {
                        "Error in refresh token or secondary call".logE(e)
                        null
                    }
                } else {
                    "Received an error ${(result["error"] as JsonObject)["error_msg"]}".logE()
                    null
                }
            }
        }
    }

    suspend fun refreshToken() {
        SecurityService.vkAccessToken = (callVkApiMethod(mutableListOf("v" to "5.68", "receipt" to SecurityService.receipt), "auth.refreshToken")!!["response"] as JsonObject)["token"] as String? ?: SecurityService.vkAccessToken
    }

    suspend private fun callVkApiMethod(parameters: MutableList<Pair<String, String>>, method: String, addSignature: Boolean = true): JsonObject? {
        val _apiUrl = "https://api.v" + "k.com"
        val _userAgent = "VKAn" + "droidApp/4.13-1183 (Android 7.1.1; SDK 25; x86; unknown Android SDK built for x86_64; en)"
        val path = "/method/$method"

        parameters.add("access_token" to SecurityService.vkAccessToken!!)

        // should be the last computed parameter
        if (addSignature) {
            addSignature(path, parameters)
        }
        val httpGet = "$_apiUrl$path".httpGet(parameters)
        httpGet.headers.put("User-Agent", _userAgent)
        "Sending request ${httpGet.cUrlString()}".log()
        val (_, _, result) = httpGet.responseString()
        "Response received ${result.component1()}".log()
        return try { result.component1()?.toJson() } catch (e: Exception) { null }
    }

    private fun addSignature(path: String, params: MutableList<Pair<String, String>>) {
        val string = path + "?" + params.joinToString("&") { "${it.first}=${it.second}" } + SecurityService.appSecret
        "Signature string $string".log()
        params.add("sig" to string.md5())
        "Signature is ${string.md5()}".log()
    }

    suspend fun getReciept(): String {
        val url = "https://android.clients.google.com/c2dm/register3"
        val params = listOf("X-scope" to "GCM",
                "app" to "com.vkont" + "akte.android",
                "sender" to "191410808405",
                "device" to "3949256210147014230")
        val headers = listOf("Authorization" to "AidLogin 3949256210147014230:1372471507630590001",
                "Content-Type" to "application/x-www-form-urlencoded")
        val post = url.httpPost(params)
        post.headers.putAll(headers)
        val (_, _, result) = post.responseString()
        "Receipt received ${result.component1()}".log()
        return result.component1()?.split("=")?.get(1) ?: ""
    }
}
