package virtual.camera.app.app;

import android.app.Application;
import android.content.Context;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class App extends Application {

    private static App instance;
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        context = getApplicationContext();
    }

    public static App getInstance() {
        return instance;
    }

    public static Context getContext() {
        return context;
    }
}
