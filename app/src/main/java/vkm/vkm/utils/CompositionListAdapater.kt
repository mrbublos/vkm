package vkm.vkm.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import vkm.vkm.*

class CompositionListAdapter(context: Context, resource: Int, data: List<Composition>, private var elementClickListener: (composition: Composition, view: View) -> Unit? = { _, _ -> }) : ArrayAdapter<Composition>(context, resource, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var view = convertView

        if (view == null) { view = LayoutInflater.from(context).inflate(R.layout.composition_list_element, null) }

        val item = getItem(position)

        item?.let {
            view?.bind<TextView>(R.id.name)?.text = item.name
            view?.bind<TextView>(R.id.artist)?.text = item.artist

            // determining icon to display
            if (context is SearchActivity) {
                DownloadManager.getDownloaded().find { it.id == item.id }?.let {
                    view?.bind<ImageView>(R.id.imageView)?.setImageDrawable(context.getDrawable(R.drawable.ic_downloaded))
                }
                DownloadManager.getQueue().find { it.id == item.id }?.let {
                    view?.bind<ImageView>(R.id.imageView)?.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
                }
                DownloadManager.getInProgress().find { it.id == item.id }?.let {
                    view?.bind<ImageView>(R.id.imageView)?.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
                }
            } else if (context is HistoryActivity) {
                view?.bind<ImageView>(R.id.imageView)?.setImageDrawable(context.getDrawable(android.R.drawable.ic_delete))
            }

            // adding icon click listener
            view?.bind<ImageView>(R.id.imageView)?.setOnClickListener { v ->
                elementClickListener.invoke(item, v)
            }
        }

        return view
    }
}