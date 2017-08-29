package vkm.vkm.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import vkm.vkm.R
import vkm.vkm.User
import vkm.vkm.bind
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class UserListAdapter(context: Context, resource: Int, data: List<User>, private var elementClickListener: (user: User?) -> Unit? = {}) : ArrayAdapter<User>(context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var view = convertView

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.user_list_element, null)
        }

        val item = getItem(position)

        item?.let {
            view?.bind<TextView>(R.id.user_name)?.text = item.fullname
            view?.bind<TextView>(R.id.user_id)?.text = item.userId
            AsyncPhotoDownloader().execute(item, view?.bind<ImageView>(R.id.user_photo))
            view?.setOnClickListener {
                elementClickListener.invoke(item)
                return@setOnClickListener
            }
        }

        return view
    }

}


class AsyncPhotoDownloader : AsyncTask<Any, Unit, Pair<ImageView, User>>() {

    companion object {
        val cache: ConcurrentHashMap<String, Bitmap> = ConcurrentHashMap()
        val limit = 1000
    }

    override fun onPostExecute(result: Pair<ImageView, User>) {
        val (view, user) = result
        user.photo?.let { view.setImageBitmap(user.photo) }
        Log.v(this.toString(), "Bitmap set")
    }


    override fun doInBackground(vararg input: Any?): Pair<ImageView, User> {
        val user = input[0] as User
        if (cache.containsKey(user.photoUrl)) {
            user.photo = cache[user.photoUrl]
            return Pair(input[1] as ImageView, user)
        }

        user.photoUrl?.let {
            val _url = URL(user.photoUrl)
            Log.v(this.toString(), "Starting download $_url")
            val connection = _url.openConnection()
            connection.connect()

            val out = ByteArrayOutputStream()

            try {
                _url.openStream().use { it.copyTo(out) }
                user.photo = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
                cache.put(user.photoUrl, user.photo as Bitmap)
                if (cache.size > limit) {
                    cache.remove(cache.keys.any() as String)
                }
            } catch (e: Exception) {
                Log.e(this.toString(), "Error downloading image", e)
            }

            Log.v(this.toString(), "Download finished $_url")
        }
        return Pair(input[1] as ImageView, user)
    }
}
