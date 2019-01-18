package vkm.vkm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.album_list_element.view.*
import vkm.vkm.R
import vkm.vkm.utils.Album
import vkm.vkm.utils.PictureDownloader
import vkm.vkm.utils.VkmFragment

class AlbumListAdapter(fragment: VkmFragment, resource: Int, data: List<Album>, private var elementClickListener: (album: Album, view: View) -> Unit? = { _, _ -> }) : ArrayAdapter<Album>(fragment.context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.album_list_element, null)
        val item = getItem(position)

        item?.let {
            view.name.text = it.name
            view.artist.text = it.artist

            view.action.setOnClickListener { v ->
                elementClickListener.invoke(item, v)
            }
            PictureDownloader.downloadAndSet(view?.cover, it.url)
        }

        return view
    }
}