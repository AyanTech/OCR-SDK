package ir.ayantech.ocr_sdk.tools

import android.graphics.Color
import android.os.Build
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.databinding.OcrActivityBinding

internal class OcrEdgeToEdgeHelper(
    private val activity: ComponentActivity
) {
    @ColorInt
    private val statusBarColor =
        ContextCompat.getColor(activity, R.color.ocr_header_background)

    @ColorInt
    private val navigationBarColor =
        ContextCompat.getColor(activity, R.color.ocr_fragment_background)

    private val useDarkStatusBarIcons = ColorUtils.calculateLuminance(statusBarColor) > 0.5
    private val useDarkNavigationBarIcons =
        ColorUtils.calculateLuminance(navigationBarColor) > 0.5

    @ColorInt
    private val legacyNavigationBarColor = if (useDarkNavigationBarIcons) {
        ColorUtils.blendARGB(navigationBarColor, Color.BLACK, 0.7f)
    } else {
        navigationBarColor
    }

    fun enable() {
        activity.enableEdgeToEdge(
            statusBarStyle = styleFor(statusBarColor, useDarkStatusBarIcons),
            navigationBarStyle = navigationBarStyle()
        )
    }

    fun applyInsets(binding: OcrActivityBinding) {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = useDarkStatusBarIcons
            isAppearanceLightNavigationBars = useDarkNavigationBarIcons
        }
        binding.statusBarBackground.setBackgroundColor(statusBarColor)
        binding.navigationBarBackground.setBackgroundColor(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                navigationBarColor
            } else {
                legacyNavigationBarColor
            }
        )
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

    private fun styleFor(@ColorInt color: Int, useDarkIcons: Boolean): SystemBarStyle =
        if (useDarkIcons) {
            SystemBarStyle.light(color, color)
        } else {
            SystemBarStyle.dark(color)
        }

    private fun navigationBarStyle(): SystemBarStyle =
        if (useDarkNavigationBarIcons) {
            SystemBarStyle.light(navigationBarColor, legacyNavigationBarColor)
        } else {
            SystemBarStyle.dark(navigationBarColor)
        }
}
