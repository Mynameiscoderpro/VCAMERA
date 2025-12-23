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
                ToastUtils.showToast(getString(R.string.done))
                return true
            }
            R.id.open_source -> {
                DialogUtil.showDialog(this, false)
                return true
            }
            R.id.toggle_camera -> {  // ✅ This should exist in menu_main.xml
                toggleVirtualCamera(item)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleVirtualCamera(menuItem: MenuItem) {
        val sharedPrefs = getSharedPreferences("vcamera_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("camera_enabled", false)

        if (isEnabled) {
            App.stopVirtualCamera()
            sharedPrefs.edit().putBoolean("camera_enabled", false).apply()
            menuItem.title = getString(R.string.enable_camera)
            menuItem.setIcon(R.drawable.ic_camera_off)
            ToastUtils.showToast(getString(R.string.camera_disabled))
        } else {
            App.startVirtualCamera()
            sharedPrefs.edit().putBoolean("camera_enabled", true).apply()
            menuItem.title = getString(R.string.disable_camera)
            menuItem.setIcon(R.drawable.ic_camera_on)
            ToastUtils.showToast(getString(R.string.camera_enabled))
        }
    }
}