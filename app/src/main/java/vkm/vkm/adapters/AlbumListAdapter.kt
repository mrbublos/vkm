package vkm.vkm.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import kotlinx.android.synthetic.main.album_list_element.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.R
import vkm.vkm.utils.*
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class AlbumListAdapter(fragment: VkmFragment, resource: Int, data: List<Album>, private var elementClickListener: (album: Album, view: View) -> Unit? = { _, _ -> }) : ArrayAdapter<Album>(fragment.context, resource, data) {

    private val photoCache: ConcurrentHashMap<String, Bitmap> = ConcurrentHashMap()
    private val limit = 1000

    private fun scheduleDownload(view: ImageView?, url: String) = launch(CommonPool) {
        val image = downloadPhoto(url)
        image?.let { launch(UI) { view?.setImageBitmap(image) } }
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.album_list_element, null)
        val item = getItem(position)

        item?.let {
            view.name.text = it.name
            view.artist.text = it.artist

            view.action.setOnClickListener { v ->
                elementClickListener.invoke(item, v)
            }
            scheduleDownload(view?.cover, it.url)
        }

        return view
    }

    private fun downloadPhoto(url: String): Bitmap? {
        if (photoCache.containsKey(url)) { return photoCache[url] }

        var result: Bitmap? = null

        try {
            val _url = URL(url)
            "Starting download $_url".log()
            val connection = _url.openConnection()
            connection.connect()
            val bytes = _url.openStream().use { it.readBytes() }
            result = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            photoCache[url] = result as Bitmap
            if (photoCache.size > limit) { photoCache.remove(photoCache.keys.any() as String) }
            "Download finished $_url".log()
        } catch (e: Exception) {
            "Error downloading photo".logE()
        }
        return result
    }
}