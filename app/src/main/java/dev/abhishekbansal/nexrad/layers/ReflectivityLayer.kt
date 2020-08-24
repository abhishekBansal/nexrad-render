package dev.abhishekbansal.nexrad.layers

import android.content.Context
import android.opengl.GLES20
import com.google.gson.Gson
import dev.abhishekbansal.nexrad.R
import dev.abhishekbansal.nexrad.models.Reflectivity
import dev.abhishekbansal.nexrad.utils.Shader
import dev.abhishekbansal.nexrad.utils.extensions.rawResToString
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class ReflectivityLayer(val context: Context) : Layer {

    /**
     * Approximation of distance covered along longitude or latitude in a per degree
     */
    private val meterPerDegree = 111111

    /**
     * How many bytes per float.
     */
    private val bytesPerFloat = 4

    /**
     * This will be used to pass in model position information.
     */
    private var positionHandle: Int = 0

    /**
     * This will be used to pass in the transformation matrix.
     */
    private var mvpMatrixHandle: Int = 0

    /**
     * This will be used to pass in model color information.
     */
    private var colorHandle = 0

    /**
     * Offset of the position data.
     */
    private val positionOffset = 0

    /**
     * Size of the position data in elements.
     */
    private val positionDataSize = 3

    /**
     * Offset of the color data.
     */
    private val colorOffset = 3

    /**
     * Size of the color data in elements.
     */
    private val colorDataSize = 3

    /**
     * How many elements per vertex.
     */
    private val strideBytes = (colorDataSize + positionDataSize) * bytesPerFloat


    private val meshShader by lazy {
        Shader(
            context.rawResToString(R.raw.basic_vertex),
            context.rawResToString(R.raw.basic_fragment)
        )
    }

    private var meshSize = 0

    /**
     * Store our model data in a float buffer.
     */
    private lateinit var meshVertices: FloatBuffer

    override fun prepare() {
        generateVertexData()
        meshShader.link(arrayOf("a_Position", "a_Reflectivity"))

        // Set program handles. These will later be used to pass in values to the program.
        // Set program handles. These will later be used to pass in values to the program.

        // Set program handles. These will later be used to pass in values to the program.
        mvpMatrixHandle = GLES20.glGetUniformLocation(meshShader.handle, "u_MVPMatrix")
        positionHandle = GLES20.glGetAttribLocation(meshShader.handle, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(meshShader.handle, "a_Color")
    }

    override fun draw(mvpMatrix: FloatArray) {
        // Pass in the position information
        meshShader.activate()

        // Pass in the position information
        meshVertices.position(positionOffset)
        GLES20.glVertexAttribPointer(
            positionHandle, positionDataSize, GLES20.GL_FLOAT, false,
            strideBytes, meshVertices
        )

        GLES20.glEnableVertexAttribArray(positionHandle)

        // Pass in the color information

        // Pass in the color information
        meshVertices.position(colorOffset)
        GLES20.glVertexAttribPointer(
            colorHandle, colorDataSize, GLES20.GL_FLOAT, false,
            strideBytes, meshVertices
        )

        GLES20.glEnableVertexAttribArray(colorHandle)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, meshSize)
        meshShader.deactivate()
    }

    private fun generateVertexData() {
        val data = getData(context)

        // per vertex data = 3xyz + 3rgb
        val perVertexElements = 3 + 3
        // since we don't want go beyond last gate hence gate-1
        val totalVertices = data.azimuth.size * (data.gates.size - 1)
        // Each vertex is part of 6 triangles,
        meshSize = totalVertices * perVertexElements * 6
        val reflectivityMesh = FloatArray(meshSize)

        var index = 0
        val nAzimuth = data.azimuth.size
        val nGates = data.gates.size
        for (r in 0 until nGates - 1) {
            val radius1 = data.gates[r] / meterPerDegree
            val radius2 = data.gates[r + 1] / meterPerDegree
            for (angleIndex in 0 until nAzimuth) {
                // vertex 1 x
                val angle = data.azimuth[angleIndex]
                val angle2 = if (angleIndex != nAzimuth - 1) {
                    data.azimuth[angleIndex + 1]
                } else {
                    data.azimuth[0]
                }
                val reflectivity = data.reflectivity[angleIndex][r]

                ///// Begin Triangle 1 /////
                // r1 theta1
                reflectivityMesh[index++] = radius1 * sin(Math.toRadians(angle.toDouble()))
                    .toFloat()
                reflectivityMesh[index++] = radius1 * cos(Math.toRadians(angle.toDouble()))
                    .toFloat()
                reflectivityMesh[index++] = 0.0f
                // color information for triangle 1 vertex 1
                reflectivityMesh[index++] = getColor(reflectivity)[0]
                reflectivityMesh[index++] = getColor(reflectivity)[1]
                reflectivityMesh[index++] = getColor(reflectivity)[2]

                // r2 theta1
                reflectivityMesh[index++] = radius2 * sin(Math.toRadians(angle.toDouble()))
                    .toFloat()
                reflectivityMesh[index++] = radius2 * cos(Math.toRadians(angle.toDouble()))
                    .toFloat()
                reflectivityMesh[index++] = 0.0f
                // color information for triangle 1 vertex 2
                reflectivityMesh[index++] = getColor(reflectivity)[0]
                reflectivityMesh[index++] = getColor(reflectivity)[1]
                reflectivityMesh[index++] = getColor(reflectivity)[2]

                // r1 theta2
                reflectivityMesh[index++] =
                    radius1 * sin(Math.toRadians(angle2.toDouble())).toFloat()
                reflectivityMesh[index++] =
                    radius1 * cos(Math.toRadians(angle2.toDouble())).toFloat()
                reflectivityMesh[index++] = 0.0f
                // color information for triangle 1 vertex 3
                reflectivityMesh[index++] = getColor(reflectivity)[0]
                reflectivityMesh[index++] = getColor(reflectivity)[1]
                reflectivityMesh[index++] = getColor(reflectivity)[2]


                ///// Begin Triangle 2 /////

                // r1 theta2
                reflectivityMesh[index++] = radius1 * sin(Math.toRadians(angle2.toDouble()))
                    .toFloat()
                reflectivityMesh[index++] = radius1 * cos(Math.toRadians(angle2.toDouble()))
                    .toFloat()
                reflectivityMesh[index++] = 0.0f
                // color information for triangle 1 vertex 3
                reflectivityMesh[index++] = getColor(reflectivity)[0]
                reflectivityMesh[index++] = getColor(reflectivity)[1]
                reflectivityMesh[index++] = getColor(reflectivity)[2]

                // r2 theta2
                reflectivityMesh[index++] =
                    radius2 * sin(Math.toRadians(angle2.toDouble())).toFloat()
                reflectivityMesh[index++] =
                    radius2 * cos(Math.toRadians(angle2.toDouble())).toFloat()
                reflectivityMesh[index++] = 0.0f
                // color information for triangle 1 vertex 3
                reflectivityMesh[index++] = getColor(reflectivity)[0]
                reflectivityMesh[index++] = getColor(reflectivity)[1]
                reflectivityMesh[index++] = getColor(reflectivity)[2]

                // r2 theta1
                // r2 theta1
                reflectivityMesh[index++] =
                    radius2 * sin(Math.toRadians(angle.toDouble())).toFloat()
                reflectivityMesh[index++] =
                    radius2 * cos(Math.toRadians(angle.toDouble())).toFloat()
                reflectivityMesh[index++] = 0.0f
                // color information for triangle 1 vertex 2
                reflectivityMesh[index++] = getColor(reflectivity)[0]
                reflectivityMesh[index++] = getColor(reflectivity)[1]
                reflectivityMesh[index++] = getColor(reflectivity)[2]
            }
        }

        Timber.i("index: $index, meshsize: $meshSize")

        // Initialize the buffers.
        meshVertices = ByteBuffer.allocateDirect(reflectivityMesh.size * bytesPerFloat)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        meshVertices.put(reflectivityMesh)?.position(0)
    }

    private fun getData(context: Context): Reflectivity {
        return Gson().fromJson(context.rawResToString(R.raw.l3_data), Reflectivity::class.java)
    }

    private fun getColor(reflectivity: Float): FloatArray {
        return when {
            reflectivity <= 0 -> {
                floatArrayOf(0.0f, 0.0f, 0.0f)
            }
            reflectivity < 10 -> {
                floatArrayOf(1.0f, 1.0f, 0.0f)
            }
            reflectivity < 15 -> {
                floatArrayOf(1.0f, 0.0f, 1.0f)
            }
            reflectivity < 20 -> {
                floatArrayOf(0.0f, 0.0f, 1.0f)
            }
            reflectivity < 25 -> {
                floatArrayOf(.5f, 1.0f, 0.0f)
            }
            reflectivity < 30 -> {
                floatArrayOf(0.0f, 1.0f, 0.0f)
            }
            reflectivity < 35 -> {
                floatArrayOf(0.0f, 1.0f, 1.0f)
            }
            else -> {
                floatArrayOf(1.0f, .2f, .2f)
            }
        }
    }
}