package com.example.carbon3.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.carbon3.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ResultActivity : AppCompatActivity() {

    private val foodSafetyApiKey = "a0e02a461adc46d593b5"

    // UI Components
    private lateinit var progressOverlay: View
    private lateinit var progressBar: View
    private lateinit var productNameTextView: TextView
    private lateinit var servingSizeTextView: TextView
    private lateinit var decreaseButton: MaterialButton
    private lateinit var quantityTextView: TextView
    private lateinit var increaseButton: MaterialButton
    private lateinit var energyTextView: TextView
    private lateinit var fatTextView: TextView
    private lateinit var carbsTextView: TextView
    private lateinit var proteinTextView: TextView
    private lateinit var sugarTextView: TextView
    private lateinit var sodiumTextView: TextView
    private lateinit var carbonTextView: TextView
    private lateinit var missingNutrientsTextView: TextView
    private lateinit var rescanButton: MaterialButton

    // Data Variables
    private var quantity: Int = 1
    private var originalNutrition: NutritionInfo? = null
    private var originalCarbonEmission: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 초기화
        progressOverlay = findViewById(R.id.progressOverlay)
        progressBar = findViewById(R.id.progressBar)
        productNameTextView = findViewById(R.id.productNameTextView)
        servingSizeTextView = findViewById(R.id.servingSizeTextView)
        decreaseButton = findViewById(R.id.decreaseButton)
        quantityTextView = findViewById(R.id.quantityTextView)
        increaseButton = findViewById(R.id.increaseButton)
        energyTextView = findViewById(R.id.energyTextView)
        fatTextView = findViewById(R.id.fatTextView)
        carbsTextView = findViewById(R.id.carbsTextView)
        proteinTextView = findViewById(R.id.proteinTextView)
        sugarTextView = findViewById(R.id.sugarTextView)
        sodiumTextView = findViewById(R.id.sodiumTextView)
        carbonTextView = findViewById(R.id.carbonTextView)
        missingNutrientsTextView = findViewById(R.id.missingNutrientsTextView)
        rescanButton = findViewById(R.id.rescanButton)

        // 초기 수량 설정
        quantityTextView.text = quantity.toString()

        // 버튼 클릭 리스너 설정
        increaseButton.setOnClickListener {
            if (quantity < 100) { // 최대 수량 제한
                quantity++
                quantityTextView.text = quantity.toString()
                updateNutritionAndCarbonEmission()
            }
        }

        decreaseButton.setOnClickListener {
            if (quantity > 1) { // 최소 수량 제한
                quantity--
                quantityTextView.text = quantity.toString()
                updateNutritionAndCarbonEmission()
            }
        }

        rescanButton.setOnClickListener {
            // 재촬영 버튼 클릭 시 현재 액티비티 종료
            finish()
            // 필요 시 스캔 액티비티를 명시적으로 시작할 수 있습니다.
            // val intent = Intent(this, ScanActivity::class.java)
            // startActivity(intent)
        }

        // 바코드 정보 받기
        val barcode = intent.getStringExtra(EXTRA_BARCODE)
        barcode?.let {
            showLoading(true)
            searchProductByBarcode(it)
        } ?: run {
            displayError("바코드 정보가 없습니다.")
        }
    }

    // 데이터 클래스 정의
    private data class NutritionInfo(
        val energy: Double?,
        val fat: Double?,
        val carbs: Double?,
        val protein: Double?,
        val sugar: Double?,
        val sodium: Double?
    )

    private data class CarbonEmissionResult(
        val emission: Double,
        val missingNutrients: List<String>
    )

    // 탄소 배출량 계산 함수
    private fun calculateCarbonEmission(
        energy: Double?,
        fat: Double?,
        carbs: Double?,
        protein: Double?,
        sugar: Double?,
        sodium: Double?
    ): CarbonEmissionResult {
        val missingNutrients = mutableListOf<String>()
        var penaltyMultiplier = 1.0

        // 기본 배출 계수
        val fatEmissionFactor = 0.06
        val carbsEmissionFactor = 0.03
        val proteinEmissionFactor = 0.1
        val sugarEmissionFactor = 0.02
        val sodiumEmissionFactor = 0.001

        // 필수 영양소 체크 및 패널티 적용
        if (fat == null) {
            missingNutrients.add("지방")
            penaltyMultiplier *= 1.2
        }
        if (carbs == null) {
            missingNutrients.add("탄수화물")
            penaltyMultiplier *= 1.2
        }
        if (protein == null) {
            missingNutrients.add("단백질")
            penaltyMultiplier *= 1.2
        }

        // 영양소별 배출량 계산 (없는 경우 기본값 적용)
        val fatEmission = (fat ?: 5.0) * fatEmissionFactor
        val carbsEmission = (carbs ?: 15.0) * carbsEmissionFactor
        val proteinEmission = (protein ?: 3.0) * proteinEmissionFactor
        val sugarEmission = (sugar ?: 8.0) * sugarEmissionFactor
        val sodiumEmission = (sodium ?: 100.0) * sodiumEmissionFactor / 1000

        // 총 배출량 계산 (패널티 적용)
        val totalEmission = (fatEmission + carbsEmission + proteinEmission + sugarEmission + sodiumEmission) * penaltyMultiplier

        return CarbonEmissionResult(totalEmission, missingNutrients)
    }

    // 바코드로 제품 검색 함수
    private fun searchProductByBarcode(barcode: String) {
        lifecycleScope.launch {
            try {
                val productName = withContext(Dispatchers.IO) {
                    val url = URL("http://openapi.foodsafetykorea.go.kr/api/$foodSafetyApiKey/C005/json/1/1/BAR_CD=$barcode")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    try {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()

                        val jsonObject = JSONObject(response)
                        val items = jsonObject.getJSONObject("C005").getJSONArray("row")

                        if (items.length() > 0) {
                            val rawProductName = items.getJSONObject(0).getString("PRDLST_NM")
                            rawProductName.replace(" ", "")
                        } else null
                    } finally {
                        connection.disconnect()
                    }
                }

                if (productName != null) {
                    searchNutritionInfo(productName)
                } else {
                    displayError("바코드에 해당하는 제품을 찾을 수 없습니다.")
                }

            } catch (e: Exception) {
                displayError("오류가 발생했습니다: ${e.message}")
            }
        }
    }

    // 제품명으로 영양 정보 검색 함수
    private fun searchNutritionInfo(productName: String) {
        lifecycleScope.launch {
            try {
                val serviceKey = "/htUdqATshOhd/y8WDXw/QmL/8YHT86WpwNOjkwAFrxVmaoqsgFG+sMw/JHmkXREz7MtYrzPTXqLseQsZGxTyQ=="
                val encodedServiceKey = URLEncoder.encode(serviceKey, "UTF-8")
                val encodedFoodName = URLEncoder.encode(productName, "UTF-8")

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

                    try {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()
                        response
                    } finally {
                        connection.disconnect()
                    }
                }

                withContext(Dispatchers.Main) {
                    try {
                        val jsonObject = JSONObject(result)
                        val body = jsonObject.getJSONObject("body")
                        val items = body.getJSONArray("items")

                        if (items.length() > 0) {
                            val food = items.getJSONObject(0)

                            val energy = food.optString("AMT_NUM1").toDoubleOrNull()
                            val fat = food.optString("AMT_NUM4").toDoubleOrNull()
                            val carbs = food.optString("AMT_NUM7").toDoubleOrNull()
                            val protein = food.optString("AMT_NUM3").toDoubleOrNull()
                            val sugar = food.optString("AMT_NUM8").toDoubleOrNull()
                            val sodium = food.optString("AMT_NUM14").toDoubleOrNull()

                            val servingSize = food.optString("SERVING_SIZE", "정보 없음")
                            val carbonResult = calculateCarbonEmission(energy, fat, carbs, protein, sugar, sodium)

                            // 원본 영양정보 및 탄소배출량 저장
                            originalNutrition = NutritionInfo(energy, fat, carbs, protein, sugar, sodium)
                            originalCarbonEmission = carbonResult.emission

                            // 제품명 및 1회 제공량 설정
                            productNameTextView.text = "제품명: $productName"
                            servingSizeTextView.text = "1회 제공량: $servingSize"

                            // 영양소 정보 설정 (초기 수량 1)
                            updateNutritionAndCarbonEmission()

                            // 누락된 영양소 표시
                            if (carbonResult.missingNutrients.isNotEmpty()) {
                                missingNutrientsTextView.visibility = View.VISIBLE
                                missingNutrientsTextView.text = "누락된 영양정보: ${carbonResult.missingNutrients.joinToString(", ")}\n(누락된 정보는 평균값으로 대체되어 계산되었습니다)"
                            } else {
                                missingNutrientsTextView.visibility = View.GONE
                            }

                            // 데이터 로딩 완료 후 로딩바 숨기기
                            showLoading(false)
                        } else {
                            displayError("영양정보를 찾을 수 없습니다.")
                        }
                    } catch (e: Exception) {
                        displayError("데이터 파싱 오류: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                displayError("네트워크 오류: ${e.message}")
            }
        }
    }

    // 수량에 따른 영양 정보 및 탄소 배출량 업데이트 함수
    private fun updateNutritionAndCarbonEmission() {
        originalNutrition?.let { nutrition ->
            val multiplier = quantity.toDouble()

            // 영양소 정보 업데이트
            val updatedEnergy = nutrition.energy?.times(multiplier)
            val updatedFat = nutrition.fat?.times(multiplier)
            val updatedCarbs = nutrition.carbs?.times(multiplier)
            val updatedProtein = nutrition.protein?.times(multiplier)
            val updatedSugar = nutrition.sugar?.times(multiplier)
            val updatedSodium = nutrition.sodium?.times(multiplier)

            energyTextView.text = updatedEnergy?.let { String.format("%.2f kcal", it) } ?: "정보 없음"
            fatTextView.text = updatedFat?.let { String.format("%.2f g", it) } ?: "정보 없음"
            carbsTextView.text = updatedCarbs?.let { String.format("%.2f g", it) } ?: "정보 없음"
            proteinTextView.text = updatedProtein?.let { String.format("%.2f g", it) } ?: "정보 없음"
            sugarTextView.text = updatedSugar?.let { String.format("%.2f g", it) } ?: "정보 없음"
            sodiumTextView.text = updatedSodium?.let { String.format("%.2f mg", it) } ?: "정보 없음"

            // 탄소배출량 업데이트
            val updatedCarbonEmission = originalCarbonEmission * multiplier
            carbonTextView.text = "총 탄소 배출량: ${String.format("%.2f", updatedCarbonEmission)} kg CO2e"
        }
    }

    // 에러 메시지 표시 함수
    private fun displayError(message: String) {
        // 모든 텍스트뷰를 초기화하고 에러 메시지 표시
        productNameTextView.text = message
        servingSizeTextView.text = ""
        energyTextView.text = ""
        fatTextView.text = ""
        carbsTextView.text = ""
        proteinTextView.text = ""
        sugarTextView.text = ""
        sodiumTextView.text = ""
        carbonTextView.text = ""
        missingNutrientsTextView.visibility = View.GONE

        // 로딩바 숨기기
        showLoading(false)
    }

    // 로딩 오버레이 표시/숨기기 함수
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressOverlay.visibility = View.VISIBLE
        } else {
            progressOverlay.visibility = View.GONE
        }
    }

    companion object {
        const val EXTRA_BARCODE = "extra_barcode"
    }
}
