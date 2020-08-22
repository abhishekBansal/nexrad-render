package dev.abhishekbansal.nexrad.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class NexradGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var scaleFactor = 1.0f
    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(context, ScaleListener())
    }

    private var renderer: Renderer? = null

    var previousX = 0f
    var previousY = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1 && scaleGestureDetector.onTouchEvent(event)) {
            return true
        } else {
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (min((event.x - previousX)/width, 0.4f))/scaleFactor
                    val dy = (min((event.y - previousY)/width, 0.4f))/scaleFactor
                    (renderer as? PannableRenderer)?.setPan(dx, -dy)
                }

                MotionEvent.ACTION_UP -> {

                }
            }

            previousX = event.x
            previousY = event.y
        }

        return true
    }

    override fun setRenderer(renderer: Renderer?) {
        this.renderer = renderer
        super.setRenderer(renderer)
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor

            // don't let the object get too small or too large.
            scaleFactor = max(0.1f, min(scaleFactor, 100.0f))
            Timber.d("scale: $scaleFactor")
            (renderer as? ScalableRenderer)?.setScaleFactor(scaleFactor)
            return true
        }
    }
}