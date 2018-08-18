package vkm.vkm.utils

import com.beust.klaxon.JsonObject
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.jsoup.Jsoup
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.suspendCoroutine

class HttpUtils {

    companion object {

        private val blacklist = ConcurrentHashMap<Proxy, Long>()
        private var currentProxy: Proxy? = null

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

        suspend fun call4Json(url: String, withProxy: Boolean = false): JsonObject {
            var retries = 0
            while (true) {
                try {
                    retries++
                    val result = callHttp(url, withProxy)
                    return result?.component1().toJson()
                } catch (e: ProxyNotAvailableException) {
                    if (retries > 10) { return JsonObject() }
                    "Retrying with another proxy".log()
                } catch (e: Exception) {
                    return JsonObject()
                }
            }
        }

        private suspend fun callHttp(url: String, withProxy: Boolean = false): Result<String, FuelError>? {
            setProxy(if (withProxy) getProxy() else null)

            "Calling: $url".log()
            val caller = url.httpGet()
            return suspendCoroutine { continuation ->
                caller.responseString { _, response, result ->
                    "Received result $result".log()
                    try {
                        if (response.statusCode == 200) {
                            continuation.resume(result)
                            return@responseString
                        } else {
                            val currProxy = currentProxy
                            if (currProxy != null
                                    && result.component2() != null
                                    && result.component2()!!.exception is IOException) {
                                "Blacklisting $currProxy".log()
                                blacklist[currProxy] = System.currentTimeMillis()
                            }
                            result.component2().toString().logE()
                            throw ProxyNotAvailableException()
                        }
                    } catch (e: ProxyNotAvailableException) {
                        continuation.resumeWithException(e)
                        return@responseString
                    } catch (e: Exception) {
                        "Error connecting".logE(e)
                    }
                    continuation.resume(null)
                }
            }
        }

        private fun getProxy(): Proxy? {
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
                val time = blacklist[proxy] ?: return proxy
                if (System.currentTimeMillis() - time > 1000 * 60 * 60 * 24) {
                    blacklist.remove(proxy)
                    return proxy
                }
                result.removeAt(0)
            }
        }
    }
}