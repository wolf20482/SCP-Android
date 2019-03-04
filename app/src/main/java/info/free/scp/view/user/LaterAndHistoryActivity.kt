package info.free.scp.view.user

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import info.free.scp.R
import info.free.scp.SCPConstants
import info.free.scp.SCPConstants.HISTORY_TYPE
import info.free.scp.SCPConstants.LATER_TYPE
import info.free.scp.bean.SimpleScp
import info.free.scp.db.ScpDao
import info.free.scp.util.EventUtil
import info.free.scp.util.EventUtil.clickHistoryList
import info.free.scp.util.Toaster
import info.free.scp.view.detail.DetailActivity
import info.free.scp.view.base.BaseActivity
import info.free.scp.view.base.BaseAdapter
import info.free.scp.view.search.SimpleScpAdapter
import kotlinx.android.synthetic.main.activity_like.*
import kotlinx.android.synthetic.main.layout_dialog_report.view.*

/**
 * 待读列表
 */
class LaterAndHistoryActivity : BaseActivity() {
    private var viewType = -1
        set(value) {
            field = value
            viewItemList.clear()
            viewItemList.addAll(ScpDao.getInstance().getViewListByTypeAndOrder(value, orderType))
            supportActionBar?.title = if (value == HISTORY_TYPE) "历史阅读记录" else "待读列表"
            if (value == HISTORY_TYPE) {
                EventUtil.onEvent(this, clickHistoryList)
            }
            adapter?.notifyDataSetChanged()
        }
    val viewItemList = emptyList<SimpleScp?>().toMutableList()
    var adapter : SimpleScpAdapter? = null
    private var orderType = 1 // 0 時間正序，倒序
        set(value) {
            field = value
            viewItemList.clear()
            viewItemList.addAll(ScpDao.getInstance().getViewListByTypeAndOrder(viewType, value))
            adapter?.notifyDataSetChanged()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)
        initToolbar()

        val lm = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        rv_like?.layoutManager = lm
        adapter = SimpleScpAdapter(this, viewItemList)
        rv_like?.adapter = adapter
        adapter?.mOnItemClickListener = object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val intent = Intent()
                intent.putExtra("link", viewItemList[position]?.link)
                intent.setClass(this@LaterAndHistoryActivity, DetailActivity::class.java)
                startActivity(intent)
            }
        }
        viewType = intent?.getIntExtra("view_type", 0)?:0
    }

    override fun onResume() {
        super.onResume()
        viewItemList.clear()
        viewItemList.addAll(ScpDao.getInstance().getViewListByTypeAndOrder(viewType, orderType))
        adapter?.notifyDataSetChanged()
    }

    private fun initToolbar() {
        setSupportActionBar(like_toolbar)
        like_toolbar?.inflateMenu(R.menu.menu_read_list)
        like_toolbar?.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        like_toolbar?.setNavigationOnClickListener { finish() }
        like_toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.reverse -> {
                    orderType = if (orderType == 0) 1 else 0
                }
                R.id.import_read_list -> {
                    if (viewType == LATER_TYPE) {
                        showInputListDialog()
                    } else {
                        Toaster.show("请在待读列表页点击此按钮")
                    }
                }
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.category_menu, menu)
        return true
    }

    private fun showInputListDialog() {
        val inputView = LayoutInflater.from(this)
                .inflate(R.layout.layout_dialog_input_large, null)
        val inputDialog = AlertDialog.Builder(this)
                .setTitle(R.string.menu_import_read_list)
                .setMessage("导入的文章标题用逗号分隔，标题内需要包含cn，j等关键词作为区分")
                .setView(inputView)
                .setPositiveButton("OK") { _, _ -> }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create()
        inputDialog.show()
        inputDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val inputString = inputView.et_report.text.toString()
            Log.i("readlist", inputString)
            splitReadList(inputString)
            inputDialog.dismiss()
            Toaster.show("导入完成")
        }
    }

    private fun splitReadList(input: String) {
        val titleList = input.split(",")
        if (titleList.isEmpty()) return
        titleList.forEach { str ->
            var type = SCPConstants.ScpType.SAVE_SERIES
            var numberString = ""
            if (str.contains("cn") || str.contains("CN")) {
                type = SCPConstants.ScpType.SAVE_SERIES_CN
            }
            str.forEach {
                if (it.isDigit()) {
                    numberString += it
                } else {
                    if (it == 'j' || it == 'J') {
                        type = if (type == SCPConstants.ScpType.SAVE_SERIES) SCPConstants.ScpType.SAVE_JOKE else SCPConstants.ScpType.SAVE_JOKE_CN
                    }
                }
            }
            if (numberString.isNotEmpty()) {
                val targetScp = ScpDao.getInstance().getScpByTypeAndNumber(type, numberString)
                if (targetScp != null) {
                    print(targetScp)
                    ScpDao.getInstance().insertViewListItem(targetScp.link, targetScp.title, SCPConstants.LATER_TYPE)
                }
            }
        }
    }
}
