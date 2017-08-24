package vkm.vkm

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import vkm.vkm.ListType.*
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

object DownloadManager {

    val _downloadedList = ConcurrentLinkedQueue<Composition>()
    val _inProgress = ConcurrentLinkedQueue<Composition>()
    val _queue = ConcurrentLinkedQueue<Composition>()
    var context: Context? = null

    fun initialize(context: Context) {
        this.context = context
        Log.v("vkm", "Loading all lists")
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

    fun downloadComposition(composition: Composition) {
        _queue.offer(composition)
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
        return context?.filesDir!!
    }

    // download worker

    private var currentDownload: AtomicReference<Composition?> = AtomicReference(null)

    private fun downloadNext() {
        val nextDownload = DownloadManager._queue.peek()
        if (currentDownload.compareAndSet(null, nextDownload)) {
            DownloadManager._queue.remove(nextDownload)
            _inProgress.offer(nextDownload)
            CompositionDownloadTask().execute(nextDownload)
        }
    }

    private fun stopDownload(error: String) {
        val composition = currentDownload.get()
        _queue.offer(composition)
        _inProgress.remove(composition)
        currentDownload.set(null)
        // TODO error notification
    }

    private fun downloaded(composition: Composition) {
        val downloaded = currentDownload.get()
        if (currentDownload.compareAndSet(composition, null)) {
            _downloadedList.offer(downloaded)
            _inProgress.remove(downloaded)
            currentDownload.set(null)
            downloadNext()
        } else {
            Log.e("vkm", "Parallel download of two tracks, Should not be like this!!!")
        }
    }

    class CompositionDownloadTask: AsyncTask<Composition, Int, String?>() {
        private val dir = getDownloadDir()

        override fun doInBackground(vararg params: Composition): String? {
            val composition = params[0]
            val _url = URL(composition.url)
            Log.v(this.toString(), "Starting download track $_url")
            val connection = _url.openConnection()
            connection.connect()

            val out = ByteArrayOutputStream()

            try {
                BufferedInputStream(_url.openStream()).copyTo(out)
                val bytes = out.toByteArray()
                composition.hash = bytes.md5()
                if (getDownloaded().find { it.hash == composition.hash } != null) { return null }

                if (dir.canWrite() && dir.usableSpace > bytes.size) {
                    val dest = dir.resolve("${composition.artist}-${composition.name}.mp3")
                    // TODO check that file not exist already (otherwise it will be overwritten)
                    dest.writeBytes(bytes)
                    downloaded(composition)
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
                stopDownload(error)
            }
        }
    }
}

enum class ListType {
    downloaded, queue, inProgress
}
