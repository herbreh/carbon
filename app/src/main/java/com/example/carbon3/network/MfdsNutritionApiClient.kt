package com.example.carbon3.network

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class NutritionInfo(
    val servingSize: String?, // 영양성분함량기준량
    val energy: String?,      // 에너지(kcal)
    val protein: String?,     // 단백질(g)
    val fat: String?,         // 지방(g)
    val carbs: String?,       // 탄수화물(g)
    val sugar: String?,       // 당류(g)
    val sodium: String?,      // 나트륨(mg)
    val foodName: String?     // 식품명
)

class MfdsNutritionApiClient(private val serviceKeyEncoded: String) {

    companion object {
        private const val TAG = "MfdsNutritionApiClient"
    }

    suspend fun getNutritionInfo(productName: String): NutritionInfo? {
        val endpoint = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo01/getFoodNtrCpntDbInq01"

        // URL 구성 - 파라미터를 명확하게 분리
        val urlString = buildString {
            append(endpoint)
            append("?serviceKey=").append(serviceKeyEncoded)
            append("&pageNo=1")
            append("&numOfRows=3")
            append("&type=xml")
            append("&FOOD_NM_KR=").append(URLEncoder.encode("크런키빼빼로", "UTF-8"))
        }

        Log.d(TAG, "Request URL: $urlString")

        val response = try {
            URL(urlString).readText().also {
                Log.d(TAG, "Raw Response: $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            throw e
        }

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(response.reader())

            var currentTag = ""
            var nutritionInfo = NutritionInfo(
                servingSize = null,
                energy = null,
                protein = null,
                fat = null,
                carbs = null,
                sugar = null,
                sodium = null,
                foodName = null
            )

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                    }
                    XmlPullParser.TEXT -> {
                        when (currentTag) {
                            "FOOD_NM_KR" -> {
                                nutritionInfo = nutritionInfo.copy(foodName = parser.text)
                                Log.d(TAG, "Found FOOD_NM_KR: ${parser.text}")
                            }
                            "SERVING_SIZE" -> {
                                nutritionInfo = nutritionInfo.copy(servingSize = parser.text)
                                Log.d(TAG, "Found SERVING_SIZE: ${parser.text}")
                            }
                            "AMT_NUM1" -> {
                                nutritionInfo = nutritionInfo.copy(energy = parser.text)
                                Log.d(TAG, "Found Energy: ${parser.text}")
                            }
                            "AMT_NUM3" -> {
                                nutritionInfo = nutritionInfo.copy(protein = parser.text)
                                Log.d(TAG, "Found Protein: ${parser.text}")
                            }
                            "AMT_NUM4" -> {
                                nutritionInfo = nutritionInfo.copy(fat = parser.text)
                                Log.d(TAG, "Found Fat: ${parser.text}")
                            }
                            "AMT_NUM7" -> {
                                nutritionInfo = nutritionInfo.copy(carbs = parser.text)
                                Log.d(TAG, "Found Carbs: ${parser.text}")
                            }
                            "AMT_NUM8" -> {
                                nutritionInfo = nutritionInfo.copy(sugar = parser.text)
                                Log.d(TAG, "Found Sugar: ${parser.text}")
                            }
                            "AMT_NUM14" -> {
                                nutritionInfo = nutritionInfo.copy(sodium = parser.text)
                                Log.d(TAG, "Found Sodium: ${parser.text}")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            return nutritionInfo.also {
                Log.d(TAG, "Final NutritionInfo: $it")
            }

        } catch (e: Exception) {
            Log.e(TAG, "XML Parsing error", e)
            throw e
        }
    }
}

