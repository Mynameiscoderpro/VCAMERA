package virtual.camera.app.view.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import virtual.camera.app.R
import virtual.camera.app.bean.InstalledAppBean

/**
 * Fixed: Replaced RVHolder with standard RecyclerView.ViewHolder
 */
class ListAdapter(
    private val onItemClick: (InstalledAppBean) -> Unit
) : RecyclerView.Adapter<ListAdapter.ListVH>() {

    private val items = mutableListOf<InstalledAppBean>()

    fun setItems(newItems: List<InstalledAppBean>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_package, parent, false)
        return ListVH(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ListVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ListVH(itemView: View, private val onItemClick: (InstalledAppBean) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val packageName: TextView = itemView.findViewById(R.id.packageName)

        fun bind(item: InstalledAppBean) {
            icon.setImageDrawable(item.icon)
            name.text = item.name
            packageName.text = item.packageName

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}