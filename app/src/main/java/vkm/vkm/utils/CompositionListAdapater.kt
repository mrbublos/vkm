package vkm.vkm.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
                if (MusicPlayer.isCurrentTrack(item)) {
                    audioControl?.apply {
                        setImageDrawable(context.getDrawable(R.drawable.ic_stop))
                        initSeekBar(seekBar)
                        setOnClickListener { onStopPressed(seekBar, audioControl, item) }
                    }
                } else {
                    audioControl?.setOnClickListener { onPlayPressed(audioControl, item) }
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

    private fun onStopPressed(seekBar: SeekBar?, audioControl: ImageView, item: Composition) {
        MusicPlayer.stop()
        hideSeekBar(seekBar)
        audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_play))
        audioControl.setOnClickListener { onPlayPressed(audioControl, item) }
    }

    private fun onPlayPressed(audioControl: ImageView, item: Composition) {
        MusicPlayer.stop(true)
        (MusicPlayer.currentSeekBar?.parent?.parent as View?)?.bind<View>(R.id.audioControl)?.callOnClick()

        // TODO loading icon
//        audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_loading))
//        ((audioControl.parent.parent as ListView).adapter as ArrayAdapter<Composition>).notifyDataSetChanged()

        val seekBar = (audioControl.parent as View).bind<SeekBar>(R.id.seekBar)
        val trackLength = MusicPlayer.play(if (item.hash.isEmpty()) item.url else item.fileName(), seekBar, { onStopPressed(seekBar, audioControl, item) })
        if (trackLength == 0) {
            audioControl.setImageDrawable(context.getDrawable(R.drawable.ic_stop))
            initSeekBar(seekBar)
            audioControl.setOnClickListener { onStopPressed(seekBar, audioControl, item) }
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