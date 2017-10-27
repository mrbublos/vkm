package vkm.vkm.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import vkm.vkm.R

class ProxyAdapter(context: Context, resource: Int, data: List<Proxy?>, private var elementClickListener: (user: Proxy?) -> Unit? = {}) : ArrayAdapter<Proxy>(context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.proxy_element, null)
        val item = getItem(position)

        item?.let {
            if (item.host.isNotEmpty()) {
                view?.bind<TextView>(R.id.address)?.text = "${item.host}:${item.port}"
                view?.bind<TextView>(R.id.country)?.text = "${item.country} (${item.type})"
                view?.setOnClickListener { elementClickListener.invoke(item) }
            } else {
                view?.bind<TextView>(R.id.address)?.text = context.getString(R.string.no_proxy)
                view?.bind<TextView>(R.id.country)?.text = ""
                view?.setOnClickListener { elementClickListener.invoke(null) }
            }
        }

        return view
    }
}