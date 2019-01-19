package vkm.vkm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.pager_activity.*
import kotlinx.android.synthetic.main.pager_activity.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import vkm.vkm.utils.Composition
import vkm.vkm.utils.HttpUtils
import vkm.vkm.utils.db.Db

class PagerActivity : AppCompatActivity(), ServiceConnection {

    var musicPlayer: MusicPlayService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HttpUtils.loadProxies(Db.instance(applicationContext).proxyDao())
        DownloadManager.initialize(Db.instance(applicationContext).tracksDao())

        savedInstanceState?.let {
            State.currentSearchTab = it.getInt("currentSearchTab")
            State.currentHistoryTab = it.getString("currentHistoryTab") ?: ""
        }

        setContentView(R.layout.pager_activity)
        pager.adapter = PagerAdapter(supportFragmentManager)
        pager.currentItem = 0

        bindService(Intent(applicationContext, MusicPlayService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("currentSearchTab", State.currentSearchTab)
        outState?.putString("currentHistoryTab", State.currentHistoryTab)

        HttpUtils.storeProxies(Db.instance(applicationContext).proxyDao())
    }

    override fun onDestroy() {
        super.onDestroy()
        HttpUtils.storeProxies(Db.instance(applicationContext).proxyDao())
        unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        musicPlayer = (service as MusicPlayService.MusicPlayerController).getService()
        setupMusicService()
    }

    private fun setupMusicService() {
        nextTrack.setOnClickListener { musicPlayer?.next() }
        pause.setOnClickListener {
            if (musicPlayer?.isPlaying() == true) {
                musicPlayer?.pause()
            } else {
                musicPlayer?.play(musicPlayer?.displayedComposition?.value)
            }
        }

        trackPlayingProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) { return }

                val duration = musicPlayer?.trackLength ?: 0
                musicPlayer?.skipTo((progress * duration / 100).toInt())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        musicPlayer?.state?.observe(this, Observer { state ->
            when (state) {
                "playing" -> onPlayerPlay()
                "stopped" -> onPlayerStop()
                "paused" -> onPlayerPause()
                else -> refreshList()
            }
        })

        musicPlayer?.progress?.observe(this, Observer {
            GlobalScope.launch(Dispatchers.Main) {
                currentTrackPlaying.trackPlayingProgress.progress = it.toInt()
            }
        })

        musicPlayer?.displayedComposition?.observe(this, Observer {
            GlobalScope.launch(Dispatchers.Main) {
                currentTrackPlaying?.name?.text = it.name
                currentTrackPlaying?.name?.isSelected = true
                currentTrackPlaying?.artist?.text = it.artist
                currentTrackPlaying?.artist?.isSelected = true
            }
        })
    }

    private fun refreshList() {
        GlobalScope.launch(Dispatchers.Main) {
            ((findViewById<ListView>(R.id.resultList))?.adapter as BaseAdapter?)?.notifyDataSetChanged()
        }
    }

    private val onPlayerPlay: () -> Unit = {
        GlobalScope.launch(Dispatchers.Main) {
            currentTrackPlaying.visibility = View.VISIBLE
            pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_pause_player))
            refreshList()
        }
    }

    private val onPlayerStop: () -> Unit = {
        GlobalScope.launch(Dispatchers.Main) {
            currentTrackPlaying.visibility = View.VISIBLE
            pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_play_player))
            refreshList()
        }
    }

    private val onPlayerPause: () -> Unit = {
        GlobalScope.launch(Dispatchers.Main) {
            currentTrackPlaying.visibility = View.VISIBLE
            pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_play_player))
            refreshList()
        }
    }

    fun playNewTrack(list: List<Composition>, track: Composition) {
        musicPlayer?.stop()
        val newPlayList = mutableListOf<Composition>()
        newPlayList.addAll(list)
        musicPlayer?.playList = newPlayList
        musicPlayer?.play(track)
        refreshList()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayer = null
    }
}

class PagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

    // Order: Search, History, Settings
    override fun getItem(position: Int): Fragment {
        return when (position % 3) {
            1 -> HistoryFragment()
            2 -> SettingsFragment()
            else -> SearchFragment()
        }
    }

    override fun getCount(): Int = 3
}
