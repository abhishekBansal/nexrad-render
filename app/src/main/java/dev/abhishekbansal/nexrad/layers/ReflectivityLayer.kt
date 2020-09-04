package dev.abhishekbansal.nexrad.layers

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import com.google.gson.Gson
import dev.abhishekbansal.nexrad.R
import dev.abhishekbansal.nexrad.models.Reflectivity
import dev.abhishekbansal.nexrad.models.reflectivityColors
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
    private var uMvpMatrixHandle: Int = 0

    /**
     * This will be used to pass in color lookup table
     */
    private var uColorMapHandle = 0

    /**
     * This will be used to pass in reflectivity value per vertex
     */
    private var reflectivityHandle = 0

    /**
     * Offset of the position data.
     */
    private val positionOffset = 0

    /**
     * Size of the position data in elements.
     */
    private val positionDataSize = 2

    /**
     * Offset of the reflectivity data in array
     */
    private val reflectivityOffset = positionDataSize

    /**
     * Size of the reflectivity data in elements.
     */
    private val reflectivityDataSize = 1

    /**
     * How many elements per vertex.
     */
    private val strideBytes = (reflectivityDataSize + positionDataSize) * bytesPerFloat


    private val meshShader by lazy {
        Shader(
            context.rawResToString(R.raw.reflectivity_vertex),
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
        uMvpMatrixHandle = GLES20.glGetUniformLocation(meshShader.handle, "u_MVPMatrix")
        uColorMapHandle = GLES30.glGetUniformLocation(meshShader.handle, "u_colorMap")
        positionHandle = GLES20.glGetAttribLocation(meshShader.handle, "a_Position")
        reflectivityHandle = GLES30.glGetAttribLocation(meshShader.handle, "a_Reflectivity")
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

        // Pass in the reflectivity information
        meshVertices.position(reflectivityOffset)
        GLES20.glVertexAttribPointer(
            reflectivityHandle, reflectivityDataSize, GLES20.GL_FLOAT, false,
            strideBytes, meshVertices
        )
        GLES20.glEnableVertexAttribArray(reflectivityHandle)

        // pass in lookup table
        GLES30.glUniform3fv(uColorMapHandle, 83, reflectivityColors, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, meshSize)
        meshShader.deactivate()
    }

    private fun generateVertexData() {
        val data = getData(context)

        // per vertex data = 2xy + 1R
        val perVertexElements = positionDataSize + reflectivityDataSize
        // since we don't want go beyond last gate hence gate-1
        val totalVertices = data.azimuth.size * (data.gates.size - 1)
        // Each vertex is part of 6 triangles,
        meshSize = totalVertices * perVertexElements * 6
        val reflectivityMesh = FloatArray(meshSize)

        var index = 0
        val nAzimuth = data.azimuth.size
        val nGates = data.gates.size
        for (r in 0 until nGates - 1) {
            // since we want distances along longitude we approximate that by dividing approximate meters in a degree
            val radius1 = data.gates[r] / meterPerDegree
            val radius2 = data.gates[r + 1] / meterPerDegree
            for (angleIndex in 0 until nAzimuth) {
                val reflectivity = data.reflectivity[angleIndex][r]
                // early exit whenever possible
                if (reflectivity <= 0) continue

                // vertex 1 x
                val angle = data.azimuth[angleIndex]
                val angle2 = if (angleIndex != nAzimuth - 1) {
                    data.azimuth[angleIndex + 1]
                } else {
                    data.azimuth[0]
                }

                // precalculate coordinates
                val r1SinTheta1 = radius1 * sin(Math.toRadians(angle.toDouble())).toFloat()
                val r1SinTheta2 = radius1 * sin(Math.toRadians(angle2.toDouble())).toFloat()
                val r2SinTheta2 = radius2 * sin(Math.toRadians(angle2.toDouble())).toFloat()
                val r2SinTheta1 = radius2 * sin(Math.toRadians(angle.toDouble())).toFloat()

                val r1CosTheta1 = radius1 * cos(Math.toRadians(angle.toDouble())).toFloat()
                val r1CosTheta2 = radius1 * cos(Math.toRadians(angle2.toDouble())).toFloat()
                val r2CosTheta2 = radius2 * cos(Math.toRadians(angle2.toDouble())).toFloat()
                val r2CosTheta1 = radius2 * cos(Math.toRadians(angle.toDouble())).toFloat()

                ///// Begin Triangle 1 /////
                // r1 theta1
                reflectivityMesh[index++] = r1SinTheta1
                reflectivityMesh[index++] = r1CosTheta1
                // reflectivity information for triangle 1 vertex 1
                reflectivityMesh[index++] = reflectivity

                // r2 theta1
                reflectivityMesh[index++] = r2SinTheta1
                reflectivityMesh[index++] = r2CosTheta1
                reflectivityMesh[index++] = reflectivity

                // r1 theta2
                reflectivityMesh[index++] = r1SinTheta2
                reflectivityMesh[index++] = r1CosTheta2
                reflectivityMesh[index++] = reflectivity

                ///// Begin Triangle 2 /////

                // r1 theta2
                reflectivityMesh[index++] = r1SinTheta2
                reflectivityMesh[index++] = r1CosTheta2
                reflectivityMesh[index++] = reflectivity

                // r2 theta2
                reflectivityMesh[index++] = r2SinTheta2
                reflectivityMesh[index++] = r2CosTheta2
                // color information for triangle 1 vertex 3
                reflectivityMesh[index++] = reflectivity

                // r2 theta1
                reflectivityMesh[index++] = r2SinTheta1
                reflectivityMesh[index++] = r2CosTheta1
                // color information for triangle 1 vertex 2
                reflectivityMesh[index++] = reflectivity
            }
        }
        Timber.i("index: $index, meshsize: $meshSize")
        meshSize = index

        // Initialize the buffers.
        meshVertices = ByteBuffer.allocateDirect(reflectivityMesh.size * bytesPerFloat)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        meshVertices.put(reflectivityMesh)?.position(0)
    }

    private fun getData(context: Context): Reflectivity {
        return Gson().fromJson(context.rawResToString(R.raw.l2_data), Reflectivity::class.java)
    }
}