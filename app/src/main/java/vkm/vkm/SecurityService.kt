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

    var vkAccessToken: String? = null
        set(value) {
            field = value
            value?.let { dumpProperties() }
        }

    var context: Context? = null
    var user: User? = null

    fun isLoggedIn(defaultToken: String? = null): Boolean {
        loadProperties()
        vkAccessToken = vkAccessToken ?: defaultToken
        return vkAccessToken != null
    }

    fun logIn(newUser: User): String {
        // TODO store user in internal storage
        user = newUser
        return VkApi.performVkLogin()
    }

    fun dumpProperties() {
        val settingsFile = File(context?.filesDir, name)
        val settings = Properties()
        if (settingsFile.exists()) { settings.load(FileInputStream(settingsFile)) }
        settings.put("vkAccessToken", vkAccessToken)
        settings.put("mySecret", vkAccessToken)
        settings.put("enableDownloadAll", StateManager.enableDownloadAll)
        settings.store(FileOutputStream(settingsFile), null)
    }

    private fun loadProperties() {
        val settingsFile = File(context?.filesDir, name)
        if (!settingsFile.exists()) { return }
        val settings = Properties()
        settings.load(FileInputStream(settingsFile))
        vkAccessToken = settings["vkAccessToken"] as String
        StateManager.enableDownloadAll = settings["enableDownloadAll"] as Boolean
    }

    fun clearAll() {
        val settingsFile = File(context?.filesDir, name)
        if (settingsFile.exists()) { settingsFile.delete() }
    }
}