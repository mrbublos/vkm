package vkm.vkm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.kittinunf.fuel.core.FuelManager
import kotlinx.android.synthetic.main.activity_proxy.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import vkm.vkm.utils.ProxyAdapter
import java.net.InetSocketAddress
import java.net.Proxy as JProxy

class ProxyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxy)

        proxies.adapter = ProxyAdapter(applicationContext, R.layout.proxy_element, State.proxies, this::setProxy)
    }

    private fun setProxy(proxy: Proxy?) {
        FuelManager.instance.proxy = proxy?.let { JProxy(JProxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)) }
        launch(CommonPool) { VkApi.refreshToken() }
        finish()
    }

}