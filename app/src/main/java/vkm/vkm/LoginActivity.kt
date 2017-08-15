package vkm.vkm

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

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

    fun initializeButtons() {

        button.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                lockUnlockScreen(true)

                if (!areValuesValid()) {
                    lockUnlockScreen(false)
                    return@setOnTouchListener view.onTouchEvent(event)
                }

                LoginPerformer(this).execute(loginName.text.toString(), password.text.toString())
            }

            return@setOnTouchListener view.onTouchEvent(event)
        }
    }

    fun loginCallback(result: String) {
        when(result) {
            "ok" -> {
                startActivity(Intent(applicationContext, SearchActivity::class.java))
                finish()
                return
            }
            else -> {
                error.text = result
                lockUnlockScreen(false)
            }
        }
    }

    fun areValuesValid(): Boolean {
        if (loginName.text.isEmpty() || password.text.isEmpty()) {
            error.text = getString(R.string.empty_password_login)
            return false
        }

        return true
    }

    fun lockUnlockScreen(lock: Boolean) {
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
        Log.v(this.toString(), "Login result $result")
        activity.loginCallback(result)
    }

    override fun doInBackground(vararg p0: String): String {
        return SecurityService.logIn(User(password = p0[1], userId = p0[0]))
    }

}