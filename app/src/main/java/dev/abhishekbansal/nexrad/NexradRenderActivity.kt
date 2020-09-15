package dev.abhishekbansal.nexrad

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import dev.abhishekbansal.nexrad.databinding.ActivityNexradRenderBinding
import dev.abhishekbansal.nexrad.renderer.FPSChangeListener
import dev.abhishekbansal.nexrad.renderer.NexradRenderer
import timber.log.Timber

class NexradRenderActivity : Activity() {
    /**
     * Hold a reference to our GLSurfaceView
     */
    private val binding: ActivityNexradRenderBinding by lazy { ActivityNexradRenderBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Timber.plant(Timber.DebugTree())

        // Check if the system supports OpenGL ES 2.0.
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            binding.glSurfaceView.setEGLContextClientVersion(2)

            // Set the renderer to our
            // demo renderer, defined below.
            val renderer = NexradRenderer(this)
            binding.glSurfaceView.setRenderer(renderer)
            renderer.fpsListener = FPSChangeListener {
                runOnUiThread { binding.fpsTv.text = "$it" }
            }
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return
        }
    }

    override fun onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume()
        binding.glSurfaceView.onResume()
    }

    override fun onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause()
        binding.glSurfaceView.onPause()
    }
}