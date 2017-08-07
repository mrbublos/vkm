package vkm.vkm.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.os.AsyncTask
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

class UserListAdapter(context: Context, resource: Int, data: List<User>) : ArrayAdapter<User>(context, resource, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var view = convertView

        if (view == null) { view = LayoutInflater.from(context).inflate(R.layout.user_list_element, null) }

        val item = getItem(position)

        item?.let {
            view?.bind<TextView>(R.id.name)?.text = item.fullname
            view?.bind<TextView>(R.id.artist)?.text = item.userId
            AsyncDownloader().execute(item.photoUrl, view?.bind<ImageView>(R.id.photo))
            // TODO download photo and display
        }

        return view
    }

}

class AsyncDownloader: AsyncTask<Any, Unit, Unit>() {


    override fun doInBackground(vararg input: Any?) {
        downloadPhotoAndSet(input[0] as String, input[1] as ImageView)
    }

    // maybe do it in a background
    fun downloadPhotoAndSet(url: String, view: ImageView?) {
        val _url = URL(url)
        val connection = _url.openConnection()
        connection.connect()

        val out = ByteArrayOutputStream()
        BufferedInputStream(_url.openStream()).copyTo(out)

        val options = BitmapFactory.Options()
        options.outHeight = 100
        options.outWidth = 100

        view?.setImageBitmap(BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size(), options))
    }

}
