package mt.parkingqr.ui.home

import android.os.Bundle
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
import mt.parkingqr.databinding.FragmentHomeBinding
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment() {
    private var viewBinding: FragmentHomeBinding? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
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
            val imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                        // TODO: analyse video frame
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
        viewBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}