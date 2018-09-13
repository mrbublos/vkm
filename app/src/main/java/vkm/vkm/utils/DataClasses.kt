package vkm.vkm.utils

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.graphics.Bitmap

@Entity(tableName = "tracks")
data class Composition(var id: String = "", var name: String = "", var url: String = "",
                       var artist: String = "", var progress: Int = 0, var hash: String = "",
                       var length: String = "", var ownerId: String = "", var status: String = "downloaded",
                       @PrimaryKey
        var vkmId: Long = System.nanoTime())

data class User(var userId: String = "", var password: String = "", var token: String = "", var fullname: String = "", var photoUrl: String = "", @Transient var photo: Bitmap? = null, var isGroup: Boolean = false)
data class Proxy(val host: String, val port: Int, val country: String = "", val type: String, val speed: Int)
data class Album(val id: String, val name: String, val url: String, val artist: String,
                 var compositions: List<Composition>? = null,
                 @Transient var compositionFetcher: (() -> Unit)? = null)