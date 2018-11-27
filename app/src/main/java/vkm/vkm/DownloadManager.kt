package vkm.vkm

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import vkm.vkm.utils.*
import vkm.vkm.utils.db.TracksDao
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

object DownloadManager {

    // TODO this should be a background service, but i do not care now 8)

    private val downloadedList = ConcurrentLinkedQueue<Composition>()
    private val inProgress = ConcurrentLinkedQueue<Composition>()
    private val queue = ConcurrentLinkedQueue<Composition>()
    var downloadedPercent = 0
    private val lock = Mutex()
    private var dao: TracksDao? = null

    fun initialize(dao: TracksDao) {
        "Loading all lists".log()
        this.dao = dao
        launch(CommonPool) {
            downloadedList.clear()
            queue.clear()
            "Fetching data from db".log()
            downloadedList.addAll(dao.getDownloadedTracks())
            queue.addAll(dao.getQueuedTracks())
            "Data fetching from db complete ${downloadedList.size}".log()
        }
    }

    fun clearDownloaded() {
        downloadedList.clear()
        launch(CommonPool) { dao?.deleteAllDownloaded() }
        "Cleared downloaded list".log()
    }

    fun clearQueue() {
        queue.clear()
        launch(CommonPool) { dao?.deleteAllQueued() }
        "Cleared queue list".log()
    }

    fun downloadComposition(composition: Composition?) {
        composition?.vkmId = System.nanoTime()
        composition?.takeIf { it.url.isNotEmpty() }?.let {
            queue.offer(composition)
            composition.status = "queued"
            launch(CommonPool) { dao?.insert(composition) }
        }
        downloadNext()
    }

    fun getQueue(): List<Composition> {
        return queue.mapNotNull { it }
    }

    fun getDownloaded(): List<Composition> {
        return downloadedList.mapNotNull { it }.reversed()
    }

    fun getInProgress(): List<Composition> {
        return inProgress.mapNotNull { it }
    }

    fun removeFromQueue(composition: Composition) {
        queue.find { it.id == composition.id }?.let {
            queue.remove(it)
            launch(CommonPool) { dao?.delete(it) }
        }
    }

    fun removeDownloaded(composition: Composition) {
        downloadedList.find { it.vkmId == composition.vkmId }?.let {
            downloadedList.remove(it)
            launch(CommonPool) { dao?.delete(it) }
        }
    }

    fun getDownloadDir(): File {
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).resolve("vkm")
        destinationDir.mkdirs()
        return destinationDir
    }

    // download worker

    private var currentDownload: AtomicReference<Composition?> = AtomicReference(null)

    private fun downloadNext() {
        queue.poll()?.let { itemToDownload ->
            if (currentDownload.compareAndSet(null, itemToDownload)) {
                queue.remove(itemToDownload)
                if (itemToDownload.url.isEmpty()) {
                    "Track ${itemToDownload.fileName()} is not available for download, skipping".log()
                    // TODO search track on alternative sources
                    currentDownload.set(null)
                    downloadNext()
                } else {
                    inProgress.offer(itemToDownload)
                    launch(CommonPool) {
                        MusicService.trackMusicService.preprocess(itemToDownload)
                        val error = downloadTrack(itemToDownload)
                        if (error != null) {
                            "Error downloading file $error".logE()
                            finishDownload(itemToDownload, false)
                        } else {
                            finishDownload(itemToDownload)
                        }
                    }
                }
            } else {
                queue.offer(itemToDownload)
            }
        }
    }

    fun stopDownload() {
        currentDownload.get()?.let { composition ->
            queue.offer(composition)
            inProgress.remove(composition)
            currentDownload.set(null)
        }
    }

    private fun finishDownload(composition: Composition, wasDownloaded: Boolean = true) {
        val downloaded = currentDownload.get()
        "Finished downloading composition " + composition.artist + " ${composition.name}".log()
        if (currentDownload.compareAndSet(composition, null)) {
            inProgress.remove(downloaded)
            if (wasDownloaded) {
                downloadedList.offer(downloaded)
                composition.status = "downloaded"
                dao?.update(composition)
                downloadNext()
            }
        } else {
            "Parallel download of two tracks, Should not be like this!!!".logE()
        }
    }

    private suspend fun downloadTrack(vararg params: Composition): String? {
        val dir = getDownloadDir()
        val composition = params[0]
        val dest = dir.resolve(composition.fileName())
        try {
            val url = URL(composition.url)
            "Starting download track $url".log()
            val connection = url.openConnection()
            connection.connect()
            val totalBytes = connection.contentLength
            val out = ByteArrayOutputStream()
            connection.getInputStream().use {
                var bytesCopied: Long = 0
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = it.read(buffer)
                while (bytes >= 0) {
                    out.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    DownloadManager.downloadedPercent = (bytesCopied * 100 / totalBytes).toInt()
                    bytes = it.read(buffer)
                }
            }

            val bytes = out.toByteArray()
            composition.hash = bytes.md5()
            if (getDownloaded().find { it.hash.isNotEmpty() && it.hash == composition.hash } != null) {
                "File already was downloaded, skipping saving".log()
                return null
            }

            if (dir.canWrite() && dir.usableSpace > bytes.size) {
                try {
                    dest.writeBytes(bytes)
                    finishDownload(composition)
                } catch (e: Exception) {
                    Log.e("vkm", "Error saving track", e)
                    return "Error saving fie"
                }
            } else {
                "Not enough free space or unable to write to the $dir".logE()
            }
        } catch(e: Exception) {
            Log.e(this.toString(), "Error downloading track", e)
            return "Error downloading track"
        }
        return null
    }

    fun removeAllMusic() {
        launch(CommonPool) {
            getDownloadDir().deleteRecursively()
            dao?.deleteAllDownloaded()
        }
    }

    fun rehash() {
        launch(CommonPool) {
            lock.withLock {
                downloadedList.filter { it.hash.isEmpty() }.forEach {
                    it.hash = it.localFile()?.readBytes().md5()
                    dao?.update(it)
                }
            }
        }
    }

    suspend fun restoreDownloaded() {
        lock.withLock {
            clearDownloaded()
            val jobs = mutableListOf<Job>()
            getDownloadDir().listFiles().forEach { file ->
                 file.takeIf { it.isFile && it.extension == "mp3" }?.let {
                     val job = launch(CommonPool) {
                         val fileName = file.name.substringBeforeLast(".") // cutting .mp3
                         val data = fileName.replace('_', ' ').split('-')
                         val existingComposition = Composition(artist = data[0], name = data[1], hash = file.readBytes().md5())
                         if (existingComposition !in downloadedList ) { downloadedList.add(existingComposition) }
                     }
                     jobs.add(job)
                 }
            }
            jobs.forEach { it.join() }
            dao?.insertAll(downloadedList.toList())
            "Downloaded list restored with ${downloadedList.size} elements".log()
        }
    }
}
