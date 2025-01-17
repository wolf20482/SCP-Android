@file:Suppress("SpellCheckingInspection")

package info.free.scp.util

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import info.free.scp.SCPConstants.PUBLIC_DIR_NAME
import info.free.scp.ScpApplication
import org.jetbrains.anko.toast
import org.jetbrains.anko.windowManager
import java.io.*
import java.text.DateFormat.LONG
import java.text.DateFormat.SHORT
import java.text.SimpleDateFormat


object Utils {

    /**
     * 获取屏幕宽高
     */
    fun getScreenHeight(context: Activity): Int {
        val metric = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metric)
        return metric.heightPixels
    }

    fun getScreenWidth(context: Context): Int {
        val metric = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metric)
        return metric.widthPixels
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    fun dp2px(dpValue: Int, context: Context = ScpApplication.context): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    fun px2dp(pxValue: Int, context: Context = ScpApplication.context): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * wifi开启检查
     *
     * @return
     */
    fun enabledWifi(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        return wifiManager != null && wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED
    }

    /**
     * 3G网开启检查
     *
     * @return
     */
    fun enabledNetwork(context: Context): Boolean {
        val mConnectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable
            }
        } else {
            val network = mConnectivityManager.activeNetwork ?: return false
            val status = mConnectivityManager.getNetworkCapabilities(network)
                    ?: return false
            if (status.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                return true
            }
        }
        return false
    }
    fun onlyEnabled4G(context: Context): Boolean {
        return enabledNetwork(context) && !enabledWifi(context)
    }

    fun formatDate(time: Long): String {
        val format = SimpleDateFormat.getDateTimeInstance(SHORT, LONG)
        return format.format(time)
    }

    fun formatNow() = formatDate(System.currentTimeMillis())


    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun copyPrivateFileToPublic(context: Context, orgFilePath: String, displayName: String, mimeType: String) {
        val values = ContentValues()
        values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
        values.put(MediaStore.Files.FileColumns.TITLE, displayName)
        values.put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + PUBLIC_DIR_NAME)
        val external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val resolver = context.contentResolver
        val insertUri = resolver.insert(external, values)
        var ist: InputStream? = null
        var ost: OutputStream? = null
        try {
            ist = FileInputStream(File(orgFilePath))
            if (insertUri != null) {
                ost = resolver.openOutputStream(insertUri)
            }
            if (ost != null) {
                val buffer = ByteArray(4096)
                var byteCount = 0
                while (ist.read(buffer).also { byteCount = it } != -1) {
                    ost.write(buffer, 0, byteCount)
                }
            }
        } catch (e: IOException) {
        } finally {
            try {
                ist?.close()
                ost?.close()
            } catch (e: IOException) {
            }
        }
    }


    /**
     * bitmap保存为一个文件
     * @param bitmap bitmap对象
     * @return 文件对象
     */
    fun saveBitmapFile(bitmap: Bitmap, filename: String): File {
        val filePath = getAlbumStorageDir("SCP").path + "/$filename"
        val file = File("$filePath.jpg")
        try {
            val outputStream = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val contentResolver = ScpApplication.context.contentResolver
        val values = ContentValues(4)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.ORIENTATION, 0)
        values.put(MediaStore.Images.Media.TITLE, "scp_donation")
        values.put(MediaStore.Images.Media.DESCRIPTION, "scp_donation")
        values.put(MediaStore.Images.Media.DATA, file.absolutePath)
        values.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        var url: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ScpApplication.context.grantUriPermission(ScpApplication.context.packageName,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            url = contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            //其实质是返回 Image.Media.DATA中图片路径path的转变而成的uri
            url?.let {
                val imageOut = contentResolver?.openOutputStream(url)
                imageOut?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageOut)
                }
                val id = ContentUris.parseId(url)
                MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND,
                        null)
            }

            //获取缩略图

        } catch (e: Exception) {
            if (url != null) {
                contentResolver?.delete(url, null, null)
            }
        }
        return file
    }

    fun save(file: File, filename: String) {
        val filePath = getAlbumStorageDir("SCP").path + "/$filename"
        val newFile = File("$filePath.jpg")
        if (!newFile.exists()) {
            newFile.createNewFile()
        }
        try {
            val outputStream = BufferedOutputStream(FileOutputStream(newFile))
            outputStream.write(file.readBytes())
            outputStream.close()
        } catch (e: FileNotFoundException) {
            ScpApplication.currentActivity?.toast("未开启SD卡读取权限，需要手动开启。")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getAlbumStorageDir(albumName: String): File {
        // Get the directory for the user's public pictures directory.
        val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName)
        if (!file.mkdirs() && !file.exists()) {
            Log.e("scp", "Directory not created")
        }
        return file
    }

    /**
     * 保存gif文件，单独处理
     */
    fun saveGifFile(bytes: ByteArray, fileName: String) {
        val destFile = File(fileName)
        try {
            val outputStream = FileOutputStream(destFile, false)
            outputStream.write(bytes)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    fun getUrlIntent(url: String): Intent {
        val updateIntent = Intent()
        updateIntent.action = "android.intent.action.VIEW"
        val updateUrl = Uri.parse(url)
        updateIntent.data = updateUrl
        return updateIntent
    }

    fun share(file: File, activity: AppCompatActivity) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity,
                    activity.applicationContext.packageName + ".provider", file)
            )
        } else {
            shareIntent.putExtra(Intent.EXTRA_STREAM, file.toUri())
        }
        activity.startActivity(Intent.createChooser(shareIntent, "分享到"))
    }
}