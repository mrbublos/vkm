package vkm.vkm

import android.content.Context
import com.github.kittinunf.fuel.httpGet
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object SecurityService {

    var user: User? = null

    val appId = "2274003"
    val appSecret = "HbZxrka2uZ6jB1inYsH"
    var vkAccessToken: String? = null
    var context: Context? = null

    fun isLoggedIn(): Boolean {
        val accessToken = loadAccessToken()
        vkAccessToken = accessToken
        return accessToken != null
    }

    fun logIn(newUser: User): String {
        // TODO store user in internal storage
        user = newUser
        return performVkLogin()
    }

    fun performVkLogin(): String {
        val _user = user
        _user?.let {
            val url = "https://oauth.vk.com/token"
            val params = listOf(Pair("grant_type", "password"),
                    Pair("client_id", appId),
                    Pair("client_secret", appSecret),
                    Pair("username", _user.userId),
                    Pair("password", _user.password))
            var resultString = "ok"
            url.httpGet(params).responseString { _, resp, result ->
                if (resp.httpStatusCode != 200) {
                    return@responseString
                } else {
                    dumpAccessToken()
                }
            }
        }

        return "Error logging in"
    }

    fun dumpAccessToken() {
        val name = "mydata.properties"
        val settingsFile = File(context?.filesDir, name)
        val settings = Properties()
        settings.load(FileInputStream(settingsFile))
        settings.put("vkAccessToken", vkAccessToken)
        settings.store(FileOutputStream(settingsFile), null)
    }

    fun loadAccessToken(): String? {
        val name = "mydata.properties"
        val settingsFile = File(context?.filesDir, name)
        if (!settingsFile.exists()) { return null }

        val settings = Properties()
        settings.load(FileInputStream(settingsFile))
        return settings.getProperty("vkAccessToken")
    }
}