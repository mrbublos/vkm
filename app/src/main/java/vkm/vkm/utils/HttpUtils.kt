package vkm.vkm.utils

import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.jsoup.Jsoup
import vkm.vkm.State
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.suspendCoroutine

class HttpUtils {

    companion object {

        private val proxyBlacklist = ConcurrentHashMap<Proxy, Long>()
        var currentProxy: Proxy? = null

        private fun setProxy(proxy: Proxy?) {
            "Using proxy: $proxy".log()
            currentProxy = proxy
            FuelManager.instance = FuelManager()
            FuelManager.instance.proxy = proxy?.let {
                java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress(it.host, it.port))
            }
            FuelManager.instance.timeoutInMillisecond = 5000
            FuelManager.instance.timeoutReadInMillisecond = 5000
        }

        fun setBlackList(list: List<Proxy>) {
            proxyBlacklist.clear()
            list.forEach {
                proxyBlacklist[it] = it.added
            }
        }

        fun getBlackList(): List<Proxy> {
            return proxyBlacklist.map {
                it.key.added = it.value
                it.key
            }
        }

        suspend fun call4Json(method: HttpMethod, url: String, withProxy: Boolean = false): Json? {
            for (retries in 0..10) {
                try {
                    return callHttp(method, url, withProxy)
                } catch (e: ProxyNotAvailableException) {
                    "Retrying with another proxy".log()
                } catch (e: Exception) {
                    "Error connecting".logE(e)
                    break
                }
            }
            return null
        }

        private suspend fun callHttp(method: HttpMethod = HttpMethod.GET, url: String, withProxy: Boolean = false): Json {
            setProxy(if (State.useProxy && withProxy) getProxy() else null)

            "Calling: $method $url".log()
            val caller = when (method) {
                HttpMethod.GET -> url.httpGet()
                HttpMethod.POST -> url.httpPost()
            }

            return suspendCoroutine { continuation ->
                caller.responseJson { _, _, result ->
                    "Received result $result".log()
                    try {
                        if (result is Result.Success) {
                            continuation.resume(result.component1()!!)
                            return@responseJson
                        } else {
                            val currProxy = currentProxy
                            if (currProxy != null
                                    && result.component2() != null
                                    && result.component2()!!.exception is IOException) {
                                "Blacklisting $currProxy".log()
                                proxyBlacklist[currProxy] = System.currentTimeMillis()
                            }
                            result.component2().toString().logE()
                            throw ProxyNotAvailableException()
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }

        private fun getProxy(): Proxy? {
            val currProxy = currentProxy
            if (currProxy != null && proxyBlacklist[currProxy] == null) { return currProxy }

            val result = mutableListOf<Proxy>()
            Jsoup.connect("https://www.proxy" + "nova.com/proxy-server-list/country-ru/").get().run {
                getElementById("tbl_p" + "roxy_list").select("tbody tr").forEach { row ->
                    if (!row.hasAttr("data-proxy-id")) { return@forEach }

                    val columns = row.select("td")
                    val ip = columns[0].select("abbr").attr("title")
                    val port = columns[1].select("a").text()
                    val speed = columns[3].select("small").text().split(" ")[0]
                    val type = columns[6].select("span").text()

                    if (port.isBlank() || speed.isBlank()) { return@forEach }
                    result.add(Proxy(host = ip, port = port.toInt(), type = type, speed = speed.toInt()))
                }
            }

            result.sortBy { it.speed }

            while (true) {
                val proxy = if (result.isNotEmpty()) result[0] else return null
                val time = proxyBlacklist[proxy] ?: return proxy
                if (System.currentTimeMillis() - time > 1000 * 60 * 60 * 24) {
                    proxyBlacklist.remove(proxy)
                    return proxy
                }
                result.removeAt(0)
            }
        }
    }
}
enum class HttpMethod {
    GET,
    POST
    ;
}