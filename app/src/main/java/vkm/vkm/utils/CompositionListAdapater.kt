package vkm.vkm.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import vkm.vkm.Composition
import vkm.vkm.R
import vkm.vkm.bind

class CompositionListAdapter(context: Context, resource: Int, data: List<Composition>, var elementTouchListener: (composition: Composition, view: View) -> Unit? = { _,_ -> }) : ArrayAdapter<Composition>(context, resource, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var view = convertView

        if (view == null) { view = LayoutInflater.from(context).inflate(R.layout.composition_list_element, null) }

        val item = getItem(position)

        item?.let {
            view?.bind<TextView>(R.id.name)?.text = item.name
            view?.bind<TextView>(R.id.artist)?.text = item.artist
            view?.setOnTouchListener { v, event ->
                elementTouchListener.invoke(item, v)
                return@setOnTouchListener v.onTouchEvent(event)
            }
        }

        return view
    }
}