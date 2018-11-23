package vkm.vkm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.SeekBar
import kotlinx.android.synthetic.main.pager_activity.*
import kotlinx.android.synthetic.main.pager_activity.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.Composition
import vkm.vkm.utils.HttpUtils
import vkm.vkm.utils.db.Db
import vkm.vkm.utils.equalsTo

class PagerActivity : AppCompatActivity(), ServiceConnection {

    var musicPlayer: MusicPlayService? = null

    private var displayedComposition: Composition? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            State.currentSearchTab = it.getInt("currentSearchTab")
            State.currentHistoryTab = it.getString("currentHistoryTab")
            launch(CommonPool) {
                val proxyDao = Db.instance(applicationContext).proxyDao()
                HttpUtils.setBlackList(proxyDao.getAll())
                DownloadManager.initialize(Db.instance(applicationContext).tracksDao())
            }
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

        // slow but simple
        val blackList = HttpUtils.getBlackList().filter { it.added > System.currentTimeMillis() - 1000 * 60 * 60 * 24 }
        launch(CommonPool) {
            val proxyDao = Db.instance(applicationContext).proxyDao()
            proxyDao.deleteAll()
            proxyDao.insertAll(blackList)
            HttpUtils.currentProxy?.let { proxyDao.insert(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
                musicPlayer?.play(musicPlayer?.currentComposition, this::onPlayingProgressUpdated)
            }
        }

        musicPlayer?.onPlay = onPlayerPlay
        musicPlayer?.onStop = onPlayerStop
        musicPlayer?.onPause = onPlayerPause


        trackPlayingProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) { return }

                val duration = musicPlayer?.trackLength ?: 0
                musicPlayer?.skipTo(progress * duration / 100)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun refreshList() {
        launch (UI) {
            ((findViewById<ListView>(R.id.resultList))?.adapter as BaseAdapter?)?.notifyDataSetChanged()
        }
    }

    private val onPlayerPlay: () -> Unit = {
        launch(UI) {
            currentTrackPlaying.visibility = View.VISIBLE
            pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_pause_player))
            refreshList()
        }
    }

    private val onPlayerStop: () -> Unit = {
        launch(UI) {
            currentTrackPlaying.visibility = View.VISIBLE
            pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_play_player))
            refreshList()
        }
    }

    private val onPlayerPause: () -> Unit = {
        launch(UI) {
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
        musicPlayer?.play(track, this::onPlayingProgressUpdated)
        refreshList()
    }

    private fun onPlayingProgressUpdated() {
        currentTrackPlaying.trackPlayingProgress.progress = musicPlayer?.trackProgress ?: 0
        val composition = musicPlayer?.currentComposition
        if (composition?.equalsTo(displayedComposition) != true) {
            displayedComposition = composition
            launch(UI) {
                currentTrackPlaying?.name?.text = composition?.name ?: ""
                currentTrackPlaying?.name?.isSelected = true
                currentTrackPlaying?.artist?.text = composition?.artist ?: ""
                currentTrackPlaying?.artist?.isSelected = true
                refreshList()
            }
        }
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
