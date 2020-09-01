package dev.abhishekbansal.nexrad.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import dev.abhishekbansal.nexrad.layers.Layer
import dev.abhishekbansal.nexrad.layers.ReflectivityLayer
import dev.abhishekbansal.nexrad.layers.TriangleLayer
import timber.log.Timber
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES30 is used instead.
 */
class NexradRenderer(private val context: Context) : GLSurfaceView.Renderer,
    ScalableRenderer, PannableRenderer {

    private var scaleFactor: Float = 1.0f
    private var translateX: Float = 0.0f
    private var translateY: Float = 0.0f
    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private val mModelMatrix = FloatArray(16)

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private val mViewMatrix = FloatArray(16)

    /**
     * Store the projection matrix. This is used to project the scene onto a 2D viewport.
     */
    private val mProjectionMatrix = FloatArray(16)

    private var eyeZ = 2f
    private var eyeX = 0f
    private var eyeY = 0f

    private var lookX = 0f
    private var lookY = 0f
    private var lookZ = 0f

    // Position the eye behind the origin.
    // Set our up vector. This is where our head would be pointing were we holding the camera.
    private val upX = 0.0f
    private val upY = 1.0f
    private val upZ = 0.0f

    var fpsListener: FPSChangeListener? = null
    val layers = mutableListOf<Layer>()
    /**
     * Allocate storage for the final combined matrix. This will be passed into the meshShader program.
     */
    private val mMVPMatrix = FloatArray(16)

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        // Set the background clear color to gray.
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ)
        val layer = ReflectivityLayer(context)
        layer.prepare()
        layers.add(layer)
    }

    override fun onSurfaceChanged(glUnused: GL10, width: Int, height: Int) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES30.glViewport(0, 0, width, height)

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        val ratio = width.toFloat() / height
        val left = -ratio
        val bottom = -1.0f
        val top = 1.0f
        val near = 1.0f
        val far = 10.0f

        Matrix.frustumM(mProjectionMatrix, 0, left, ratio, bottom, top, near, far);
    }

    var lastTime: Long = 0L
    var nFrames = 0
    override fun onDrawFrame(glUnused: GL10) {
        val currentTime = System.currentTimeMillis()
        nFrames++

        if (currentTime - lastTime >= 1000) {
            fpsListener?.onFPSChanged(nFrames)
            nFrames = 0
            lastTime = currentTime
        }

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
        Matrix.setIdentityM(mModelMatrix, 0)

        Matrix.scaleM(mModelMatrix, 0, scaleFactor, scaleFactor, 0.0f)
        Matrix.translateM(mModelMatrix, 0, translateX, translateY, 0f)

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)

        layers.forEach {
            it.draw(mMVPMatrix)
        }
    }

    override fun setScaleFactor(scaleFactor: Float) {
        this.scaleFactor = scaleFactor
        Timber.d("Scale: $scaleFactor")
    }

    override fun setPan(dx: Float, dy: Float) {
        Timber.d("Translate: $dx $dy")
        translateX += dx
        translateY += dy
    }
}
