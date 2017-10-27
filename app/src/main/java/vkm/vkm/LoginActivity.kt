package vkm.vkm

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import vkm.vkm.utils.User
import vkm.vkm.utils.bind
import vkm.vkm.utils.log

class LoginActivity : AppCompatActivity() {

    val loginName by bind<EditText>(R.id.login)
    val password by bind<EditText>(R.id.password)
    val button by bind<Button>(R.id.login_button)
    val error by bind<TextView>(R.id.error)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        lockUnlockScreen(false)

        initializeButtons()
    }

    private fun initializeButtons() {

        button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                lockUnlockScreen(true)

                if (!areValuesValid()) {
                    lockUnlockScreen(false)
                    return@setOnTouchListener false
                }

                LoginPerformer(this).execute(loginName.text.toString(), password.text.toString())
            }

            return@setOnTouchListener false
        }
    }

    fun loginCallback(result: String) {
        when(result) {
            "ok" -> {
                startActivity(Intent(applicationContext, SearchFragment::class.java))
                finish()
                return
            }
            else -> {
                error.text = result
                lockUnlockScreen(false)
            }
        }
    }

    private fun areValuesValid(): Boolean {
        if (loginName.text.isEmpty() || password.text.isEmpty()) {
            error.text = getString(R.string.empty_password_login)
            return false
        }

        return true
    }

    private fun lockUnlockScreen(lock: Boolean) {
        loginName.isFocusable = !lock
        loginName.isClickable = !lock
        password.isFocusable = !lock
        password.isClickable = !lock
        button.isFocusable = !lock
        button.isClickable = !lock
    }
}

class LoginPerformer(val activity: LoginActivity): AsyncTask<String, Any, String>() {

    override fun onPostExecute(result: String) {
        "Login result $result".log()
        activity.loginCallback(result)
    }

    override fun doInBackground(vararg p0: String): String {
        return SecurityService.logIn(User(password = p0[1], userId = p0[0]))
    }

}