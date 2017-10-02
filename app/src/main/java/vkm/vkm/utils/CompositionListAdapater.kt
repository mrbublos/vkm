package vkm.vkm.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.composition_list_element.view.*
import vkm.vkm.*

class CompositionListAdapter(context: Context, resource: Int, data: List<Composition>, private var elementClickListener: (composition: Composition, view: View) -> Unit? = { _, _ -> }) : ArrayAdapter<Composition>(context, resource, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.composition_list_element, null)
        val item = getItem(position)

        item?.let {
            view.name.text = item.name
            view.artist.text = item.artist

            // determining icon to display
            var withAction = false
            val trackAvailable = item.hash.isNotEmpty() || item.url.trim().isNotEmpty()

            val actionButton = view?.imageView
            val audioControl = view?.audioControl
            val seekBar = view?.seekBar

            seekBar?.visibility = View.GONE

            if (trackAvailable) {
                if (MusicPlayer.isCurrentTrack(item)) {
                    initSeekBar(view)
                    audioControl?.apply {
                        if (MusicPlayer.isLoading) {
                            setImageDrawable(context.getDrawable(R.drawable.ic_loading))
                        } else {
                            setImageDrawable(context.getDrawable(R.drawable.ic_stop))
                            MusicPlayer.takeIf { it.currentlyAnimatedView != view }?.apply {
                                "Restarting slider updater".log()
                                currentlyAnimatedView = view
                                runSeekBarUpdate(view)
                            }
                        }
                        setOnClickListener { onStopPressed(view, item) }
                    }
                } else {
                    audioControl?.apply {
                        setOnClickListener { onPlayPressed(view, item) }
                        setImageDrawable(context.getDrawable(R.drawable.ic_play))
                    }
                }
            } else {
                audioControl?.setImageDrawable(context.getDrawable(R.drawable.ic_unavailable))
            }

            if (context is SearchActivity) {
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

    private fun onStopPressed(animatedView: View?, item: Composition) {
        MusicPlayer.stop()
        hideSeekBar(animatedView)
        animatedView?.audioControl?.apply {
            setImageDrawable(context.getDrawable(R.drawable.ic_play))
            setOnClickListener { onPlayPressed(this, item) }
        }
    }

    private fun onPlayPressed(viewToAnimate: View?, item: Composition) {
        // stopping previously played, if exists
        MusicPlayer.currentlyAnimatedView?.audioControl?.callOnClick()

        viewToAnimate?.audioControl?.setImageDrawable(context.getDrawable(R.drawable.ic_loading))

        MusicPlayer.play(if (item.hash.isEmpty()) item.url else item.fileName(), viewToAnimate, { onStopPressed(viewToAnimate, item) }, {
            initSeekBar(MusicPlayer.currentlyAnimatedView)
            viewToAnimate?.audioControl?.apply {
                setImageDrawable(context.getDrawable(R.drawable.ic_stop))
                setOnClickListener { onStopPressed(viewToAnimate, item) }
            }
        })
    }

    private fun hideSeekBar(view: View?) {
        view?.seekBar?.apply {
            visibility = View.GONE
            view.invalidate()
        }
    }

    private fun initSeekBar(view: View?) {
        view?.seekBar?.apply {
            visibility = View.VISIBLE
            setOnSeekBarChangeListener(MusicPlayer)
            max = MusicPlayer.trackLength / 1000
        }
    }
}