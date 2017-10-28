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
import android.view.Window
import android.widget.SeekBar
import kotlinx.android.synthetic.main.pager_activity.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.Composition

class PagerActivity : AppCompatActivity(), ServiceConnection {

    var musicPlayer: MusicPlayService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pager_activity)
        pager.adapter = PagerAdapter(supportFragmentManager)
        pager.currentItem = 0

        bindService(Intent(applicationContext, MusicPlayService::class.java), this, Context.BIND_AUTO_CREATE)
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
        previousTrack.setOnClickListener { musicPlayer?.previous() }
        nextTrack.setOnClickListener { musicPlayer?.next() }
        stop.setOnClickListener {
            musicPlayer?.stop()
            pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_play_player))
        }
        pause.setOnClickListener {
            if (musicPlayer?.isPlaying() == true) {
                musicPlayer?.pause()
                pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_play_player))
            } else {
                musicPlayer?.play(this::onPlayingProgressUpdated)
                pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_pause_player))
            }
        }
        musicPlayer?.onPlay = {
            launch(UI) {
                currentTrackPlaying.visibility = View.VISIBLE
                pause.setImageDrawable(applicationContext.getDrawable(R.drawable.ic_pause_player) )
            }
        }
        trackPlayingProgress.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = musicPlayer?.trackLength ?: 0
                musicPlayer?.skipTo(progress / 100 * duration)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun playNewTrack(list: List<Composition>, track: Composition) {
        musicPlayer?.stop()
        musicPlayer?.currentComposition = track
        val newPlayList = mutableListOf<Composition>()
        newPlayList.addAll(list)
        musicPlayer?.playList = newPlayList
        musicPlayer?.play(this::onPlayingProgressUpdated)
    }

    private fun onPlayingProgressUpdated() {
        trackPlayingProgress.progress = musicPlayer?.trackProgress ?: 0
        val composition = musicPlayer?.currentComposition
        launch(UI) {
            name.text = composition?.name ?: ""
            artist.text = composition?.artist ?: ""
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
            0 -> SearchFragment()
            1 -> HistoryFragment()
            2 -> SettingsFragment()
            else -> SearchFragment()
        }
    }

    override fun getCount(): Int = 3
}
