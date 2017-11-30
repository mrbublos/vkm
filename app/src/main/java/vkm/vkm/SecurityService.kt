package vkm.vkm

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object SecurityService {

    val appId = "2274003"
    val appSecret = "hHbZxrka2uZ6jB1inYsH"
    val name = "mydata.properties"
    var receipt = ""
    var sender = ""
    var device = ""
    var aidLogin = ""
    var spotifyAppId = ""
    var spotifyAppSecret = ""
    var login = ""
    var deviceId = ""

    var vkAccessToken: String? = null
        set(value) {
            field = value
            value?.let { dumpProperties() }
        }

    var spotifyAccessToken: String? = null
        set(value) {
            field = value
            value?.let { dumpProperties() }
        }

    var context: Context? = null

    fun isLoggedIn(defaultToken: String? = null): Boolean {
        loadProperties()
        vkAccessToken = vkAccessToken ?: defaultToken
        return vkAccessToken != null
    }

    fun dumpProperties() {
        val settingsFile = File(context?.filesDir, name)
        val settings = Properties()
        if (settingsFile.exists()) { settings.load(FileInputStream(settingsFile)) }
        settings.put("vkAccessToken", vkAccessToken)
        settings.put("enableDownloadAll", State.enableDownloadAll.toString())
        settings.put("spotifyAccessToken", spotifyAccessToken ?: "")
        settings.store(FileOutputStream(settingsFile), null)
    }

    private fun loadProperties() {
        val settingsFile = File(context?.filesDir, name)
        if (!settingsFile.exists()) { return }
        val settings = Properties()
        settings.load(FileInputStream(settingsFile))
        vkAccessToken = settings["vkAccessToken"] as String?
        spotifyAccessToken = settings["spotifyAccessToken"] as String?
        State.enableDownloadAll = (settings["enableDownloadAll"] as String?)?.toBoolean() ?: true
    }

    fun clearAll() {
        val settingsFile = File(context?.filesDir, name)
        if (settingsFile.exists()) { settingsFile.delete() }
    }
}