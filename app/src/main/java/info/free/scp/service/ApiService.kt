package info.free.scp.service

import info.free.scp.PrivateConstants
import info.free.scp.bean.ApiBean
import info.free.scp.bean.ScpModel
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Created by Free on 2018/5/5.
 * retrofit接口
 */

interface ApiService {
    // 获取全部数据
    // --data-urlencode 'limit=200' \
    // --data-urlencode 'skip=400' \
    // --data-urlencode 'keys=score,playerName' \
    @Headers(
            "Content-Type:application/json",
            "X-Bmob-Application-Id:${PrivateConstants.APP_ID}",
            "X-Bmob-REST-API-Key:${PrivateConstants.API_KEY}"
    )
    @GET("1/classes/ScpModelV2")
    fun getAllScp(@Query("skip") skip: Int,
                  @Query("limit") limit: Int,
                  @Query("keys") keys: String="type,cn,contest_link,contest_name,created_time," +
                          "desc,event_type,link,month,number,page_code,snippet,story_num," +
                          "subtext,title,author")
            : Observable<ApiBean.ApiListResponse<ScpModel>>

    // 获取全部数据
    // --data-urlencode 'limit=200' \
    // --data-urlencode 'skip=400' \
    // --data-urlencode 'keys=score,playerName' \
    @Headers(
            "Content-Type:application/json",
            "X-Bmob-Application-Id:${PrivateConstants.APP_ID}",
            "X-Bmob-REST-API-Key:${PrivateConstants.API_KEY}"
    )
    @GET("1/classes/ScpModelV2")
    fun getScpDetail(@Query("skip") skip: Int,
                  @Query("limit") limit: Int,
                  @Query("keys") keys: String="detail")
            : Observable<ApiBean.ApiListResponse<ScpModel>>


    @Headers(
            "Content-Type:application/json",
            "X-Bmob-Application-Id:${PrivateConstants.APP_ID}",
            "X-Bmob-REST-API-Key:${PrivateConstants.API_KEY}"
    )
    @GET("1/classes/Config")
    fun getAppConfig(): Observable<ApiBean.ApiListResponse<ApiBean.ConfigResponse>>


}