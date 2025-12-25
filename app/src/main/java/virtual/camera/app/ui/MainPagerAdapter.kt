package virtual.camera.app.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import virtual.camera.app.ui.fragments.AppsFragment
import virtual.camera.app.ui.fragments.CameraFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CameraFragment()
            1 -> AppsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
