package vkm.vkm.utils.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import vkm.vkm.utils.Composition
import vkm.vkm.utils.Proxy

@Database(entities = [Composition::class, Proxy::class], version = 1)
abstract class Db : RoomDatabase() {
    companion object {
        @Volatile
        private var instance: Db? = null

        fun instance(context: Context): Db {
            val i = instance
            if (i != null) { return i }

            return synchronized(this) {
                val i2 = instance
                if (i2 != null) {
                    i2
                } else {
                    val newInstance = Room.databaseBuilder(context.applicationContext, Db::class.java, "vkm.db").build()
                    instance = newInstance
                    newInstance
                }
            }
        }
    }

    abstract fun tracksDao(): TracksDao
    abstract fun proxyDao(): ProxyDao
}