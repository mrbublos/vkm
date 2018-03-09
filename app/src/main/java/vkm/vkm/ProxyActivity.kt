package vkm.vkm

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.kittinunf.fuel.core.FuelManager
import kotlinx.android.synthetic.main.activity_proxy.*
import vkm.vkm.utils.Proxy
import vkm.vkm.utils.ProxyAdapter
import java.net.InetSocketAddress
import java.net.Proxy as JProxy

class ProxyActivity : AppCompatActivity() {

    // TODO move to more appropriate class
    companion object {
        fun setProxy(proxy: Proxy?) {
            FuelManager.instance = FuelManager()
            FuelManager.instance.proxy = proxy?.let { JProxy(JProxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)) }
        }

        fun setProxy(proxy: String) {
            FuelManager.instance = FuelManager()
            FuelManager.instance.proxy = proxy.takeIf { it.isNotEmpty() }?.let {
                JProxy(JProxy.Type.HTTP, InetSocketAddress(proxy.split(":")[0], proxy.split(":")[1].toInt()))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxy)

        proxies.adapter = ProxyAdapter(applicationContext, R.layout.proxy_element, State.proxies, this::setProxy)
    }

    private fun setProxy(proxy: Proxy?) {
        ProxyActivity.setProxy(proxy)
        startActivity(Intent(applicationContext, LoginActivity::class.java))
        finish()
    }

}