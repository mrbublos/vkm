package vkm.vkm

import android.content.Context
import com.beust.klaxon.string
import com.github.kittinunf.fuel.httpGet
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object SecurityService {

    var user: User? = null

    val appId = "2274003"
    val appSecret = "hHbZxrka2uZ6jB1inYsH"
    var vkAccessToken: String? = null
    var context: Context? = null

    fun isLoggedIn(defaultToken: String?): Boolean {
        loadAccessToken()
        vkAccessToken = vkAccessToken ?: defaultToken
        return vkAccessToken != null
    }

    fun logIn(newUser: User): String {
        // TODO store user in internal storage
        user = newUser
        return performVkLogin()
    }

    fun performVkLogin(): String {
        val _user = user
        var resultString = "Error logging in"

        _user?.let {
            val url = "https://oauth.vk.com/token"
            val params = listOf(Pair("grant_type", "password"),
                    Pair("client_id", appId),
                    Pair("client_secret", appSecret),
                    Pair("username", _user.userId),
                    Pair("password", _user.password))

            val result = url.httpGet(params).responseString()
            val resp = result.component2()
            val res = result.component3()

            if (resp.httpStatusCode == 200) {
                vkAccessToken = res.component1()?.toJson()?.string("access_token")
                dumpAccessToken()
                resultString = "ok"
            }
        }

        return resultString
    }

    fun dumpAccessToken() {
        val name = "mydata.properties"
        val settingsFile = File(context?.filesDir, name)
        val settings = Properties()
        if (settingsFile.exists()) { settings.load(FileInputStream(settingsFile)) }
        settings.put("vkAccessToken", vkAccessToken)
        settings.put("mySecret", vkAccessToken)
        settings.store(FileOutputStream(settingsFile), null)
    }

    private fun loadAccessToken() {
        val name = "mydata.properties"
        val settingsFile = File(context?.filesDir, name)
        if (!settingsFile.exists()) { return }

        val settings = Properties()
        settings.load(FileInputStream(settingsFile))
        vkAccessToken = settings.getProperty("vkAccessToken")
    }
}