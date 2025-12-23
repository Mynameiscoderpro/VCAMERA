package virtual.camera.app.view.apps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import virtual.camera.app.R
import virtual.camera.app.bean.AppInfo

/**
 * Fixed: Replaced RVAdapter with standard RecyclerView.Adapter
 */
class AppsAdapter : RecyclerView.Adapter<AppsAdapter.AppsVH>() {

    private val items = mutableListOf<AppInfo>()

    fun setItems(newItems: List<AppInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<AppInfo> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppsVH(view)
    }

    override fun onBindViewHolder(holder: AppsVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AppsVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val name: TextView = itemView.findViewById(R.id.name)

        fun bind(item: AppInfo) {
            icon.setImageDrawable(item.icon)
            name.text = item.name
        }
    }
}