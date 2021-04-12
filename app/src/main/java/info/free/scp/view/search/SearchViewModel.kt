package info.free.scp.view.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.free.scp.SCPConstants.SearchType.TITLE
import info.free.scp.bean.*
import info.free.scp.db.ScpDatabase
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val scpDao = ScpDatabase.getInstance().scpDao()
    var titleResult = emptyList<ScpModel>()
    var contentResult: MutableLiveData<List<ScpModel>> = MutableLiveData()

    fun searchScp(type: Int, keyword: String) {
        if (type == TITLE) {
            titleResult = scpDao.searchScpByTitle(keyword)
        } else {
            viewModelScope.launch {
                contentResult.postValue(searchDetail(keyword))
            }
        }
//        scpDao?.let {
//        } ?: run {
//            return emptyList()
//        }
    }

    suspend fun searchDetail(keyword: String): List<ScpModel> {
        return scpDao.searchScpByDetail(keyword)
    }
}