package info.free.scp.view.download

import android.graphics.Color
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lzy.okgo.model.Progress
import com.lzy.okserver.download.DownloadListener
import info.free.scp.R
import info.free.scp.ScpApplication
import info.free.scp.bean.DownloadModel
import info.free.scp.databinding.ItemDownloadBinding
import info.free.scp.db.ScpDatabase
import info.free.scp.util.DownloadUtil
import info.free.scp.util.DownloadUtil.Status.DOWNLOADING
import info.free.scp.util.DownloadUtil.Status.ERROR
import info.free.scp.util.DownloadUtil.Status.FINISH
import info.free.scp.util.DownloadUtil.Status.NEED_UPDATE
import info.free.scp.util.DownloadUtil.Status.NONE
import info.free.scp.util.DownloadUtil.Status.PAUSE
import info.free.scp.util.FileHelper
import info.free.scp.util.PreferenceUtil
import info.free.scp.util.ThemeUtil
import org.jetbrains.anko.*
import java.io.File


class DownloadAdapter : ListAdapter<DownloadModel, DownloadAdapter.DownloadHolder>(DownloadDiffCallback()) {

    val downloadList = arrayListOf(-1L, -1L, -1L, -1L, -1L, -1L)
    val holderList = arrayListOf<DownloadHolder>()
    var mStartVideoHandler: Handler = Handler()
    private var runnable: Runnable? = null




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadHolder {

        val holder = DownloadHolder(ItemDownloadBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
        holderList.add(holder)
        if (holderList.size == 6 && runnable == null) {
            runnable = Runnable {
                for ((index, id) in downloadList.withIndex()) {
                    if (id > 0) {
                        holderList[index].refreshProgress(DownloadUtil.getDownloadStatus(id))
                    }
                }
                runnable?.let {
                    mStartVideoHandler.postDelayed(runnable, 1000)
                }
            }
            runnable?.run()
        }
        return holder
    }

    override fun onBindViewHolder(holder: DownloadHolder, position: Int) {
        val download = getItem(position)
        // 绑定 holder
        holder.apply {
            val link = PreferenceUtil.getDataDownloadLink(position - 1)
            if (link.isEmpty()) {
                ScpApplication.currentActivity?.toast("下载链接未加载成功，请稍后再进入该页面")
            }
            ScpApplication.currentActivity?.info("download: $position: $link")
//            val request: GetRequest<File> = OkGo.get(link)
//            val task = OkDownload.request(link, request)
//                    .fileName(getFileNameByIndex(position - 1))
//                    .register(ListDownloadListener(download.title, this, position - 1))
//                    .save()
            bind(createOnClickListener(position), download) // 点击事件
            itemView.tag = download
        }
    }

    private fun getFileNameByIndex(index: Int): String {
        return when (index) {
            -1 -> "full_scp_data.db"
            else -> "only_scp_$index.db"
        }
    }

    private fun createOnClickListener(position: Int): View.OnClickListener {
        return View.OnClickListener {
            val fileHelper = FileHelper.getInstance(ScpApplication.context)
            if (position == 0) {
                // 检查本地是否有已经下载过的
                if (fileHelper.checkBackupDataExist() && downloadList[position] == -1L) { // 是总数据库
                    ScpApplication.currentActivity?.alert("检测到该数据库之前已下载完成，是否恢复？", "恢复") {
                        positiveButton("恢复") {
                            ScpApplication.context.toast("开始恢复")
                            doAsync {
                                if (fileHelper.restoreData()) {
                                    ScpDatabase.getNewInstance()
                                    uiThread {
                                        ScpApplication.currentActivity?.toast("恢复完成")
                                    }
                                }
                            }
                        }
                        negativeButton("仍要下载") {
                            toggleDownloadStatus(position, PreferenceUtil.getDataDownloadLink(position - 1))
//                            if (task.progress.status == Progress.LOADING) {
//                                task.pause()
//                            } else if (task.progress.url.isNotEmpty()) {
//                                task.start()
//                            }
                        }
                        neutralPressed("取消") {}
                    }?.show()
                } else {
                    toggleDownloadStatus(position, PreferenceUtil.getDataDownloadLink(position - 1))
//                    if (task.progress.status == Progress.LOADING) {
//                        task.pause()
//                    } else if (task.progress.url.isNotEmpty()) {
//                        task.start()
//                    }
                }
            } else {
                toggleDownloadStatus(position, PreferenceUtil.getDataDownloadLink(position - 1))
//                if (task.progress.status == Progress.LOADING) {
//                    task.pause()
//                } else if (task.progress.url.isNotEmpty()) {
//                    task.start()
//                }
            }
        }
    }

    private fun toggleDownloadStatus(position: Int, url: String) {
        val downloadId = downloadList[position]
        if (downloadId < 0) {
            val newDownloadId = DownloadUtil.createDownload(url)
            downloadList[position] = newDownloadId
        } else {
            ScpApplication.downloadManager.remove(downloadId)
            downloadList[position] = -1
            holderList[position].refreshProgress("取消下载")
        }

    }

    class DownloadHolder(private val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root) {

        var index = 0
        fun bind(listener: View.OnClickListener, item: DownloadModel) {
            // 具体绑定监听事件和数据
            binding.apply {
                clickListener = listener
                download = item
                index = item.dbIndex
                tvDownloadTime.text = "本地同步时间：" + item.lastDownloadTime
                tvUpdateTime.text = "服务器更新时间：" + item.lastUpdateTime
                val fillColor = when (item.status) {
                    FINISH -> Color.GREEN
                    NONE -> Color.LTGRAY
                    DOWNLOADING -> Color.BLUE
                    else -> Color.LTGRAY

                }
                vDownloadStatus.post {
                    vDownloadStatus.background = ThemeUtil.customShape(fillColor, fillColor, 0,
                            itemView.context.dip(5))
                }
                executePendingBindings()
            }
        }

        fun refreshStatus(status: Int) {
            val fillColor = when (status) {
                FINISH -> Color.GREEN
                NONE -> Color.LTGRAY
                DOWNLOADING -> Color.BLUE
                PAUSE -> itemView.context.resources.getColor(R.color.colorAccent)
                NEED_UPDATE -> Color.YELLOW
                ERROR -> Color.RED
                else -> Color.LTGRAY

            }
            binding.vDownloadStatus.post {
                binding.vDownloadStatus.background = ThemeUtil.customShape(fillColor, fillColor, 0,
                        itemView.context.dip(5))
            }
            if (status == FINISH) {
                binding.tvDownloadProgress.text = ""
                // TODO 更新时间 离线完成时间
                PreferenceUtil.setDetailLastLoadTime(index, System.currentTimeMillis())
            }
        }

        fun refreshProgress(info: String) {
            binding.tvDownloadProgress.text = info
        }
    }

    class ListDownloadListener(tag: Any, val holder: DownloadHolder, val index: Int) : DownloadListener(tag) {
        override fun onFinish(t: File?, progress: Progress?) {
            holder.refreshStatus(FINISH)
            Log.i("freescp", "finish")
            if (ScpApplication.currentActivity != null) {
                Log.i("freescp", ScpApplication.currentActivity.toString())
                ScpApplication.currentActivity?.alert("确定使用已下载的数据库文件${t?.name
                        ?: ""}吗？建议复制完成后重启一次app",
                        "下载完成") {
                    yesButton {
                        ScpApplication.currentActivity?.toast("开始复制")
                        doAsync {
                            FileHelper(ScpApplication.currentActivity!!).copyDataBaseFile(t?.name
                                    ?: "", true)
                            ScpDatabase.getNewInstance()
                            uiThread {
                                ScpApplication.currentActivity?.toast("复制完成")
                            }
                        }

                        for (i in -1..4) {
                            PreferenceUtil.setDetailDataLoadFinish(i, false)
                        }
                        PreferenceUtil.setDetailDataLoadFinish(index, true)
                    }
                    noButton { }
                }?.show()
            }
        }

        override fun onRemove(progress: Progress?) {
        }

        /**
         * int NONE = 0;
         * int WAITING = 1;
         * int LOADING = 2;
         * int PAUSE = 3;
         * int ERROR = 4;
         * int FINISH = 5;
         * @param progress Progress?
         */
        override fun onProgress(progress: Progress?) {
//            holder.refreshProgress(progress)
            if (progress?.status == Progress.PAUSE) {
                holder.refreshStatus(PAUSE)
            }
        }

        override fun onError(progress: Progress?) {
            holder.refreshStatus(ERROR)
        }

        override fun onStart(progress: Progress?) {
            holder.refreshStatus(DOWNLOADING)
        }
    }

    /**
     * 用来对比列表中数据是否变化
     */
    private class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadModel>() {

        /**
         * 是否是同一个item
         */
        override fun areItemsTheSame(
                oldItem: DownloadModel,
                newItem: DownloadModel
        ): Boolean {
            return oldItem.title == newItem.title
        }

        /**
         * item的内容是否一致，仅当[areItemsTheSame]返回true时才调用做进一步判断
         */
        override fun areContentsTheSame(
                oldItem: DownloadModel,
                newItem: DownloadModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}