package vkm.vkm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.composition_list_element.view.*
import vkm.vkm.*
import vkm.vkm.utils.Composition
import vkm.vkm.utils.VkmFragment
import vkm.vkm.utils.equalsTo

class CompositionListAdapter(private val fragment: VkmFragment, resource: Int, val data: List<Composition>, private var elementClickListener: (composition: Composition, view: View) -> Unit? = { _, _ -> }) : ArrayAdapter<Composition>(fragment.context, resource, data) {

    private val activity = fragment.activity as PagerActivity

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.composition_list_element, null)
        val item = getItem(position)

        item?.let {
            view.name.text = item.name
            view.artist.text = item.artist

            // determining icon to display
            var withAction = false
            val trackAvailable = item.hash.isNotEmpty() || item.url.trim().isNotEmpty()

            val actionButton = view.imageView
            val audioControl = view.audioControl
            audioControl.visibility = View.VISIBLE

            if (trackAvailable) {
                when {
                    activity.musicPlayer?.isCurrentTrack(item) == true -> audioControl.apply {
                        setImageDrawable(context.getDrawable(R.drawable.ic_stop))
                        setOnClickListener { activity.musicPlayer?.stop() }
                    }
                    activity.musicPlayer?.isCurrentTrackLoading(item) == true -> audioControl.apply {
                        setImageDrawable(context.getDrawable(R.drawable.ic_loading))
                        setOnClickListener { activity.musicPlayer?.stop() }
                    }
                    else -> audioControl?.apply {
                        setOnClickListener { activity.playNewTrack(data, item) }
                        setImageDrawable(context.getDrawable(R.drawable.ic_play))
                    }
                }
            } else {
                audioControl?.setImageDrawable(context.getDrawable(R.drawable.ic_unavailable))
            }

            if (fragment is SearchFragment) {
                if (trackAvailable) {
                    withAction = true
                    actionButton?.setImageDrawable(context.getDrawable(R.drawable.ic_add))

                    DownloadManager.getDownloaded().find { it.equalsTo(item) }?.let {
                        actionButton?.setImageDrawable(context.getDrawable(R.drawable.ic_downloaded))
                    }
                    DownloadManager.getQueue().find { it.equalsTo(item) }?.let {
                        actionButton?.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
                        withAction = false
                    }
                    DownloadManager.getInProgress().find { it.equalsTo(item) }?.let {
                        actionButton?.setImageDrawable(context.getDrawable(R.drawable.ic_downloading))
                        withAction = false
                        audioControl?.visibility = View.INVISIBLE
                    }
                } else {
                    withAction = false
                    actionButton?.setImageDrawable(context.getDrawable(R.drawable.ic_unavailable))
                }
            } else if (fragment is HistoryFragment) {
                actionButton?.setImageDrawable(context.getDrawable(android.R.drawable.ic_delete))
                withAction = true
            }

            // adding icon click listener
            actionButton?.takeIf { withAction }?.setOnClickListener { v ->
                elementClickListener.invoke(item, v)
                this.notifyDataSetInvalidated()
            }
        }

        return view
    }
}