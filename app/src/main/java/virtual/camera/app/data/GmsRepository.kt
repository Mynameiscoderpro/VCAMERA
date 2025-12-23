package virtual.camera.app.data

import androidx.lifecycle.MutableLiveData
import virtual.camera.app.bean.GmsBean
import virtual.camera.app.bean.GmsInstallBean

/**
 * Fixed: Removed all HackApi dependencies
 * This is now a stub repository for GMS features (not functional without HackApi)
 */
class GmsRepository {

    fun getGmsInstalledList(mInstalledLiveData: MutableLiveData<List<GmsBean>>) {
        // Fixed: Return empty list - GMS features require HackApi
        mInstalledLiveData.postValue(emptyList())
    }

    fun installGms(
        userID: Int,
        mUpdateInstalledLiveData: MutableLiveData<GmsInstallBean>
    ) {
        // ✅ FIXED: Correct parameter types
        mUpdateInstalledLiveData.postValue(GmsInstallBean(false, "GMS not available"))
    }

    fun uninstallGms(
        userID: Int,
        mUpdateInstalledLiveData: MutableLiveData<GmsInstallBean>
    ) {
        // ✅ FIXED: Correct parameter types
        mUpdateInstalledLiveData.postValue(GmsInstallBean(false, "GMS not available"))
    }
}