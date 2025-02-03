package com.example.carbon3.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.carbon3.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class HomeFragment : Fragment() {
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        resultTextView = view.findViewById(R.id.resultTextView)

        searchButton.setOnClickListener {
            val foodName = searchEditText.text.toString()
            if (foodName.isNotEmpty()) {
                searchFood(foodName)
            }
        }

        return view
    }

    private fun searchFood(foodName: String) {
        lifecycleScope.launch {
            try {
                val serviceKey = "/htUdqATshOhd/y8WDXw/QmL/8YHT86WpwNOjkwAFrxVmaoqsgFG+sMw/JHmkXREz7MtYrzPTXqLseQsZGxTyQ=="
                val encodedServiceKey = URLEncoder.encode(serviceKey, "UTF-8")
                val encodedFoodName = URLEncoder.encode(foodName, "UTF-8")

                val urlString = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo01/getFoodNtrCpntDbInq01" +
                        "?serviceKey=$encodedServiceKey" +
                        "&pageNo=1" +
                        "&numOfRows=3" +
                        "&type=json" +
                        "&FOOD_NM_KR=$encodedFoodName"

                val result = withContext(Dispatchers.IO) {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15000

                    try {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()
                        connection.disconnect()
                        response
                    } catch (e: Exception) {
                        connection.disconnect()
                        throw e
                    }
                }

                // JSON 파싱 및 UI 업데이트
                withContext(Dispatchers.Main) {
                    try {
                        val jsonObject = JSONObject(result)
                        val items = jsonObject.getJSONObject("body")
                            .getJSONArray("items")

                        if (items.length() > 0) {
                            val food = items.getJSONObject(0)
                            val displayText = """
                                식품명: ${food.getString("FOOD_NM_KR")}
                                1회 제공량: ${food.getString("SERVING_SIZE")}
                                열량: ${food.getString("AMT_NUM1")} kcal
                                단백질: ${food.getString("AMT_NUM3")} 
                                당류: ${food.getString("AMT_NUM8")} g
                                나트륨: ${food.getString("AMT_NUM14")} mg
                            """.trimIndent()

                            resultTextView.text = displayText
                        } else {
                            resultTextView.text = "검색 결과가 없습니다."
                        }
                    } catch (e: Exception) {
                        resultTextView.text = "데이터 파싱 오류: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultTextView.text = "네트워크 오류: ${e.message}"
                }
            }
        }
    }
}
