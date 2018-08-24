package vkm.vkm

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import vkm.vkm.ListType.*
import vkm.vkm.utils.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

object DownloadManager {

    // TODO this should be a background service, but i do not care now 8)

    val downloadedList = ConcurrentLinkedQueue<Composition>()
    private val _inProgress = ConcurrentLinkedQueue<Composition>()
    private val _queue = ConcurrentLinkedQueue<Composition>()
    var downloadedPercent = 0
    private val lock = Mutex()

    fun initialize() {
        "Loading all lists".log()
        loadAll()
    }

    fun loadAll() {
        loadList(downloaded, downloadedList)
        loadList(queue, _queue)
        loadList(inProgress, _inProgress)
    }

    fun dumpAll() {
        "Dumping all lists".log()
        dumpList(downloaded, getDownloaded())
        dumpList(queue, getQueue())
        dumpList(inProgress, getInProgress())
    }

    fun clearDownloaded() {
        downloadedList.clear()
        "Cleared downloaded list".log()
    }

    fun clearQueue() {
        _queue.clear()
        "Cleared queue list".log()
    }

    fun downloadComposition(composition: Composition?) {
        composition?.vkmId = System.currentTimeMillis()
        composition?.takeIf { it.url.isNotEmpty() }?.let { _queue.offer(composition) }
        downloadNext()
    }

    fun getQueue(): List<Composition> {
        return _queue.mapNotNull { it }
    }

    fun getDownloaded(): List<Composition> {
        return downloadedList.mapNotNull { it }.reversed()
    }

    fun getInProgress(): List<Composition> {
        return _inProgress.mapNotNull { it }
    }

    fun removeFromQueue(composition: Composition) {
        _queue.remove(_queue.find { it.id == composition.id })
    }

    @Synchronized
    private fun dumpList(name: ListType, data: List<Composition> = listOf()) {
        val timeStamp = System.currentTimeMillis()
        val file = getListFileName(name, timeStamp.toString())

        file.bufferedWriter().use { writer ->
            data.forEach {
                val serialize = it.serialize()
                writer.write(serialize)
                writer.newLine()
            }
        }

        file.copyTo(getListFileName(name), true)
        file.delete()
        "Dumping $name finished".log()
    }

    private fun loadList(name: ListType, data: ConcurrentLinkedQueue<Composition>) {
        val file = getListFileName(name)
        data.clear()

        file.takeIf { it.exists() && it.canRead() }?.bufferedReader()?.use { reader ->
            reader.readLines().forEach { line ->
                try {
                    data.offer(line.toComposition())
                } catch(e: Exception) {
                    Log.e("vkm", "Unable to parse composition, skipping")
                }
            }
            data.sortedByDescending { it.vkmId }
        }
    }

    private fun getListFileName(name: ListType, suffix: String = ""): File {
        return when (name) {
            downloaded -> File(getDownloadDir(), "downloadedList.json$suffix")
            queue -> File(getPropertiesDir(), "queue.json$suffix")
            inProgress -> File(getPropertiesDir(), "inProgress.json$suffix")
        }
    }

    fun getDownloadDir(): File {
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).resolve("vkm")
        destinationDir.mkdirs()
        return destinationDir
    }

    fun getPropertiesDir(): File {
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).resolve("vkm")
        destinationDir.mkdirs()
        return destinationDir
    }

    // download worker

    private var currentDownload: AtomicReference<Composition?> = AtomicReference(null)

    private fun downloadNext() {
        _queue.poll()?.let { itemToDownload ->
            if (currentDownload.compareAndSet(null, itemToDownload)) {
                _queue.remove(itemToDownload)
                if (itemToDownload.url.isEmpty()) {
                    "Track ${itemToDownload.fileName()} is not available for download, skipping".log()
                    // TODO search track on alternative sources
                    currentDownload.set(null)
                    downloadNext()
                } else {
                    _inProgress.offer(itemToDownload)
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
                _queue.offer(itemToDownload)
            }
        }
    }

    fun stopDownload() {
        currentDownload.get()?.let { composition ->
            _queue.offer(composition)
            _inProgress.remove(composition)
            currentDownload.set(null)
        }
    }

    private fun finishDownload(composition: Composition, wasDownloaded: Boolean = true) {
        val downloaded = currentDownload.get()
        "Finished downloading composition " + composition.artist + " ${composition.name}".log()
        if (currentDownload.compareAndSet(composition, null)) {
            _inProgress.remove(downloaded)
            dumpAll()
            if (wasDownloaded) {
                downloadedList.offer(downloaded)
                downloadNext()
            }
        } else {
            "Parallel download of two tracks, Should not be like this!!!".logE()
        }
    }

    private fun downloadTrack(vararg params: Composition): String? {
        val dir = getDownloadDir()
        val composition = params[0]
        if (!State.developerMode && getDownloaded().find { it.id == composition.id } != null) {
            "File already was downloaded, skipping download".log()
            return null
        }

        val dest = dir.resolve(composition.fileName())
        if (!State.developerMode && dest.exists()) {
            "File already exists, skipping download".log()
            return null
        }

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
                    if (!State.developerMode) { dest.writeBytes(bytes) }
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
        getDownloadDir().deleteRecursively()
    }

    fun rehashAndDump() {
        launch(CommonPool) {
            lock.withLock {
                downloadedList.filter { it.hash.isEmpty() }.forEach {
                    it.hash = it.localFile()?.readBytes().md5()
                }
                dumpList(downloaded, getDownloaded())
            }
        }
    }

    suspend fun restoreDownloaded() {
        lock.withLock {
            clearDownloaded()
            getDownloadDir().listFiles().forEach { file ->
                takeIf { file.isFile && file.extension == "mp3" }.let {
                    val fileName = file.name.beginning(file.name.length - 4) // cutting .mp3
                    val data = fileName.replace('_', ' ').split('-')
                    downloadedList.add(Composition(artist = data[0], name = data[1], hash = file.readBytes().md5()))
                }
            }
            dumpList(downloaded, getDownloaded())
        }
    }
}

enum class ListType {
    downloaded, queue, inProgress
}
