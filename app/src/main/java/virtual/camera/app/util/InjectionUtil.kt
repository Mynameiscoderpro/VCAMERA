package virtual.camera.app.util

import virtual.camera.app.data.AppsRepository
import virtual.camera.app.data.XpRepository
import virtual.camera.app.view.apps.AppsFactory
import virtual.camera.app.view.list.ListFactory

object InjectionUtil {

    // ✅ FIXED: Use lazy initialization instead of eager initialization
    private val appsRepository by lazy { AppsRepository() }
    private val xpRepository by lazy { XpRepository() }

    fun getAppsFactory(): AppsFactory {
        return AppsFactory(appsRepository)
    }

    fun getListFactory(): ListFactory {
        return ListFactory(appsRepository)
    }
}
