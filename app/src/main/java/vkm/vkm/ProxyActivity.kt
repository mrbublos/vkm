package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import com.github.kittinunf.fuel.core.FuelManager
import vkm.vkm.utils.ProxyAdapter
import java.net.InetSocketAddress
import java.net.Proxy as JProxy

class ProxyActivity : AppCompatActivity() {

    private val proxyList by bind<ListView>(R.id.proxies)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxy)

        proxyList.adapter = ProxyAdapter(applicationContext, R.layout.proxy_element, StateManager.proxies, this::setProxy)
    }

    private fun setProxy(proxy: Proxy?) {
        FuelManager.instance.proxy = proxy?.let { JProxy(JProxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)) }
        finish()
    }

}