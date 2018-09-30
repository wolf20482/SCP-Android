package info.free.scp.view.base

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.umeng.analytics.MobclickAgent


open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    public override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }

    /**
     * wifi开启检查
     *
     * @return
     */
    fun Activity.enabledWifi(): Boolean {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        return wifiManager != null && wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED
    }

    /**
     * 3G网开启检查
     *
     * @return
     */
    fun Activity.enabledNetwork(): Boolean {
        val cManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        return cManager?.activeNetworkInfo != null && cManager?.activeNetworkInfo.isAvailable
    }
}
