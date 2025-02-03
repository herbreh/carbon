// File: FoodSafetyApiClient.kt
package com.example.carbon3.network

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

class FoodSafetyApiClient(private val apiKey: String) {

    /**
     * 바코드를 사용하여 제품명을 조회합니다.
     * @param barcode 바코드 값
     * @return 제품명 또는 null (찾을 수 없을 경우)
     */
    suspend fun getProductName(barcode: String): String? {
        val url = "http://openapi.foodsafetykorea.go.kr/api/$apiKey/C005/xml/1/1/BAR_CD=$barcode"
        val response = URL(url).readText()

        var productName: String? = null
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(response.reader())

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "RESULT" -> {
                        parser.next()
                        if (parser.text == "NO_RESULT") {
                            productName = null
                            break
                        }
                    }
                    "PRDLST_NM" -> {
                        parser.next()
                        productName = parser.text
                    }
                }
            }
            eventType = parser.next()
        }

        return productName
    }
}
