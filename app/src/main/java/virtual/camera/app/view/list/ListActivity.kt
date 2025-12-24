package virtual.camera.app.view.list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import virtual.camera.app.R
import virtual.camera.app.bean.InstalledAppBean
import virtual.camera.app.databinding.ActivityListBinding
import virtual.camera.app.util.InjectionUtil
import virtual.camera.app.util.inflate
import virtual.camera.app.view.base.BaseActivity

class ListActivity : BaseActivity() {

    private val viewBinding: ActivityListBinding by inflate()
    private lateinit var mAdapter: ListAdapter
    private lateinit var viewModel: ListViewModel
    private var appList: List<InstalledAppBean> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        initToolbar(viewBinding.toolbarLayout.toolbar, R.string.installed_app, true)

        mAdapter = ListAdapter { item ->
            finishWithResult(item.packageName)
        }

        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = mAdapter

        initViewModel()
    }

    private fun filterApp(newText: String) {
        val newList = this.appList.filter {
            it.name.contains(newText, true) || it.packageName.contains(newText, true)
        }
        mAdapter.setItems(newList)
    }

    private val openDocumentedResult = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.run {
            finishWithResult(it.toString())
        }
    }

    private fun finishWithResult(source: String) {
        intent.putExtra("source", source)
        setResult(Activity.RESULT_OK, intent)
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        window.peekDecorView()?.run {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        // Search functionality removed - menu_search doesn't exist
        return true
    }

    override fun onStop() {
        super.onStop()
        viewModel.loadingLiveData.postValue(true)
        viewModel.loadingLiveData.removeObservers(this)
        viewModel.appsLiveData.postValue(null)
        viewModel.appsLiveData.removeObservers(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, InjectionUtil.getListFactory()).get(ListViewModel::class.java)
        val onlyShowXp = intent.getBooleanExtra("onlyShowXp", false)
        val userID = intent.getIntExtra("userID", 0)

        if (onlyShowXp) {
            viewModel.getInstalledModules()
            viewBinding.toolbarLayout.toolbar.setTitle(R.string.installed_module)
        } else {
            viewModel.getInstallAppList(userID)
            viewBinding.toolbarLayout.toolbar.setTitle(R.string.installed_app)
        }

        // ✅ FIXED: Simplified without loading view
        viewModel.loadingLiveData.observe(this) { isLoading ->
            viewBinding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.appsLiveData.observe(this) {
            if (it != null) {
                this.appList = it
                mAdapter.setItems(it)
                viewBinding.recyclerView.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    companion object {
        fun start(context: Context, onlyShowXp: Boolean) {
            val intent = Intent(context, ListActivity::class.java)
            intent.putExtra("onlyShowXp", onlyShowXp)
            context.startActivity(intent)
        }
    }
}
