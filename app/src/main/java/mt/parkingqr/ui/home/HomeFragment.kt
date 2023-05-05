package mt.parkingqr.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import mt.parkingqr.databinding.FragmentHomeBinding
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment() {
    private var viewBinding: FragmentHomeBinding? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = createBarcodeScanner()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewBinding = FragmentHomeBinding.inflate(inflater, container, false).apply {
            homeViewModel.text.observe(viewLifecycleOwner) {
                textHome.text = it
            }
            startCamera(this)
        }

        return viewBinding!!.root
    }

    private fun createBarcodeScanner(): BarcodeScanner {
        return BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun startCamera(binding: FragmentHomeBinding) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // Which camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Image analysis
            imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                        // TODO: analyse video frame
//                        Log.d("ImageAnalysis", "Image proxy: format=${imageProxy.format}, w x h=${imageProxy.width} x ${imageProxy.height}, rotation=${imageProxy.imageInfo.rotationDegrees}, timestamp=${imageProxy.imageInfo.timestamp}")
                        imageProxy.image?.let { mediaImage ->
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(image).addOnSuccessListener { barcodes ->
//                                Log.d("Barcodes", "barcodes size: ${barcodes.size}")
                                if(barcodes.size == 1) {
                                    Log.d("Barcodes", "Finally barcode: ${barcodes[0].rawValue}")
                                    cameraProvider.unbindAll()
                                    binding.previewView.visibility = View.GONE
                                    binding.textHome.visibility = View.VISIBLE
                                    binding.textHome.text = barcodes[0].rawValue
                                }
                            }
                        }
                        imageProxy.close()
                    })
                }

            // Bind to lifecycle
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        },
        ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageAnalysis.clearAnalyzer()
        viewBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}