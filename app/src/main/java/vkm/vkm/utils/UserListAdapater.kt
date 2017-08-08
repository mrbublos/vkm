package vkm.vkm.utils

import android.content.Context
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
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class UserListAdapter(context: Context, resource: Int, data: List<User>, var elementTouchListener: (user: User?) -> Unit? = {}) : ArrayAdapter<User>(context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var view = convertView

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.user_list_element, null)
        }

        val item = getItem(position)

        item?.let {
            view?.bind<TextView>(R.id.name)?.text = item.fullname
            view?.bind<TextView>(R.id.artist)?.text = item.userId
            AsyncPhotoDownloader().execute(item, view?.bind<ImageView>(R.id.photo))
            view?.setOnTouchListener { v, event ->
                elementTouchListener.invoke(item)
                return@setOnTouchListener v.onTouchEvent(event)
            }
        }

        return view
    }

}


class AsyncPhotoDownloader : AsyncTask<Any, Unit, Triple<ImageView, ByteArray, User>>() {

    companion object {
        val cache: ConcurrentHashMap<String, ByteArray> = ConcurrentHashMap()
        val limit = 1000
    }

    override fun onPostExecute(result: Triple<ImageView, ByteArray, User>) {
        val (view, out, user) = result

        val options = BitmapFactory.Options()
        options.outHeight = 10
//        options.outWidth = 100

        user.photo = BitmapFactory.decodeByteArray(out, 0, out.size, options)
        view.setImageBitmap(user.photo)
        Log.v(this.toString(), "Bitmap set")
    }


    override fun doInBackground(vararg input: Any?): Triple<ImageView, ByteArray, User> {
        val user = input[0] as User
        if (cache.containsKey(user.photoUrl)) {
            return Triple(input[1] as ImageView, cache[user.photoUrl] as ByteArray, user)
        }

        val _url = URL(user.photoUrl)
        Log.v(this.toString(), "Starting download $_url")
        val connection = _url.openConnection()
        connection.connect()

        val out = ByteArrayOutputStream()
        try {
            BufferedInputStream(_url.openStream()).copyTo(out)
            cache.put(user.photoUrl, out.toByteArray())
            if (cache.size > limit) {
                cache.remove(cache.keys.any() as String)
            }
        } catch(e: Exception) {
            Log.e(this.toString(), "Error downloading image", e)
        }
        Log.v(this.toString(), "Download finished $_url")
        return Triple(input[1] as ImageView, out.toByteArray(), user)
    }
}
