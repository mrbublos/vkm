package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.VkApi

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        lockUnlockScreen(false)

        if (!SecurityService.login.isEmpty()) { loginName.setText(SecurityService.login, TextView.BufferType.EDITABLE) }
        initializeButtons()
    }

    private fun initializeButtons() {

        submit.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                lockUnlockScreen(true)

                if (!areValuesValid()) {
                    lockUnlockScreen(false)
                    return@setOnTouchListener false
                }

                SecurityService.login = loginName.text.toString()
                launch(CommonPool) {
                    // TODO what should we do if reg/unreg fails?
                    VkApi.unregisterDevice()
                    val result = VkApi.performVkLogin(SecurityService.login, password.text.toString())
                    VkApi.registerDevice()
                    launch(UI) { loginCallback(result) }
                }
            }

            return@setOnTouchListener false
        }
    }

    fun loginCallback(result: String) {
        when(result) {
            "ok" -> {
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
        submit.isFocusable = !lock
        submit.isClickable = !lock
    }
}
