package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

data class User(var userId: String, var password: String)

class LoginActivity : AppCompatActivity() {

    var _user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

}
