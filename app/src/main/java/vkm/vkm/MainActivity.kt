package vkm.vkm

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = "vkm.vkm"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SecurityService.context = applicationContext

        // importing local properties
        try {
            val localProperties = Properties()
            assets.open("myprops.properties").use { localProperties.load(it) }
            SecurityService.receipt = localProperties["receipt"] as String
        } catch (e: Exception) {}

        if (SecurityService.isLoggedIn()) {
            startActivity(Intent(applicationContext, SearchActivity::class.java))
            DownloadManager.initialize(applicationContext)
        } else {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DownloadManager.dumpAll()
    }
}

