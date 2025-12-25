package virtual.camera.app.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import virtual.camera.app.ui.fragments.AppsFragment
import virtual.camera.app.ui.fragments.CameraFragment
import virtual.camera.app.ui.fragments.SettingsFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppsFragment.newInstance()
            1 -> CameraFragment.newInstance()
            2 -> SettingsFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
