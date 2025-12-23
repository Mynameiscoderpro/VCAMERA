package virtual.camera.app.view.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import virtual.camera.app.R
import virtual.camera.app.app.App
import virtual.camera.app.app.AppManager
import virtual.camera.app.databinding.ActivityMainBinding
import virtual.camera.app.util.AppUtil
import virtual.camera.app.util.Resolution
import virtual.camera.app.util.ToastUtils
import virtual.camera.app.util.inflate
import virtual.camera.app.view.apps.AppsFragment
import virtual.camera.app.view.base.LoadingActivity
import virtual.camera.app.view.list.ListActivity
import virtual.camera.app.view.setting.SettingActivity
import virtual.camera.core.service.VirtualCameraService

/**
 * Fixed: Removed HackApi dependency - now single-user mode only
 */
class MainActivity : LoadingActivity() {

    private val viewBinding: ActivityMainBinding by inflate()
    private lateinit var mViewPagerAdapter: ViewPagerAdapter
    private val fragmentList = mutableListOf<AppsFragment>()
    private var currentUser = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name)
        initViewPager()
        initFab()
        initToolbarSubTitle()
        DialogUtil.showDialog(this, true)
    }

    private fun initToolbarSubTitle() {
        updateUserRemark(0)
        // Click listener for subtitle
        viewBinding.toolbarLayout.toolbar.getChildAt(1).setOnClickListener {
            MaterialDialog(this).show {
                title(res = R.string.userRemark)
                input(
                    hintRes = R.string.userRemark,
                    prefill = viewBinding.toolbarLayout.toolbar.subtitle
                ) { _, input ->
                    AppManager.mRemarkSharedPreferences.edit {
                        putString("Remark0", input.toString())
                        viewBinding.toolbarLayout.toolbar.subtitle = input
                    }
                }
                positiveButton(res = R.string.done)
                negativeButton(res = R.string.cancel)
            }
        }
    }

    private fun initViewPager() {
        // Fixed: Single user mode only (userId = 0)
        fragmentList.add(AppsFragment.newInstance(0))

        mViewPagerAdapter = ViewPagerAdapter(this)
        mViewPagerAdapter.replaceData(fragmentList)
        viewBinding.viewPager.adapter = mViewPagerAdapter
        viewBinding.dotsIndicator.setViewPager2(viewBinding.viewPager)

        viewBinding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentUser = 0 // Always 0 in single-user mode
                updateUserRemark(0)
                showFloatButton(true)
            }
        })
    }

    private fun initFab() {
        viewBinding.fab.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            intent.putExtra("userID", 0) // Always user 0
            apkPathResult.launch(intent)
        }
    }

    fun showFloatButton(show: Boolean) {
        val tranY: Float = Resolution.convertDpToPixel(120F, App.getContext())
        val time = 200L
        if (show) {
            viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time)
                .start()
        } else {
            viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time)
                .start()
        }
    }

    fun scanUser() {
        // Fixed: No-op in single-user mode
        // This was for multi-user space scanning
    }

    private fun updateUserRemark(userId: Int) {
        var remark = AppManager.mRemarkSharedPreferences.getString("Remark$userId", "User $userId")
        if (remark.isNullOrEmpty()) {
            remark = "User $userId"
        }
        viewBinding.toolbarLayout.toolbar.subtitle = remark
    }

    private val apkPathResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.let { data ->
                    val source = data.getStringExtra("source")
                    if (source != null) {
                        fragmentList[0].installApk(source) // Always use first fragment
                    }
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // ✅ FIXED: Check if menu item exists before adding
        if (menu != null) {
            val toggleItem = menu.add(0, R.id.toggle_camera, 0, "Enable Camera")
            toggleItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            toggleItem.setIcon(android.R.drawable.ic_menu_camera)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_setting -> {
                SettingActivity.start(this)
                return true
            }
            R.id.killApps -> {
                AppUtil.killAllApps()
                ToastUtils.showToast("Done.")
                return true
            }
            R.id.open_source -> {
                DialogUtil.showDialog(this, false)
                return true
            }
            R.id.toggle_camera -> {
                toggleVirtualCamera(item)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    /**
     * Toggle virtual camera on/off from the menu
     */
    private fun toggleVirtualCamera(menuItem: MenuItem) {
        val isEnabled = VirtualCameraService.isRunning

        if (isEnabled) {
            App.stopVirtualCamera()
            menuItem.title = "Enable Camera"
            menuItem.setIcon(android.R.drawable.ic_menu_camera)
            ToastUtils.showToast("Virtual camera disabled")
        } else {
            App.startVirtualCamera()
            menuItem.title = "Disable Camera"
            menuItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
            ToastUtils.showToast("Virtual camera enabled")
        }
    }
}