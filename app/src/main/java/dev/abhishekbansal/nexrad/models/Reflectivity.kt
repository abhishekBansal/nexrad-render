package dev.abhishekbansal.nexrad.models

@Suppress("ArrayInDataClass")
data class Reflectivity(val azimuth: FloatArray, val gates: FloatArray, val reflectivity: Array<FloatArray>)