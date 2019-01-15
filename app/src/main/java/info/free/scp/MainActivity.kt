package info.free.scp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.LayoutInflater
import info.free.scp.SCPConstants.BroadCastAction.ACTION_CHANGE_THEME
import info.free.scp.SCPConstants.BroadCastAction.INIT_PROGRESS
import info.free.scp.SCPConstants.BroadCastAction.LOAD_DETAIL_FINISH
import info.free.scp.db.ScpDao
import info.free.scp.service.HttpManager
import info.free.scp.service.InitCategoryService
import info.free.scp.service.InitDetailService
import info.free.scp.util.*
import info.free.scp.util.EventUtil.chooseJob
import info.free.scp.view.user.UserFragment
import info.free.scp.view.base.BaseActivity
import info.free.scp.view.base.BaseFragment
import info.free.scp.view.category.ScpListActivity
import info.free.scp.view.home.HomeFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_dialog_report.view.*


class MainActivity : BaseActivity(), HomeFragment.CategoryListener, UserFragment.AboutListener {
    private var currentFragment: BaseFragment? = null
    private val homeFragment = HomeFragment.newInstance()
//    private val feedFragment = FeedFragment.newInstance()
    private val userFragment = UserFragment.newInstance()
    private var isDownloadingDetail = false

    var progressDialog: ProgressDialog? = null

    private var mLocalBroadcastManager: LocalBroadcastManager? = null


    private var themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshTheme()
        }
    }


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val transaction = supportFragmentManager?.beginTransaction()
        transaction?.hide(currentFragment as Fragment)
        when (item.itemId) {
            R.id.navigation_home -> {
                if (homeFragment.isAdded) {
                    transaction?.show(homeFragment)
                } else {
                    transaction?.add(R.id.flMainContainer, homeFragment)
                }
                currentFragment = homeFragment
            }
        //            R.id.navigation_feed -> {
        //                if (feedFragment.isAdded) {
        //                    transaction.show(feedFragment)
        //                } else {
        //                    transaction.add(R.id.flMainContainer, feedFragment)
        //                }
        //                currentFragment = feedFragment
        //                return@OnNavigationItemSelectedListener true
        //            }
            R.id.navigation_about -> {
                if (userFragment.isAdded) {
                    transaction?.show(userFragment)
                } else {
                    transaction?.add(R.id.flMainContainer, userFragment)
                }
                currentFragment = userFragment
            }
        }
        transaction?.commit()
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 注册广播
        registerBroadCastReceivers()

        setContentView(R.layout.activity_main)
        // 恢复意外退出的数据（不知道有没有用）
        savedInstanceState?.let {
            currentFragment = supportFragmentManager.getFragment(savedInstanceState, "currentFragment") as BaseFragment
        }

        // 设置默认fragment
        val transaction = supportFragmentManager?.beginTransaction()
        if (currentFragment != null) {
            if (currentFragment?.isAdded == true) {
                transaction?.show(currentFragment!!)
            } else {
                transaction?.add(R.id.flMainContainer, currentFragment!!)
            }
        } else {
            transaction?.add(R.id.flMainContainer, homeFragment)
            currentFragment = homeFragment
        }
        transaction?.commit()
        navigation?.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        Logger.local = true
        UpdateManager.getInstance(this).checkAppData()
    }

    /**
     * 注册广播
     * 1. 初始化目录
     * 2. 加载正文
     * 3. 主题
     */
    private fun registerBroadCastReceivers() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(INIT_PROGRESS)
        mLocalBroadcastManager?.registerReceiver(themeReceiver, IntentFilter(ACTION_CHANGE_THEME))
    }





    override fun onSaveInstanceState(outState: Bundle) {
        currentFragment?.let {
            supportFragmentManager.putFragment(outState, "currentFragment", it)
        }
        super.onSaveInstanceState(outState)
    }

    /**
     * 初始化数据
     */
    private fun initCategoryData() {
        val intent = Intent(this, InitCategoryService::class.java)
        startService(intent)
        progressDialog = ProgressDialog(this)
        progressDialog?.max = 100
        progressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog?.setMessage("与基金会通信中")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }

    private fun initDetailData() {
        val intent = Intent(this, InitDetailService::class.java)
        startService(intent)
        isDownloadingDetail = true
    }


    fun refreshTheme() {
        navigation.setBackgroundColor(ThemeUtil.containerBg)
        homeFragment.refreshTheme()
        userFragment.refreshTheme()
    }

    override fun onCategoryClick(type: Int) {
        val intent = Intent()
        intent.putExtra("saveType", type)
        intent.setClass(this, ScpListActivity::class.java)
        startActivity(intent)
    }

    override fun onInitDataClick() {
        UpdateManager.getInstance(this).showChooseDbDialog()
    }

    override fun onResetDataClick() {
        // 重新加载
        UpdateManager.getInstance(this).checkInitData(true)
    }
}
