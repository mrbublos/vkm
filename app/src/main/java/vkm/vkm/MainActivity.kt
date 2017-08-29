package vkm.vkm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = "vkm.vkm"
    }

    val EXTERNAL_STORAGE_WRITE_PERMISSION = 1

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

    private fun initialize() {
        SecurityService.context = applicationContext
        // importing local properties
        try {
            val localProperties = Properties()
            assets.open("myprops.properties").use { localProperties.load(it) }
            SecurityService.receipt = localProperties["receipt"] as String
        } catch (e: Exception) {}

        DownloadManager.initialize(applicationContext)

        if (SecurityService.isLoggedIn()) {
            startActivity(Intent(applicationContext, SearchActivity::class.java))
        } else {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v("vkm", "Dumping all lists")
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

