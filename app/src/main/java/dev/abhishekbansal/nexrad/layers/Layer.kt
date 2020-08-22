package dev.abhishekbansal.nexrad.layers

interface Layer {
    fun prepare()
    fun draw(mvpMatrix: FloatArray)
}