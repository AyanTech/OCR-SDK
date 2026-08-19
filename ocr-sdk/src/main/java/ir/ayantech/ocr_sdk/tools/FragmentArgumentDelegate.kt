package ir.ayantech.ocr_sdk.tools

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FragmentArgumentDelegate<T : Any>(private val defaultValue: T? = null) : ReadWriteProperty<Fragment, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val key = property.name
        return thisRef.arguments?.get(key) as? T
            ?: defaultValue
            ?: throw IllegalStateException("Property ${property.name} could not be read from arguments and no default value was provided.")
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        val args = thisRef.arguments ?: Bundle().also { thisRef.arguments = it }
        val key = property.name
        when (value) {
            is String -> args.putString(key, value)
            is Int -> args.putInt(key, value)
            is Long -> args.putLong(key, value)
            is Boolean -> args.putBoolean(key, value)
            is Float -> args.putFloat(key, value)
            is Parcelable -> args.putParcelable(key, value)
            is Serializable -> args.putSerializable(key, value)
            else -> throw IllegalArgumentException("Type ${value.javaClass.canonicalName} is not supported in FragmentArgumentDelegate")
        }
    }
}

class FragmentNullableArgumentDelegate<T : Any?>(private val defaultValue: T? = null) : ReadWriteProperty<Fragment, T?> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? {
        val key = property.name
        return thisRef.arguments?.get(key) as? T ?: defaultValue
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
        val args = thisRef.arguments ?: Bundle().also { thisRef.arguments = it }
        val key = property.name
        if (value == null) {
            args.remove(key)
            return
        }
        when (value) {
            is String -> args.putString(key, value)
            is Int -> args.putInt(key, value)
            is Long -> args.putLong(key, value)
            is Boolean -> args.putBoolean(key, value)
            is Float -> args.putFloat(key, value)
            is Parcelable -> args.putParcelable(key, value)
            is Serializable -> args.putSerializable(key, value)
            else -> throw IllegalArgumentException("Type ${value.javaClass.canonicalName} is not supported in FragmentNullableArgumentDelegate")
        }
    }
}

fun <T : Any> fragmentArgument(defaultValue: T? = null) = FragmentArgumentDelegate(defaultValue)
fun <T : Any?> nullableFragmentArgument(defaultValue: T? = null) = FragmentNullableArgumentDelegate(defaultValue)
