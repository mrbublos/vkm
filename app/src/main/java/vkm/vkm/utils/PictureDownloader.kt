package vkm.vkm.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.net.URL
import java.util.concurrent.ConcurrentHashMap


// TODO consider migrating to coroutines and channels
object PictureDownloader {

    private val photoCache: ConcurrentHashMap<String, Bitmap> = ConcurrentHashMap()
    private const val limit = 1000

    fun downloadAndSet(view: ImageView?, url: String) = launch(CommonPool) {
        val image = downloadPhoto(url)
        image?.let { launch(UI) { view?.setImageBitmap(image) } }
    }

    private fun downloadPhoto(url: String): Bitmap? {
        if (photoCache.containsKey(url)) { return photoCache[url] }

        var result: Bitmap? = null

        try {
            val _url = URL(url)
            val connection = _url.openConnection()
            connection.connect()
            val bytes = _url.openStream().use { it.readBytes() }
            result = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            photoCache[url] = result as Bitmap
            if (photoCache.size > limit) { photoCache.remove(photoCache.keys.any() as String) }
        } catch (e: Exception) {
            "Error downloading photo".logE()
        }
        return result
    }
}