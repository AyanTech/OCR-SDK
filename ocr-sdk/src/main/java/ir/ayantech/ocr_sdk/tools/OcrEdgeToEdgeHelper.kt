package ir.ayantech.ocr_sdk.tools

import android.util.TypedValue
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.databinding.OcrActivityBinding

internal class OcrEdgeToEdgeHelper(
    private val activity: ComponentActivity
) {
    @ColorInt
    private val statusBarColor = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)

    @ColorInt
    private val navigationBarColor =
        ContextCompat.getColor(activity, R.color.ocr_fragment_background)

    fun enable() {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(statusBarColor),
            navigationBarStyle = SystemBarStyle.light(
                navigationBarColor,
                navigationBarColor
            )
        )
    }

    fun applyInsets(binding: OcrActivityBinding) {
        binding.statusBarBackground.setBackgroundColor(statusBarColor)
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainerFl) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            binding.statusBarBackground.updateLayoutParams<ViewGroup.LayoutParams> {
                height = systemBars.top
            }
            binding.navigationBarBackground.updateLayoutParams<ViewGroup.LayoutParams> {
                height = systemBars.bottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.fragmentContainerFl)
    }

    @ColorInt
    private fun resolveThemeColor(@AttrRes attribute: Int): Int {
        val value = TypedValue()
        if (!activity.theme.resolveAttribute(attribute, value, true)) {
            return ContextCompat.getColor(activity, R.color.purple_500)
        }
        return value.resourceId
            .takeIf { it != 0 }
            ?.let { ContextCompat.getColor(activity, it) }
            ?: value.data
    }
}
