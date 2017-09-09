package vkm.vkm.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SeekBar
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
            var withAction = false
            val trackAvailable = item.hash.isNotEmpty() || item.url.trim().isNotEmpty()

            val actionButton = view?.bind<ImageView>(R.id.imageView)
            val audioControl = view?.bind<ImageView>(R.id.audioControl)
            val seekBar = view?.bind<SeekBar>(R.id.seekBar)

            seekBar?.visibility = View.GONE

            if (trackAvailable) {
                audioControl?.setOnClickListener { onPlayPressed(audioControl, item) }
                if (item.url == MusicPlayer.resource) {
                    audioControl?.apply {
                        setImageDrawable(context.getDrawable(R.drawable.ic_stop))
                        initSeekBar(seekBar)
                    }
                } else {
                    audioControl?.setImageDrawable(context.getDrawable(R.drawable.ic_play))
                }
            } else {
                audioControl?.setImageDrawable(context.getDrawable(R.drawable.ic_unavailable))
            }

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
                        audioControl?.visibility = View.GONE
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
        }

        return view
    }

    private fun onPlayPressed(audioControl: ImageView, item: Composition) {
        MusicPlayer.stop(true)
        // TODO loading icon
//        audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_loading))
//        (audioControl.parent as View).invalidate()
        val onStop = { audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_play)) }
        val seekBar = (audioControl.parent as View).bind<SeekBar>(R.id.seekBar)
        val trackLength = if (item.hash.isEmpty()) {
            // play from url
            MusicPlayer.play(item.url, seekBar, onStop)
        } else {
            // play from disk
            MusicPlayer.play(item.fileName(), seekBar, onStop)
        }
        if (trackLength == 0) {
            onStop()
        } else {
            audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_stop))
            initSeekBar(seekBar)
            audioControl.setOnClickListener {
                MusicPlayer.stop()
                hideSeekBar(seekBar)
                onStop()
                audioControl.setOnClickListener { onPlayPressed(audioControl, item) }
            }
        }
    }

    private fun hideSeekBar(seekBar: SeekBar?) {
        seekBar?.let {
            seekBar.visibility = View.GONE
            (seekBar.parent?.parent as View?)?.invalidate()
        }
    }

    private fun initSeekBar(seekBar: SeekBar?) {
        seekBar?.apply {
            visibility = View.VISIBLE
            setOnSeekBarChangeListener(MusicPlayer)
            max = MusicPlayer.trackLength / 1000
        }
    }
}