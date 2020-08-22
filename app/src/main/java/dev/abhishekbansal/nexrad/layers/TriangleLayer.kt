package dev.abhishekbansal.nexrad.layers

import android.content.Context
import android.opengl.GLES20
import dev.abhishekbansal.nexrad.R
import dev.abhishekbansal.nexrad.utils.Shader
import dev.abhishekbansal.nexrad.utils.extensions.rawResToString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TriangleLayer(val context: Context) : Layer {
    private var mTriangle1Vertices: FloatBuffer? = null

    /**
     * How many bytes per float.
     */
    private val mBytesPerFloat = 4

    /**
     * How many elements per vertex.
     */
    private val mStrideBytes: Int = 7 * mBytesPerFloat

    /**
     * Offset of the position data.
     */
    private val mPositionOffset = 0

    /**
     * Size of the position data in elements.
     */
    private val mPositionDataSize = 3

    /**
     * Offset of the color data.
     */
    private val mColorOffset = 3

    /**
     * Size of the color data in elements.
     */
    private val mColorDataSize = 4

    /**
     * This will be used to pass in the transformation matrix.
     */
    private var mMVPMatrixHandle = 0

    /**
     * This will be used to pass in model position information.
     */
    private var mPositionHandle = 0

    /**
     * This will be used to pass in model color information.
     */
    private var mColorHandle = 0

    private val triangleShader by lazy {
        Shader(
            context.rawResToString(R.raw.basic_vertex),
            context.rawResToString(R.raw.basic_fragment)
        )
    }

    override fun prepare() {
        // This triangle is red, green, and blue.
        // Define points for equilateral triangles.
        // This triangle is red, green, and blue.
        val triangle1VerticesData = floatArrayOf( // X, Y, Z,
            // R, G, B, A
            -0.5f, -0.25f, 0.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            0.5f, -0.25f, 0.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.559016994f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f
        )

        // Initialize the buffers.
        mTriangle1Vertices =
            ByteBuffer.allocateDirect(triangle1VerticesData.size * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

        mTriangle1Vertices?.put(triangle1VerticesData)?.position(0)

        // prepare shader
        triangleShader.link(arrayOf("a_Position", "a_Color"))

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(triangleShader.handle, "u_MVPMatrix")
        mPositionHandle = GLES20.glGetAttribLocation(triangleShader.handle, "a_Position")
        mColorHandle = GLES20.glGetAttribLocation(triangleShader.handle, "a_Color")
    }


    override fun draw(mvpMatrix: FloatArray) {
        triangleShader.activate()

        // Pass in the position information

        // Pass in the position information
        mTriangle1Vertices?.position(mPositionOffset)
        GLES20.glVertexAttribPointer(
            mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
            mStrideBytes, mTriangle1Vertices
        )

        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Pass in the color information

        // Pass in the color information
        mTriangle1Vertices?.position(mColorOffset)
        GLES20.glVertexAttribPointer(
            mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
            mStrideBytes, mTriangle1Vertices
        )

        GLES20.glEnableVertexAttribArray(mColorHandle)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
    }

}