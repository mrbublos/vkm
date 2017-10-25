package vkm.vkm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.pager_activity.*

class PagerActivity : AppCompatActivity() {

//    var classInCharge = listOf(SearchActivity(), HistoryActivity(), SettingsActivity())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pager_activity)
        pager.adapter = PagerAdapter(supportFragmentManager)
        pager.currentItem = 0
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
