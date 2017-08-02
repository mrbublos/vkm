package vkm.vkm

import android.util.Log
import com.github.kittinunf.fuel.httpGet

class MusicService(login: String, password: String) {
    init {
        Log.i("", "MusicService Started")
    }

    val apiUrl = ""

    fun getUserDetails(userId: String) {
        apiUrl.httpGet().responseString { _, resp, result ->

        }
    }

}
