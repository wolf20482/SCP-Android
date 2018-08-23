package info.free.scp

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_category.view.*

/**
 * Created by zhufree on 2018/8/22.
 *
 */

class CategoryHolder(view: View) : RecyclerView.ViewHolder(view){

    fun setData(model: SimpleSCPModel) {
        itemView.tvScpTitle.text = model.title
    }
}
