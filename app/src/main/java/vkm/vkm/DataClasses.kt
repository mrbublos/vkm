package vkm.vkm

import android.graphics.Bitmap

data class Composition(var id: String = "", var name: String = "", var url: String = "", var artist: String = "", var progress: Int = 0, var hash: String = "", var length: String = "", var ownerId: String = "")

data class User(var userId: String = "", var password: String = "", var token: String = "", var fullname: String = "", var photoUrl: String = "", var photo: Bitmap? = null, var isGroup: Boolean = false)