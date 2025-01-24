package com.example.carbon3.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.carbon3.databinding.FragmentDashboardBinding
import com.example.carbon3.network.FoodApiService
import com.example.carbon3.network.NetworkModule
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalGetImage
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // CameraX
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var barcodeScanner: BarcodeScanner

    // Retrofit API Service
    private val foodApiService: FoodApiService by lazy {
        NetworkModule.foodApiService
    }

    private val serviceKey = "%2FhtUdqATshOhd%2Fy8WDXw%2FQmL%2F8YHT86WpwNOjkwAFrxVmaoqsgFG%2BsMw%2FJHmkXREz7MtYrzPTXqLseQsZGxTyQ%3D%3D"

    companion object {
        private const val TAG = "DashboardFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                processImageProxy(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "카메라 바인딩 실패", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes[0]
                        val barcodeValue = barcode.rawValue ?: ""
                        getNutriProcessInfoFromApi(barcodeValue)
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "바코드 스캔 실패", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun getNutriProcessInfoFromApi(barcodeValue: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = foodApiService.getNutriProcessInfo(
                    serviceKey = serviceKey,
                    pageNo = 1,
                    numOfRows = 5,
                    type = "json"
                )
                withContext(Dispatchers.Main) {
                    if (response.response?.header?.resultCode == "00") {
                        val items = response.response.body?.items?.item
                        if (!items.isNullOrEmpty()) {
                            val firstItem = items[0]
                            binding.scanText.text = """
                                제품명: ${firstItem.prdlstNm}
                                총내용량: ${firstItem.totalCnt}
                                상세페이지: ${firstItem.pageUrl}
                            """.trimIndent()
                        } else {
                            binding.scanText.text = "아이템이 없습니다."
                        }
                    } else {
                        val msg = response.response?.header?.resultMsg
                        binding.scanText.text = "resultCode != 00: $msg"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "API 호출 실패", e)
                withContext(Dispatchers.Main) {
                    binding.scanText.text = "API 실패: ${e.message}"
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
