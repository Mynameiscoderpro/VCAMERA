package virtual.camera.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import virtual.camera.app.data.models.AppInfo
import virtual.camera.app.databinding.ItemInstallAppBinding

class InstallAppsAdapter(
    private val onInstallClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, InstallAppsAdapter.InstallAppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstallAppViewHolder {
        val binding = ItemInstallAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InstallAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InstallAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InstallAppViewHolder(
        private val binding: ItemInstallAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.apply {
                tvAppName.text = app.packageName
                tvPackageName.text = app.packageName

                btnInstall.setOnClickListener {
                    onInstallClick(app)
                }
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
