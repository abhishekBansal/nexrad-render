package dev.abhishekbansal.nexrad.utils

import android.opengl.GLES30
import timber.log.Timber


class Shader(vertexShaderSource: String, fragmentShaderSource: String) {
    enum class ShaderType(val glValue: Int) {
        VERTEX(GLES30.GL_VERTEX_SHADER),
        FRAGMENT(GLES30.GL_FRAGMENT_SHADER)
    }

    private var vertexShaderHandle = 0
    private var fragmentShaderHandle = 0
    var handle = 0

    init {
        vertexShaderHandle = compile(ShaderType.VERTEX, vertexShaderSource)
        fragmentShaderHandle = compile(ShaderType.FRAGMENT, fragmentShaderSource)
    }

    private fun compile(type: ShaderType, source: String): Int {
        var shaderHandle = GLES30.glCreateShader(type.glValue)

        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES30.glShaderSource(shaderHandle, source)

            // Compile the shader.
            GLES30.glCompileShader(shaderHandle)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Timber.e("Error compiling shader:  ${GLES30.glGetShaderInfoLog(shaderHandle)}")
                GLES30.glDeleteShader(shaderHandle)
                shaderHandle = 0
            }
        }

        if (shaderHandle == 0) {
            throw RuntimeException("Error creating shader.")
        }

        return shaderHandle
    }

    fun link(attributes: Array<String>? = null): Int {
        handle = GLES30.glCreateProgram()

        if (handle != 0) {
            // Bind the vertex shader to the program.
            GLES30.glAttachShader(handle, vertexShaderHandle)

            // Bind the fragment shader to the program.
            GLES30.glAttachShader(handle, fragmentShaderHandle)

            // Bind attributes
            attributes?.forEachIndexed { index, attribute ->
                GLES30.glBindAttribLocation(handle, index, attribute)
            }

            // Link the two shaders together into a program.
            GLES30.glLinkProgram(handle)

            // Get the link status.
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(handle, GLES30.GL_LINK_STATUS, linkStatus, 0)

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Timber.e("Error compiling program: %s", GLES30.glGetProgramInfoLog(handle))
                GLES30.glDeleteProgram(handle)
                handle = 0
            }
        }

        if (handle == 0) {
            throw RuntimeException("Error creating program.")
        }

        return handle
    }

    fun activate() {
        GLES30.glUseProgram(handle)
    }

    fun deactivate() {
        GLES30.glUseProgram(handle)
    }
}