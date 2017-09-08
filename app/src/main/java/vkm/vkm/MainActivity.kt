package vkm.vkm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.net.URLDecoder
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = "vkm.vkm"
    }

    val EXTERNAL_STORAGE_WRITE_PERMISSION = 1
    var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // asking for writing permissions
        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            initialize()
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXTERNAL_STORAGE_WRITE_PERMISSION)
        }
    }

    override fun onStart() {
        super.onStart()
        if (initialized) {
            DownloadManager.initialize(applicationContext)
            DownloadManager.downloadComposition(null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (initialized) {
            DownloadManager.initialize(applicationContext)
            DownloadManager.downloadComposition(null)
        }
    }

    private fun initialize() {
        SecurityService.context = applicationContext
        var point = Point()
        windowManager.defaultDisplay.getSize(point)
        SwipeCatcher.SWIPE_DISTANCE_MIN = Math.max(point.x  / 3, 200)

        // importing local properties
        try {
            val localProperties = Properties()
            assets.open("myprops.properties").use { localProperties.load(it) }
            SecurityService.receipt = URLDecoder.decode(localProperties["receipt"] as String, "UTF-8")
        } catch (e: Exception) {}

        DownloadManager.initialize(applicationContext)

        if (SecurityService.isLoggedIn()) {
            startActivity(Intent(applicationContext, SearchActivity::class.java))
        } else {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }
        initialized = true
    }

    override fun onPause() {
        super.onPause()
        "Dumping all lists".log()
        DownloadManager.stopDownload("")
        DownloadManager.dumpAll()
    }

    override fun onStop() {
        super.onStop()
        "Dumping all lists".log()
        DownloadManager.stopDownload("")
        DownloadManager.dumpAll()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (EXTERNAL_STORAGE_WRITE_PERMISSION == requestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initialize()
            } else {
                finish()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}

