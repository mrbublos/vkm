package vkm.vkm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import vkm.vkm.utils.HttpUtils
import vkm.vkm.utils.Proxy
import vkm.vkm.utils.Swiper
import vkm.vkm.utils.db.Db
import vkm.vkm.utils.log
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    companion object {
        private const val EXTERNAL_STORAGE_WRITE_PERMISSION = 1
    }

    private var initialized = false
    private var permissionContinuation: Continuation<IntArray>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // asking for writing permissions
        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        GlobalScope.launch(Dispatchers.Main) {
            "starting permissions".log()
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                suspendCoroutine<IntArray> {
                    permissionContinuation = it
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXTERNAL_STORAGE_WRITE_PERMISSION)
                }.takeIf { it.isEmpty() || it[0] != PackageManager.PERMISSION_GRANTED }?.let {
                    finish()
                    return@launch
                }
            }
            permissionContinuation = null
            "permissions received".log()
            initialize()
            startActivity(Intent(applicationContext, PagerActivity::class.java))
            finish()
        }
    }

    private fun initialize() {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        Swiper.SWIPE_DISTANCE_MIN = Math.max(point.x  / 3, 200)
        loadProxy()
        DownloadManager.initialize(Db.instance(applicationContext).tracksDao())
        initialized = true
    }

    private fun loadProxy() {
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).resolve("vkm")
        destinationDir.mkdirs()
        val file = File(destinationDir, "proxy.txt")
        if (file.exists()) {
            val proxyData = file.readLines()[0].split(":")
            HttpUtils.currentProxy = Proxy(host = proxyData[0], port = proxyData[1].toInt(), speed = 0, type = "")
        }
    }

    private fun saveProxy() {
        val currentProxy = HttpUtils.currentProxy ?: return
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).resolve("vkm")
        destinationDir.mkdirs()
        File(destinationDir, "proxy.txt").writeText("${currentProxy.host}:${currentProxy.port}")
    }

    override fun onPause() {
        super.onPause()
        if (initialized) { DownloadManager.stopDownload() }
        saveProxy()
    }

    override fun onStop() {
        super.onStop()
        if (initialized) { DownloadManager.stopDownload() }
        saveProxy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (EXTERNAL_STORAGE_WRITE_PERMISSION == requestCode) {
            permissionContinuation?.resume(grantResults)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}

