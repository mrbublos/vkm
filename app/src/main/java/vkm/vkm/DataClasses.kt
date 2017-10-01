package vkm.vkm

import android.graphics.Bitmap

data class Composition(var id: String = "", var name: String = "", var url: String = "", var artist: String = "", var progress: Int = 0, var hash: String = "", var length: String = "", var ownerId: String = "", var vkmId: Long = System.currentTimeMillis())

data class User(var userId: String = "", var password: String = "", var token: String = "", var fullname: String = "", var photoUrl: String = "", var photo: Bitmap? = null, var isGroup: Boolean = false)
data class Proxy(val host: String, val port: Int, val country: String, val type: String)