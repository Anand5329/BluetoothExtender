package com.example.bluetoothextender

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import kotlin.reflect.KClass

class BluetoothUtils {
    companion object {
        inline fun <reified T : Parcelable> getDataFromIntent(
            intent: Intent?,
            key: String,
            clazz: KClass<T>
        ): T? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return intent?.getParcelableExtra(key, clazz.java)
            } else {
                val extra: Parcelable? = intent?.getParcelableExtra<Parcelable>(key)
                if (extra is T?) {
                    return extra
                } else {
                    throw ClassCastException("Extra data Couldn't be cast to type ${T::class}, of type ${extra!!::class.qualifiedName}")
                }
            }
        }

        inline fun <reified T : Parcelable> getArrayDataFromIntent(
            intent: Intent?,
            key: String,
            clazz: KClass<T>
        ): Array<T>? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return intent?.getParcelableArrayExtra(key, clazz.java)
            } else {
                val extraArray: Array<Parcelable>? = intent?.getParcelableArrayExtra(key)
                if (extraArray == null) {
                    return extraArray
                }
                for (extra in extraArray) {
                    if (extra !is T) {
                        throw ClassCastException("Constituent item couldn't be cast to type ${T::class}, of type ${extra::class.qualifiedName}")
                    }
                }
                return extraArray as Array<T>
            }
        }
    }
}