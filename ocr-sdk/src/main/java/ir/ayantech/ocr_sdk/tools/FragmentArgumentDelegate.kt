package ir.ayantech.ocr_sdk.tools

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FragmentArgumentDelegate<T : Any> @PublishedApi internal constructor(
    private val valueClass: Class<*>?,
    private val defaultValue: T? = null,
) : ReadWriteProperty<Fragment, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T =
        thisRef.arguments?.readArgument(property.name, valueClass) as? T
            ?: defaultValue
            ?: throw IllegalStateException(
                "Property ${property.name} could not be read from arguments and no default value was provided."
            )

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        val args = thisRef.arguments ?: Bundle().also { thisRef.arguments = it }
        args.putArgument(property.name, value)
    }
}

class FragmentNullableArgumentDelegate<T> @PublishedApi internal constructor(
    private val valueClass: Class<*>?,
    private val defaultValue: T? = null,
) : ReadWriteProperty<Fragment, T?> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? =
        thisRef.arguments?.readArgument(property.name, valueClass) as? T ?: defaultValue

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
        val args = thisRef.arguments ?: Bundle().also { thisRef.arguments = it }
        if (value == null) args.remove(property.name) else args.putArgument(property.name, value)
    }
}

private fun Bundle.putArgument(key: String, value: Any) {
    when (value) {
        is String -> putString(key, value)
        is Int -> putInt(key, value)
        is Long -> putLong(key, value)
        is Boolean -> putBoolean(key, value)
        is Float -> putFloat(key, value)
        is Parcelable -> putParcelable(key, value)
        is Serializable -> putSerializable(key, value)
        else -> throw IllegalArgumentException("Type ${value.javaClass.canonicalName} is not supported in FragmentArgumentDelegate")
    }
}

private fun Bundle.readArgument(key: String, valueClass: Class<*>?): Any? {
    if (!containsKey(key)) return null
    return when {
        valueClass == null -> getLegacyArgument(key)
        valueClass == String::class.java -> getString(key)
        valueClass == Int::class.java -> getInt(key)
        valueClass == Long::class.java -> getLong(key)
        valueClass == Boolean::class.java -> getBoolean(key)
        valueClass == Float::class.java -> getFloat(key)
        Parcelable::class.java.isAssignableFrom(valueClass) ->
            BundleCompat.getParcelable(this, key, valueClass.asSubclass(Parcelable::class.java))

        Serializable::class.java.isAssignableFrom(valueClass) ->
            BundleCompat.getSerializable(this, key, valueClass.asSubclass(Serializable::class.java))

        else -> null
    }
}

@Suppress("DEPRECATION")
private fun Bundle.getLegacyArgument(key: String): Any? = get(key)

inline fun <reified T : Any> fragmentArgument(defaultValue: T? = null) =
    FragmentArgumentDelegate(T::class.java, defaultValue)

inline fun <reified T : Any> nullableFragmentArgument(defaultValue: T? = null) =
    FragmentNullableArgumentDelegate(T::class.java, defaultValue)
