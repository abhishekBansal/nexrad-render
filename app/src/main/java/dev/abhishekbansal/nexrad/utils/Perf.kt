package dev.abhishekbansal.nexrad.utils

import timber.log.Timber

fun measureTime(message: String, codeBlock: () -> Unit) {
    val time = System.currentTimeMillis()
    codeBlock()
    Timber.i("$message ${System.currentTimeMillis() - time}")
}