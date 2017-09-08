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
            val bind = view?.bind<ImageView>(R.id.imageView)
            var withAction = false

            if (context is SearchActivity) {
                if (item.url.trim().isEmpty()) {
                    withAction = false
                    bind?.setImageDrawable(context.getDrawable(R.drawable.ic_unavailable))
                } else {
                    DownloadManager.getDownloaded().find { it.uid() == item.uid() }?.let {
                        bind?.setImageDrawable(context.getDrawable(R.drawable.ic_downloaded))
                        withAction = true
                    }
                    DownloadManager.getQueue().find { it.uid() == item.uid() }?.let {
                        bind?.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
                        withAction = false
                    }
                    DownloadManager.getInProgress().find { it.uid() == item.uid() }?.let {
                        bind?.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
                        withAction = false
                    }
                }
            } else if (context is HistoryActivity) {
                bind?.setImageDrawable(context.getDrawable(android.R.drawable.ic_delete))
                withAction = true
            }

            // adding icon click listener
            bind?.takeIf { withAction }?.setOnClickListener { v ->
                elementClickListener.invoke(item, v)
            }
        }

        return view
    }
}