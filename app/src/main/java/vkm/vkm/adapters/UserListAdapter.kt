package vkm.vkm.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.user_list_element.view.*
import vkm.vkm.R
import vkm.vkm.utils.PictureDownloader
import vkm.vkm.utils.User

class UserListAdapter(context: Context, resource: Int, data: List<User>, private var elementClickListener: (user: User?) -> Unit? = {}) : ArrayAdapter<User>(context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.user_list_element, null)
        val item = getItem(position)

        item?.let {
            view?.user_name?.text = item.fullname
            view?.user_id?.text = item.userId
            view?.setOnClickListener { elementClickListener.invoke(item) }

            PictureDownloader.downloadAndSet(view?.user_photo, item.photoUrl)
        }

        return view
    }
}
