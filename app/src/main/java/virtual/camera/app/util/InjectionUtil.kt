package virtual.camera.app.util

import virtual.camera.app.data.AppsRepository
import virtual.camera.app.data.XpRepository
import virtual.camera.app.view.apps.AppsFactory
import virtual.camera.app.view.list.ListFactory

object InjectionUtil {

    private val appsRepository = AppsRepository()
    private val xpRepository = XpRepository()

    fun getAppsFactory(): AppsFactory {
        return AppsFactory(appsRepository)
    }

    fun getListFactory(): ListFactory {
        return ListFactory(appsRepository)
    }
}
