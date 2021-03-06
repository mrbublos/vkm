package vkm.vkm.utils

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Composition(var id: String = "", var name: String = "", var url: String = "",
                       var artist: String = "", @Ignore var progress: Int = 0, var hash: String = "",
                       var length: String = "", var ownerId: String = "", var status: String = "downloaded",
                       @PrimaryKey var vkmId: Long = System.nanoTime()) {
    override fun equals(other: Any?): Boolean {
        if (other !is Composition) { return false }
        return name == other.name && artist == other.artist
    }
}

data class User(var userId: String = "", var password: String = "", var token: String = "", var fullname: String = "", var photoUrl: String = "", @Transient var photo: Bitmap? = null, var isGroup: Boolean = false)

@Entity(primaryKeys = ["host", "port"])
data class Proxy(val host: String,
                 val port: Int,
                 val country: String = "", var type: String = "", val speed: Int = 0,
                 var added: Long = System.currentTimeMillis())

abstract class CompositionContainer(var compositions: List<Composition>? = null,
                                    @Transient var compositionFetcher: suspend ((page: Int) -> List<Composition>) = { listOf() })
data class Album(val id: String, val name: String, val url: String, val artist: String) : CompositionContainer()
data class Artist(val id: String, val name: String, val url: String) : CompositionContainer()