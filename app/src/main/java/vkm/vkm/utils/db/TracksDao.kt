package vkm.vkm.utils.db

import androidx.room.*
import vkm.vkm.utils.Composition

@Dao
interface TracksDao {

    @Query("SELECT * FROM tracks WHERE status = 'downloaded' ORDER BY vkmId ASC")
    fun getDownloadedTracks(): MutableList<Composition>

    @Query("SELECT * FROM tracks WHERE status = 'queued'")
    fun getQueuedTracks(): MutableList<Composition>

    @Insert
    fun insertAll(data: List<Composition>)

    @Insert
    fun insert(composition: Composition)

    @Delete
    fun delete(item: Composition)

    @Query("DELETE FROM tracks WHERE status = 'downloaded'")
    fun deleteAllDownloaded()

    @Query("DELETE FROM tracks WHERE status = 'queued'")
    fun deleteAllQueued()

    @Update
    fun update(data: Composition)
}