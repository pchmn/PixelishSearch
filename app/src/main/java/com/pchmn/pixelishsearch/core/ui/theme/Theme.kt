package com.pchmn.pixelishsearch.core.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Material 3 theme with Dynamic Color (Material You) on Android 12+.
 * Automatically picks up wallpaper colors to match the system theme.
 */

private val FallbackLight = lightColorScheme()
private val FallbackDark = darkColorScheme()

val GoogleSans = FontFamily(
    Font(DeviceFontFamilyName("google-sans"), weight = FontWeight.Normal),
    Font(DeviceFontFamilyName("google-sans"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("google-sans"), weight = FontWeight.Bold),
)

/**
 * Pre-resolve [GoogleSans] into Compose's (process-global) font cache so the
 * first text layout doesn't pay the device-font lookup
 * (`DeviceFontFamilyName("google-sans")` → platform `Typeface`) on the heavy
 * post-first-frame composition — which, while it runs, blocks the main thread
 * from processing `WINDOW_FOCUS_CHANGED` and therefore delays the IME. The
 * typeface caches behind `createFontFamilyResolver` are shared across resolver
 * instances, so warming one here warms the resolver the composition uses. See
 * `docs/performance-analysis.md`.
 */
fun warmUpGoogleSans(scope: CoroutineScope, context: Context) {
    val resolver = createFontFamilyResolver(context.applicationContext)
    scope.launch(Dispatchers.IO) {
        runCatching { resolver.preload(GoogleSans) }
    }
}

private fun appTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun PixelishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> FallbackDark
        else -> FallbackLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(GoogleSans),
        content = content,
    )
}
