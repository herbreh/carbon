package com.example.carbon3.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 공공데이터포털 영양성분 가공 정보 API (예시)
 * 실제 parameter 이름, JSON 구조는 문서와 일치하게 꼭 확인하세요.
 */
interface FoodApiService {

    /**
     * tn_pubr_public_nutri_process_info_api
     * - serviceKey: 인코딩된 인증키
     * - pageNo, numOfRows: 페이지네이션
     * - type=json (JSON 응답을 받기 위한 파라미터 - API 문서에 따라 다를 수 있음)
     */
    @GET("tn_pubr_public_nutri_process_info_api")
    suspend fun getNutriProcessInfo(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("type") type: String = "json"
    ): NutriProcessResponse
}

/**
 * 실제 응답 JSON 구조에 맞춰서 데이터 클래스 정의
 * 아래는 "response → header/body → items" 형태라는 가정하에 예시 작성
 */
data class NutriProcessResponse(
    val response: ResponseWrapper?
)

data class ResponseWrapper(
    val header: ResponseHeader?,
    val body: ResponseBody?
)

data class ResponseHeader(
    val resultCode: String?,
    val resultMsg: String?
)

data class ResponseBody(
    val pageNo: Int?,
    val totalCount: Int?,
    val items: NutriProcessItems?
)

data class NutriProcessItems(
    val item: List<NutriItem>?
)

/** 실제 필드명은 문서에 따라 다름. 아래는 임의 예시 */
data class NutriItem(
    val prdlstNm: String?,  // 예: 제품명
    val totalCnt: String?,  // 예: 총내용량
    val pageUrl: String?,   // 예: 상세 페이지
    // ...
)
