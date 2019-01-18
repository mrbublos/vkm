package vkm.vkm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import vkm.vkm.utils.Artist
import vkm.vkm.utils.PictureDownloader
import vkm.vkm.utils.VkmFragment

class ArtistListAdapter(fragment: VkmFragment, resource: Int, data: List<Artist>, private var elementClickListener: (album: Artist, view: View) -> Unit? = { _, _ -> }) : ArrayAdapter<Artist>(fragment.context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.artist_list_element, null)
        val item = getItem(position)

        item?.let {
            view.name.text = it.name

            view.action.setOnClickListener { v ->
                elementClickListener.invoke(item, v)
            }
            PictureDownloader.downloadAndSet(view.cover, it.url)
        }

        return view
    }
}