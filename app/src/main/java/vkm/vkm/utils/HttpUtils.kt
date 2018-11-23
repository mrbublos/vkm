package vkm.vkm.utils

import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.jsoup.Jsoup
import vkm.vkm.State
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.suspendCoroutine

class HttpUtils {

    companion object {

        private val proxyBlacklist = ConcurrentHashMap<Proxy, Long>()
        private val untrustedProxyList = mutableListOf<Proxy>()
        var currentProxy: Proxy? = null

        private fun setProxy(proxy: Proxy?) {
            "Using proxy: $proxy".log()
            if (proxy != null) {
                currentProxy = proxy
                currentProxy?.type = "current"
            }
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
                if (it.type == "current") {
                    "Setting proxy $it".log()
                    currentProxy = it
                    return@forEach
                }
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
                    if (currentProxy == null) { return null }
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
                caller.responseJson { _, resp, result ->
                    try {
                        if (result is Result.Success && resp.headers["Content-Type"]?.firstOrNull()?.contains("application/json") == true) {
                            "Received result ${result.component1()?.content}".log()
                            continuation.resume(result.component1()!!)
                            return@responseJson
                        } else {
                            val currProxy = currentProxy
                            if (currProxy != null) {
                                "Blacklisting $currProxy".log()
                                currProxy.type = "blacklisted"
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

        private suspend fun getProxy(): Proxy? {
            val currProxy = currentProxy
            if (currProxy != null && proxyBlacklist[currProxy] == null) { return currProxy }

            var fetched = false
            while (true) {
                if (untrustedProxyList.isEmpty()) {
                    if (fetched) { return null } // we have already fetched during this iteration, next fetch will bring the same results
                    untrustedProxyList.addAll(fetchProxyList())
                    fetched = true
                }

                val proxy = untrustedProxyList[0]
                val time = proxyBlacklist[proxy] ?: return proxy
                if (System.currentTimeMillis() - time > 1000 * 60 * 60 * 24) {
                    proxyBlacklist.remove(proxy)
                    return proxy
                }

                // it is still blacklisted, removing from a list
                untrustedProxyList.removeAt(0)
            }
        }

        private suspend fun fetchProxyList(): List<Proxy> {
            "Fetching proxy list".log()
            return suspendCoroutine { continuation ->
                try {
                    Jsoup.connect("https://www.proxy" + "nova.com/proxy-server-list/country-ru/").get().run {
                        "Proxy list fetched".log()
                        val result = getElementById("tbl_p" + "roxy_list").select("tbody tr").map { row ->
                            if (!row.hasAttr("data-proxy-id")) {
                                return@map Proxy("", 0)
                            }

                            val columns = row.select("td")
                            val ip = columns[0].select("abbr").attr("title")
                            val port = columns[1].select("a").text()
                            val speed = columns[3].select("small").text().split(" ")[0]
                            val type = columns[6].select("span").text()

                            if (port.isBlank() || speed.isBlank()) {
                                return@map Proxy("", 0)
                            }
                            Proxy(host = ip, port = port.toInt(), type = type, speed = speed.toInt())
                        }.filter { it.port != 0 }
                        continuation.resume(result)
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
}

enum class HttpMethod {
    GET,
    POST
    ;
}