package virtual.camera.app.view.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
        fragmentList.add(AppsFragment.newInstance(0))

        mViewPagerAdapter = ViewPagerAdapter(this)
        mViewPagerAdapter.replaceData(fragmentList)
        viewBinding.viewPager.adapter = mViewPagerAdapter
        viewBinding.dotsIndicator.setViewPager2(viewBinding.viewPager)

        viewBinding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentUser = 0
                updateUserRemark(0)
                showFloatButton(true)
            }
        })
    }

    private fun initFab() {
        viewBinding.fab.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            intent.putExtra("userID", 0)
            apkPathResult.launch(intent)
        }
    }

    fun showFloatButton(show: Boolean) {
        val tranY: Float = Resolution.convertDpToPixel(120F, App.getContext() ?: return)
        val time = 200L
        if (show) {
            viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time).start()
        } else {
            viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time).start()
        }
    }

    fun scanUser() {
        // No-op in single-user mode
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
                        fragmentList[0].installApk(source)
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
            R.id.toggle_camera -> {
                toggleVirtualCamera(item)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ✅ FIXED: Moved methods from App to MainActivity
    private fun toggleVirtualCamera(menuItem: MenuItem) {
        val sharedPrefs = getSharedPreferences("vcamera_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("camera_enabled", false)

        if (isEnabled) {
            stopVirtualCamera()
            sharedPrefs.edit().putBoolean("camera_enabled", false).apply()
            menuItem.title = getString(R.string.enable_camera)
            ToastUtils.showToast(getString(R.string.camera_disabled))
        } else {
            startVirtualCamera()
            sharedPrefs.edit().putBoolean("camera_enabled", true).apply()
            menuItem.title = getString(R.string.disable_camera)
            ToastUtils.showToast(getString(R.string.camera_enabled))
        }
    }

    // ✅ NEW: Start Virtual Camera Service
    private fun startVirtualCamera() {
        try {
            val intent = Intent(this, VirtualCameraService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d("MainActivity", "Virtual camera service started")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting virtual camera: ${e.message}", e)
            ToastUtils.showToast("Failed to start virtual camera")
        }
    }

    // ✅ NEW: Stop Virtual Camera Service
    private fun stopVirtualCamera() {
        try {
            val intent = Intent(this, VirtualCameraService::class.java)
            stopService(intent)
            Log.d("MainActivity", "Virtual camera service stopped")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping virtual camera: ${e.message}", e)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
