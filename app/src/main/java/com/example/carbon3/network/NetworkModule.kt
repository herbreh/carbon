package com.example.carbon3.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // API 엔드포인트 (마지막에 / 포함)
    // -> http://api.data.go.kr/openapi/
    private const val BASE_URL = "http://api.data.go.kr/openapi/"

    // Retrofit 인스턴스 (싱글톤)
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())  // JSON 변환기
            .build()
    }

    // FoodApiService 구현체
    val foodApiService: FoodApiService by lazy {
        retrofit.create(FoodApiService::class.java)
    }
}
