package takagicom.todo.jodo.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限助手类，用于处理应用所需的各种权限
 */
object PermissionHelper {
    
    const val REQUEST_NOTIFICATION_PERMISSION = 1001
    
    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13以下版本不需要运行时权限
            true
        }
    }
    
    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }
    
    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowNotificationPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }
      /**
     * 处理权限请求结果
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        @Suppress("UNUSED_PARAMETER") permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        }
    }
}
