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

        item?.let { item ->
            view?.bind<TextView>(R.id.name)?.text = item.name
            view?.bind<TextView>(R.id.artist)?.text = item.artist

            // determining icon to display
            val actionButton = view?.bind<ImageView>(R.id.imageView)
            var withAction = false
            val trackAvailable = item.url.trim().isNotEmpty()

            if (context is SearchActivity) {
                if (trackAvailable) {
                    withAction = true
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
                    }
                } else {
                    withAction = false
                    actionButton?.setImageDrawable(context.getDrawable(R.drawable.ic_unavailable))
                }
            } else if (context is HistoryActivity) {
                actionButton?.setImageDrawable(context.getDrawable(android.R.drawable.ic_delete))
                withAction = true
            }

            // adding icon click listener
            actionButton?.takeIf { withAction }?.setOnClickListener { v ->
                elementClickListener.invoke(item, v)
            }

            val audioControl = view?.bind<ImageView>(R.id.audioControl)

            if (trackAvailable) {
                audioControl?.setImageDrawable(context.getDrawable(R.drawable.ic_play))
                audioControl?.setOnClickListener { onPlayPressed(audioControl, item) }
            } else {
                audioControl?.setImageDrawable(context.getDrawable(R.drawable.ic_unavailable))
            }

        }

        return view
    }

    private fun onPlayPressed(audioControl: ImageView, item: Composition) {
        audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_stop))
        val onStop = { audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_play)) }
        val playerStarted = if (item.hash.isEmpty()) {
            // play from url
            MusicPlayer.play(item.url, onStop)
        } else {
            // play from disk
            MusicPlayer.play(item.fileName(), onStop)
        }
        if (!playerStarted) {
            onStop()
        } else {
            audioControl.setOnClickListener {
                MusicPlayer.stop()
                onStop()
                audioControl.setOnClickListener { onPlayPressed(audioControl, item) }
            }
        }
    }
}