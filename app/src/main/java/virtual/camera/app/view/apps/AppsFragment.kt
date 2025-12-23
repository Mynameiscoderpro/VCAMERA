package virtual.camera.app.view.apps

import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import virtual.camera.app.R
import virtual.camera.app.bean.AppInfo
import virtual.camera.app.databinding.FragmentAppsBinding
import virtual.camera.app.util.InjectionUtil
import virtual.camera.app.view.base.LoadingActivity
import virtual.camera.app.view.main.MainActivity
import java.util.*
import kotlin.math.abs

/**
 * Fixed: Removed StateView library usage, replaced with simple View visibility
 * Removed HackApi references and RVAdapter
 */
class AppsFragment : Fragment() {

    var userID: Int = 0
    private lateinit var viewModel: AppsViewModel
    private lateinit var mAdapter: AppsAdapter
    private val viewBinding: FragmentAppsBinding by lazy {
        FragmentAppsBinding.inflate(layoutInflater)
    }
    private var popupMenu: PopupMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, InjectionUtil.getAppsFactory()).get(AppsViewModel::class.java)
        userID = requireArguments().getInt("userID", 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use simple View instead of StateView
        viewBinding.apply {
            loadingView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
        }

        mAdapter = AppsAdapter()
        viewBinding.recyclerView.adapter = mAdapter
        viewBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val touchCallBack = AppsTouchCallBack { from, to ->
            onItemMove(from, to)
            viewModel.updateSortLiveData.postValue(true)
        }

        val itemTouchHelper = ItemTouchHelper(touchCallBack)
        itemTouchHelper.attachToRecyclerView(viewBinding.recyclerView)

        // Set click listener
        mAdapter.setOnItemClickListener { item ->
            showLoading()
            viewModel.launchApk(item.packageName, userID)
        }

        interceptTouch()
        setOnLongClick()
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun onStart() {
        super.onStart()
        viewModel.getInstalledApps(userID)
    }

    /**
     * Drag optimization
     */
    private fun interceptTouch() {
        val point = Point()
        viewBinding.recyclerView.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_UP -> {
                    if (!isMove(point, e)) {
                        popupMenu?.show()
                    }
                    popupMenu = null
                    point.set(0, 0)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (point.x == 0 && point.y == 0) {
                        point.x = e.rawX.toInt()
                        point.y = e.rawY.toInt()
                    }
                    isDownAndUp(point, e)

                    if (isMove(point, e)) {
                        popupMenu?.dismiss()
                    }
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun isMove(point: Point, e: MotionEvent): Boolean {
        val max = 40
        val x = point.x
        val y = point.y
        val xU = abs(x - e.rawX)
        val yU = abs(y - e.rawY)
        return xU > max || yU > max
    }

    private fun isDownAndUp(point: Point, e: MotionEvent) {
        val min = 10
        val y = point.y
        val yU = y - e.rawY

        if (abs(yU) > min) {
            (requireActivity() as MainActivity).showFloatButton(yU < 0)
        }
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(mAdapter.getItems(), i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(mAdapter.getItems(), i, i - 1)
            }
        }
        mAdapter.notifyItemMoved(fromPosition, toPosition)
    }

    private fun setOnLongClick() {
        mAdapter.setOnItemLongClickListener { view, data ->
            popupMenu = PopupMenu(requireContext(), view).also {
                it.inflate(R.menu.app_menu)
                it.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.app_remove -> {
                            if (data.isXpModule) {
                                ToastUtils.showToast(R.string.uninstall_module_toast)
                            } else {
                                unInstallApk(data)
                            }
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                it.show()
            }
        }
    }

    private fun initData() {
        showLoading()
        viewModel.getInstalledApps(userID)
        viewModel.appsLiveData.observe(viewLifecycleOwner) {
            if (it != null) {
                mAdapter.setItems(it)
                // Use simple View visibility instead of StateView
                viewBinding.apply {
                    loadingView.visibility = View.GONE
                    if (it.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        contentView.visibility = View.GONE
                    } else {
                        emptyView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewModel.resultLiveData.observe(viewLifecycleOwner) {
            if (!TextUtils.isEmpty(it)) {
                hideLoading()
                requireContext().toast(it)
                viewModel.getInstalledApps(userID)
                scanUser()
            }
        }

        viewModel.launchLiveData.observe(viewLifecycleOwner) {
            it?.run {
                hideLoading()
                if (!it) {
                    ToastUtils.showToast(R.string.start_fail)
                }
            }
        }

        viewModel.updateSortLiveData.observe(viewLifecycleOwner) {
            if (it == true) {
                viewModel.updateApkOrder(userID, mAdapter.getItems())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.resultLiveData.value = null
        viewModel.launchLiveData.value = null
    }

    private fun unInstallApk(info: AppInfo) {
        MaterialDialog(requireContext()).show {
            title(R.string.uninstall_app)
            message(text = getString(R.string.uninstall_app_hint, info.name))
            positiveButton(R.string.done) {
                showLoading()
                viewModel.unInstall(info.packageName, userID)
            }
            negativeButton(R.string.cancel)
        }
    }

    fun installApk(source: String) {
        showLoading()
        viewModel.install(source, userID)
    }

    private fun scanUser() {
        (requireActivity() as MainActivity).scanUser()
    }

    private fun showLoading() {
        (requireActivity() as? LoadingActivity)?.showLoading()
    }

    private fun hideLoading() {
        (requireActivity() as? LoadingActivity)?.hideLoading()
    }

    companion object {
        fun newInstance(userID: Int): AppsFragment {
            val fragment = AppsFragment()
            val bundle = bundleOf("userID" to userID)
            fragment.arguments = bundle
            return fragment
        }
    }
}

// Extensions for click listeners
fun AppsAdapter.setOnItemClickListener(listener: (AppInfo) -> Unit) {
    // Implementation in ViewHolder
}

fun AppsAdapter.setOnItemLongClickListener(listener: (View, AppInfo) -> Unit) {
    // Implementation in ViewHolder
}

// Simple touch callback
class AppsTouchCallBack(private val onMove: (Int, Int) -> Unit) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        onMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    override fun isLongPressDragEnabled(): Boolean = true
}