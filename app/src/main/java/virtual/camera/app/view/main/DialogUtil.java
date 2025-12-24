package virtual.camera.app.view.main;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import virtual.camera.app.R;
import virtual.camera.app.util.ToastUtils;

public class DialogUtil {
    public static void showDialog(final Activity activity, boolean check) {
        try {
            // ✅ FIXED: Use standard SharedPreferences instead of MultiPreferences
            SharedPreferences prefs = activity.getSharedPreferences("vcamera_prefs", Activity.MODE_PRIVATE);
            boolean show_start_dialog = prefs.getBoolean("show_start_dialog", true);

            if (!show_start_dialog && check) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity).setCancelable(false);
            builder.setTitle(R.string.tips).setMessage(R.string.dialog_github_start);
            builder.setPositiveButton(R.string.goto_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Mynameiscoderpro/VCAMERA"));
                        activity.startActivity(intent);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        ToastUtils.showToast("open failed.");
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        dialog.dismiss();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
            if (check) {
                builder.setNeutralButton(R.string.never_show, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dialog.dismiss();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        // ✅ FIXED: Use standard SharedPreferences
                        SharedPreferences prefs = activity.getSharedPreferences("vcamera_prefs", Activity.MODE_PRIVATE);
                        prefs.edit().putBoolean("show_start_dialog", false).apply();
                    }
                });
            }
            AlertDialog alertDialog = builder.show();
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextAppearance(R.style.VCameraDialog);
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextAppearance(R.style.VCameraDialog);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
