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
        Log.v("vkm", "Loading all lists")
        loadAll()
    }

    fun loadAll() {
        loadList(downloaded, _downloadedList)
        loadList(queue, _queue)
        loadList(inProgress, _inProgress)
    }

    fun dumpAll() {
        Log.v("vkm", "Dumping all lists")
        dumpList(downloaded, getDownloaded())
        dumpList(queue, getQueue())
        dumpList(inProgress, getInProgress())
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

    fun dumpList(name: ListType, data: List<Composition> = listOf()) {
        val file = getListFileName(name)
        if (file.exists()) { file.delete() }

        file.bufferedWriter().use { writer ->
            data.forEach {
                writer.write(it.serialize())
                writer.newLine()
            }
        }
    }

    fun loadList(name: ListType, data: ConcurrentLinkedQueue<Composition>) {
        val file = getListFileName(name)

        if (file.exists()) {
            file.bufferedReader().use { reader ->
                data.forEach {
                    data.offer(reader.readLine().toComposition())
                }
            }
        }
    }

    private fun getListFileName(name: ListType): File {
        return when (name) {
            downloaded -> File(context?.filesDir, "downloadedList.json")
            queue -> File(context?.filesDir, "queue.json")
            inProgress -> File(context?.filesDir, "inProgress.json")
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
        _queue.peek()?.let { nextDownload ->
            if (currentDownload.compareAndSet(null, nextDownload)) {
                _queue.remove(nextDownload)
                _inProgress.offer(nextDownload)
                if (nextDownload.url.isEmpty()) {
                    Log.v("vkm", "Track is not available for download, skipping")
                    // TODO search track on alternative sources
                    downloaded(nextDownload)
                } else {
                    CompositionDownloadTask().execute(nextDownload)
                }
            }
        }
    }

    fun stopDownload(error: String) {
        currentDownload.get()?.let { composition ->
            _queue.offer(composition)
            _inProgress.remove(composition)
            currentDownload.set(null)
            // TODO error notification
        }
    }

    private fun downloaded(composition: Composition) {
        val downloaded = currentDownload.get()
        Log.v("vkm", "Finished downloading composition " + composition.artist + " " + composition.name)
        if (currentDownload.compareAndSet(composition, null)) {
            _downloadedList.offer(downloaded)
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
            val _url = URL(composition.url)

            val dest = dir.resolve("${composition.artist.trim().beginning(32).replace(' ', '_')}-${composition.name.trim().beginning(32).replace(' ', '_')}.mp3")
            if (dest.exists()) {
                Log.v("vkm", "File already exists, skipping download")
                downloaded(composition)
                return null
            }

            Log.v(this.toString(), "Starting download track $_url")
            val connection = _url.openConnection()
            connection.connect()

            try {
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
                if (getDownloaded().find { it.hash == composition.hash } != null) { return null }

                if (dir.canWrite() && dir.usableSpace > bytes.size) {
                    try {
                        dest.writeBytes(bytes)
                        downloaded(composition)
                    } catch (e: Exception) {
                        Log.e("vkm", "Error downloading track", e)
                        return "Error writing file " + e.message
                    }
                } else {
                    return "Not enough free space or unable to write to the $dir"
                }
            } catch(e: Exception) {
                Log.e(this.toString(), "Error downloading track", e)
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
}

enum class ListType {
    downloaded, queue, inProgress
}
