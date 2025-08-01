package com.codex.apk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PermissionManager {

    private static final String TAG = "PermissionManager";
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 101;
    public static final int REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 102;

    private final MainActivity mainActivity;

    public PermissionManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog();
            } else {
                mainActivity.onPermissionsGranted();
            }
        } else {
            if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mainActivity,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                mainActivity.onPermissionsGranted();
            }
        }
    }

    private void showManageStoragePermissionDialog() {
        new MaterialAlertDialogBuilder(mainActivity, R.style.AlertDialogCustom)
                .setTitle(mainActivity.getString(R.string.permission_required))
                .setMessage(mainActivity.getString(R.string.permission_required_message))
                .setPositiveButton(mainActivity.getString(R.string.grant), (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", mainActivity.getPackageName(), null);
                        intent.setData(uri);
                        mainActivity.startActivityForResult(intent, REQUEST_CODE_MANAGE_EXTERNAL_STORAGE);
                    } catch (Exception e) {
                        Toast.makeText(mainActivity, mainActivity.getString(R.string.could_not_open_settings), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(mainActivity.getString(R.string.cancel), (dialog, which) -> {
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                    mainActivity.updateEmptyStateVisibility();
                })
                .setCancelable(false)
                .show();
    }

    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
}
