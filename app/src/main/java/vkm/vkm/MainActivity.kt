package vkm.vkm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.net.URLDecoder
import java.util.*
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = "vkm.vkm"
    }

    private val EXTERNAL_STORAGE_WRITE_PERMISSION = 1
    var initialized = false
    var permissionContinuation: Continuation<IntArray>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // asking for writing permissions
        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        launch (UI) {
            "starting permissions".log()
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                suspendCoroutine<IntArray> {
                    permissionContinuation = it
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXTERNAL_STORAGE_WRITE_PERMISSION)
                }.takeIf { it.isNotEmpty() && it[0] != PackageManager.PERMISSION_GRANTED }?.let {
                    finish()
                    return@launch
                }
            }
            permissionContinuation = null
            "permissions received".log()
            initialize()
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
        val point = Point()
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
        if (initialized) {
            "Dumping all lists".log()
            DownloadManager.stopDownload("")
            DownloadManager.dumpAll()
        }
    }

    override fun onStop() {
        super.onStop()
        if (initialized) {
            "Dumping all lists".log()
            DownloadManager.stopDownload("")
            DownloadManager.dumpAll()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (EXTERNAL_STORAGE_WRITE_PERMISSION == requestCode) {
            permissionContinuation?.resume(grantResults)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}

