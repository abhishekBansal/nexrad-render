package dev.abhishekbansal.nexrad.utils.extensions

import android.content.Context
import androidx.annotation.RawRes

fun Context.rawResToString(@RawRes resource: Int): String {
    val inputStream = resources.openRawResource(resource)
    return inputStream.readBytes().toString(Charsets.UTF_8)
}