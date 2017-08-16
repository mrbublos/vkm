package vkm.vkm

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String = "vkm.vkm"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SecurityService.context = applicationContext

        if (SecurityService.isLoggedIn(intent.extras.get("vkm_token") as String?)) {
            startActivity(Intent(applicationContext, SearchActivity::class.java))
        } else {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }
    }
}

