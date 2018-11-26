package vkm.vkm.utils.db

import android.arch.persistence.room.*
import vkm.vkm.utils.Proxy

@Dao
interface ProxyDao {

    @Query("SELECT * FROM proxy")
    fun getAll(): List<Proxy>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<Proxy>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(proxy: Proxy)

    @Delete
    fun delete(proxy: Proxy)

    @Query("DELETE FROM proxy")
    fun deleteAll()
}