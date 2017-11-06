package vkm.vkm.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.proxy_element.view.*
import vkm.vkm.R

class ProxyAdapter(context: Context, resource: Int, data: List<Proxy?>, private var elementClickListener: (user: Proxy?) -> Unit? = {}) : ArrayAdapter<Proxy>(context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.proxy_element, null)
        val item = getItem(position)

        item?.let {
            if (item.host.isNotEmpty()) {
                view?.address?.text = "${item.host}:${item.port}"
                view?.country?.text = "${item.country} (${item.type})"
                view?.setOnClickListener { elementClickListener(item) }
            } else {
                view?.address?.text = context.getString(R.string.no_proxy)
                view?.country?.text = ""
                view?.setOnClickListener { elementClickListener(null) }
            }
        }

        return view
    }
}