package vkm.vkm.utils.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import vkm.vkm.utils.Proxy

@Dao
interface ProxyDao {

    @Query("SELECT * FROM blacklisted_proxy")
    fun getAll(): List<Proxy>

    @Insert
    fun insertAll(list: List<Proxy>)

    @Insert
    fun insert(proxy: Proxy)

    @Delete
    fun delete(proxy: Proxy)

    @Query("DELETE FROM blacklisted_proxy")
    fun deleteAll()
}