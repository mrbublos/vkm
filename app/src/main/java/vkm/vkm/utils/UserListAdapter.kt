package vkm.vkm.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.user_list_element.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.*
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class UserListAdapter(context: Context, resource: Int, data: List<User>, private var elementClickListener: (user: User?) -> Unit? = {}) : ArrayAdapter<User>(context, resource, data) {

    companion object {
        private val photoCache: ConcurrentHashMap<String, Bitmap> = ConcurrentHashMap()
        private val limit = 1000

        fun schedulePhotoDownload(view: ImageView?, user: User) = launch(CommonPool) {
            user.photo = downloadPhoto(user)
            user.photo?.let { launch(UI) { view?.setImageBitmap(user.photo) } }
        }

        suspend private fun downloadPhoto(user: User): Bitmap? {
            if (photoCache.containsKey(user.photoUrl)) { return photoCache[user.photoUrl] }

            var result: Bitmap? = null

            try {
                val _url = URL(user.photoUrl)
                "Starting download $_url".log()
                val connection = _url.openConnection()
                connection.connect()
                val bytes = _url.openStream().use { it.readBytes() }
                result = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                photoCache.put(user.photoUrl, result as Bitmap)
                if (photoCache.size > limit) { photoCache.remove(photoCache.keys.any() as String) }
                "Download finished $_url".log()
            } catch (e: Exception) {
                "Error downloading photo".logE()
            }
            return result
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.user_list_element, null)
        val item = getItem(position)

        item?.let {
            view?.user_name?.text = item.fullname
            view?.user_id?.text = item.userId
            view?.setOnClickListener { elementClickListener.invoke(item) }

            schedulePhotoDownload( view?.user_photo, item)
        }

        return view
    }
}
