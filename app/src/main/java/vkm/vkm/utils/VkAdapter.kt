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
    suspend fun performVkLogin(user: String, password: String): String {
        var resultString = "Error logging in"

        val url = "https://oauth.v" + "k.com/token"
        val params = listOf(Pair("grant_type", "password"),
                Pair("client_id", SecurityService.appId),
                Pair("client_secret", SecurityService.appSecret),
                Pair("username", user),
                Pair("password", password))

        val result = url.httpGet(params).responseString()
        val resp = result.component2()
        val res = result.component3()

        if (resp.statusCode == 200) {
            val responseJson = res.component1()?.toJson()
            "Received response $responseJson".log()
            SecurityService.vkAccessToken = responseJson?.string("access_token")
            SecurityService.dumpProperties()
            resultString = "ok"
        } else {
            res.component2().toString().logE()
        }

        return resultString
    }

    suspend fun callVkMethod(isGet:Boolean = true, parameters: MutableList<Pair<String, String>>, method: String, addSignature: Boolean = true): JsonObject? {
        val result = callVkApiMethod(isGet, parameters, method, addSignature)
        when (result?.containsKey("error")) {
            false -> return result
            else -> {
                if (result == null) { return null }

                return if ((result["error"] as JsonObject)["error_code"] == 25 && method != "auth.refreshToken") {
                    // token confirmation required, refreshing token
                    try {
                        SecurityService.receipt = getReciept()
                        if (refreshToken()) { callVkApiMethod(isGet, parameters, method, addSignature) } else null
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

    suspend fun refreshToken(): Boolean {
        val receipt = getReciept()
        if (receipt.isEmpty()) {
            "Unable to retrieve receipt".logE()
            return false
        }

        val responseString = callVkApiMethod(true, mutableListOf("v" to "5.68", "receipt" to receipt), "auth.refre" + "shToken")!!["response"]
        if (responseString == null) {
            "Unable to connect to proxy".logE()
            return false
        }

        ((responseString as JsonObject)["token"] as String?)?.let { newToken ->
            if (newToken != SecurityService.vkAccessToken) {
                SecurityService.vkAccessToken = newToken
                return true
            } else {
                "Token refresh failed (same token returned)".logE()
            }
        }
        return false
    }

    suspend fun unregisterDevice(): Boolean {
        val parameters = mutableListOf("v" to "5.68", "https" to "1", "device_id" to SecurityService.deviceId)
        val resp = callVkMethod(false, parameters, "account.unregisterDevice")?.get("response")
        if (resp != 1) {
            "Failed to unregister device".logE()
            return false
        }

        return true
    }

    suspend fun registerDevice(): Boolean {
//        SecurityService.sender = "${System.currentTimeMillis()}"
        SecurityService.receipt = getReciept()
        val parameters = mutableListOf("v" to "5.68",
                "https" to "1",
                "token" to SecurityService.receipt,
                "system_version" to "6.0",
                "device_model" to "Unknown Android SDK built for x86",
                "type" to "4",
                "gcm" to "1",
                "settings" to "{\"sdk_open\":\"on\",\"new_post\":\"on\",\"friend_accepted\":\"on\",\"wall_publish\":\"on\",\"group_accepted\":\"on\",\"money_transfer\":\"on\",\"msg\":\"on\",\"chat\":\"on\",\"friend\":\"on\",\"friend_found\":\"on\",\"reply\":\"on\",\"comment\":\"on\",\"mention\":\"on\",\"like\":\"on\",\"repost\":\"on\",\"wall_post\":\"on\",\"group_invite\":\"on\",\"event_soon\":\"on\",\"tag_photo\":\"on\",\"tag_video\":\"on\",\"app_request\":\"on\",\"gift\":\"on\",\"birthday\":\"on\",\"live\":\"on\"}",
                "app_version" to "1206",
                "device_id" to SecurityService.deviceId)
        val resp = callVkMethod(false, parameters, "account.registerDevice")?.get("response")
        if (resp != 1) {
            "Failed to register device".logE()
            return false
        }

        return true
    }

    suspend private fun callVkApiMethod(isGet: Boolean = true, parameters: MutableList<Pair<String, String>>, method: String, addSignature: Boolean = true): JsonObject? {
        if (SecurityService.vkAccessToken == null) {
            "Token not defined".logE()
            return null
        }

        val _apiUrl = "https://api.v" + "k.com"
        val _userAgent = "VKAn" + "droidApp/4.13-1183 (Android 7.1.1; SDK 25; x86; unknown Android SDK built for x86_64; en)"
        val path = "/method/$method"

        parameters.add("access_token" to SecurityService.vkAccessToken!!)

        // should be the last computed parameter
        if (addSignature) {
            addSignature(path, parameters)
        }
        val httpGet = if (isGet) "$_apiUrl$path".httpGet(parameters) else "$_apiUrl$path".httpPost(parameters)
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
                "sender" to SecurityService.sender,
                "device" to SecurityService.device,
                "X-subtype" to SecurityService.sender)
        val headers = listOf("Authorization" to SecurityService.aidLogin,
                "Content-Type" to "application/x-www-form-urlencoded")
        val post = url.httpPost(params)
        post.headers.putAll(headers)
        "Getting receipt ${post.cUrlString()}".log()
        val (_, _, result) = post.responseString()
        "Receipt received ${result.component1()}".log()
        return result.component1()?.split("=")?.get(1) ?: ""
    }
}
