package virtual.camera.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import virtual.camera.app.R
import virtual.camera.app.data.models.InstalledAppBean
import virtual.camera.app.databinding.ItemAvailableAppBinding

class AvailableAppsAdapter(
    private val onAppClick: (InstalledAppBean) -> Unit
) : ListAdapter<InstalledAppBean, AvailableAppsAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAvailableAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemAvailableAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: InstalledAppBean) {
            binding.apply {
                textAppName.text = app.name
                textPackageName.text = app.packageName
                textVersion.text = app.versionName
                imageAppIcon.setImageDrawable(app.icon)

                // Show install status
                if (app.isInstalled) {
                    buttonInstall.text = root.context.getString(R.string.installed)
                    buttonInstall.isEnabled = false
                } else {
                    buttonInstall.text = root.context.getString(R.string.install)
                    buttonInstall.isEnabled = true
                }

                buttonInstall.setOnClickListener { onAppClick(app) }
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<InstalledAppBean>() {
        override fun areItemsTheSame(
            oldItem: InstalledAppBean,
            newItem: InstalledAppBean
        ): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(
            oldItem: InstalledAppBean,
            newItem: InstalledAppBean
        ): Boolean {
            return oldItem == newItem
        }
    }
}
