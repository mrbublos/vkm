package vkm.vkm

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import vkm.vkm.ListType.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

object DownloadManager {

    // TODO this should be a background service

    val _downloadedList = ConcurrentLinkedQueue<Composition>()
    val _inProgress = ConcurrentLinkedQueue<Composition>()
    val _queue = ConcurrentLinkedQueue<Composition>()
    var downloadedPercent = 0

    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context
        "Loading all lists".log()
        loadAll()
    }

    fun loadAll() {
        loadList(downloaded, _downloadedList)
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
        _downloadedList.clear()
        "Cleared downloaded list".log()
    }

    fun clearQueue() {
        _queue.clear()
        "Cleared queue list".log()
    }

    fun downloadComposition(composition: Composition?) {
        composition?.let { _queue.offer(composition) }
        downloadNext()
    }

    fun getQueue(): List<Composition> {
        return _queue.mapNotNull { it }
    }

    fun getDownloaded(): List<Composition> {
        return _downloadedList.mapNotNull { it }
    }

    fun getInProgress(): List<Composition> {
        return _inProgress.mapNotNull { it }
    }

    fun removeFromQueue(composition: Composition) {
        _queue.remove(_queue.find { it.id == composition.id })
    }

    @Synchronized
    private fun dumpList(name: ListType, data: List<Composition> = listOf()) {
        val file = getListFileName(name)
        if (file.exists()) { file.delete() }

        file.bufferedWriter().use { writer ->
            data.forEach {
                val serialize = it.serialize()
                writer.write(serialize)
                "Serializing composition ${serialize}".log()
                writer.newLine()
            }
        }
    }

    private fun loadList(name: ListType, data: ConcurrentLinkedQueue<Composition>) {
        val file = getListFileName(name)

        data.clear()

        if (file.exists()) {
            file.bufferedReader().use { reader ->
                reader.readLines().forEach { line ->
                    try {
                        data.offer(line.toComposition())
                    } catch(e: Exception) {
                        Log.e("vkm", "Unable to parse composition, skipping")
                    }
                }
            }
        }
    }

    private fun getListFileName(name: ListType): File {
        return when (name) {
            downloaded -> File(getPropertiesDir(), "downloadedList.json")
            queue -> File(getPropertiesDir(), "queue.json")
            inProgress -> File(getPropertiesDir(), "inProgress.json")
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
        _queue.peek()?.let { itemToDownload ->
            if (currentDownload.compareAndSet(null, itemToDownload)) {
                _queue.remove(itemToDownload)
                _inProgress.offer(itemToDownload)
                if (itemToDownload.url.isEmpty()) {
                    "Track is not available for download, skipping".log()
                    // TODO search track on alternative sources
                    finishDownload(itemToDownload, false)
                } else {
                    CompositionDownloadTask().execute(itemToDownload)
                }
            }
        }
    }

    fun stopDownload(error: String) {
        currentDownload.get()?.let { composition ->
            _queue.offer(composition)
            _inProgress.remove(composition)
            currentDownload.set(null)
            error.toast(context)
        }
    }

    private fun finishDownload(composition: Composition, wasDownloaded: Boolean = true) {
        val downloaded = currentDownload.get()
        "Finished downloading composition " + composition.artist + " ${composition.name}".log()
        if (currentDownload.compareAndSet(composition, null)) {
            if (wasDownloaded) { _downloadedList.offer(downloaded) }
            _inProgress.remove(downloaded)
            dumpAll()
            downloadNext()
        } else {
            Log.e("vkm", "Parallel download of two tracks, Should not be like this!!!")
        }
    }

    class CompositionDownloadTask: AsyncTask<Composition, Long, String?>() {
        private val dir = getDownloadDir()

        override fun doInBackground(vararg params: Composition): String? {
            val composition = params[0]
            if (getDownloaded().find { it.id == composition.id } != null) {
                "File already was downloaded, skipping download".log()
                finishDownload(composition)
                return null
            }

            val dest = dir.resolve(composition.fileName())
            if (dest.exists()) {
                "File already exists, skipping download".log()
                finishDownload(composition)
                return null
            }

            try {
                val _url = URL(composition.url)
                "Starting download track $_url".log()
                val connection = _url.openConnection()
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
                        publishProgress(bytesCopied * 100 / totalBytes)
                        bytes = it.read(buffer)
                    }
                }

                val bytes = out.toByteArray()
                composition.hash = bytes.md5()
                if (getDownloaded().find { it.hash.isNotEmpty() && it.hash == composition.hash } != null) {
                    "File already was downloaded, skipping saving".log()
                    finishDownload(composition)
                    return null
                }

                if (dir.canWrite() && dir.usableSpace > bytes.size) {
                    try {
                        dest.writeBytes(bytes)
                        finishDownload(composition)
                    } catch (e: Exception) {
                        Log.e("vkm", "Error saving track", e)
                        finishDownload(composition, false)
                        return null
                    }
                } else {
                    return "Not enough free space or unable to write to the $dir"
                }
            } catch(e: Exception) {
                Log.e(this.toString(), "Error downloading track", e)
                finishDownload(composition, false)
            }
            return null
        }

        override fun onPostExecute(error: String?) {
            error?.let {
                Log.e("vkm", "Error downloading file (skipping)" + error)
                stopDownload(error)
            }
        }

        override fun onProgressUpdate(vararg values: Long?) {
            DownloadManager.downloadedPercent = values[0]?.toInt() ?: 0
        }
    }

    fun removeAllMusic() {
        getDownloadDir().deleteRecursively()
    }

    @Synchronized
    fun rehashAndDump() {
        _downloadedList.filter { it.hash.isEmpty() }.forEach {
            it.hash = it.localFile()?.readBytes().md5()
        }
        dumpList(downloaded, getDownloaded())
    }
}

enum class ListType {
    downloaded, queue, inProgress
}
