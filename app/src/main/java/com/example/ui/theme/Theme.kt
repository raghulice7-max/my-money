package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

fun Modifier.bounceClickable(
    interactionSource: MutableInteractionSource? = null,
    indication: androidx.compose.foundation.Indication? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) = composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "clickScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = actualInteractionSource,
            indication = indication ?: androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

private val DarkColorScheme =
  darkColorScheme(
    primary = PineDarkPrimary,
    onPrimary = PineDarkOnPrimary,
    primaryContainer = PineDarkPrimaryContainer,
    onPrimaryContainer = PineDarkOnPrimaryContainer,
    secondary = SageDarkSecondary,
    onSecondary = SageDarkOnSecondary,
    secondaryContainer = SageDarkSecondaryContainer,
    onSecondaryContainer = SageDarkOnSecondaryContainer,
    tertiary = ClayDarkTertiary,
    onTertiary = ClayDarkOnTertiary,
    tertiaryContainer = ClayDarkTertiaryContainer,
    onTertiaryContainer = ClayDarkOnTertiaryContainer,
    background = ObsidianBackground,
    onBackground = ObsidianOnBackground,
    surface = ObsidianSurface,
    onSurface = ObsidianOnSurface,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PinePrimary,
    onPrimary = PineOnPrimary,
    primaryContainer = PinePrimaryContainer,
    onPrimaryContainer = PineOnPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = SageOnSecondaryContainer,
    tertiary = ClayTertiary,
    onTertiary = ClayOnTertiary,
    tertiaryContainer = ClayTertiaryContainer,
    onTertiaryContainer = ClayOnTertiaryContainer,
    background = SandBackground,
    onBackground = SandOnBackground,
    surface = CreamSurface,
    onSurface = SandOnSurface,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = SandOnSurfaceVariant
  )

fun getCustomTypography(fontFamily: FontFamily, scale: Float): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (57 * scale).sp,
            lineHeight = (64 * scale).sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (45 * scale).sp,
            lineHeight = (52 * scale).sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (36 * scale).sp,
            lineHeight = (44 * scale).sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (32 * scale).sp,
            lineHeight = (40 * scale).sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (22 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.5.sp
        )
    )
}

@Composable
fun MyApplicationTheme(
  appTheme: String = "SYSTEM",
  appFontFamily: String = "DEFAULT",
  appFontSize: String = "NORMAL",
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (appTheme) {
    "LIGHT" -> false
    "DARK" -> true
    else -> isSystemInDarkTheme()
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val fontFamily = when (appFontFamily) {
    "SANS_SERIF" -> FontFamily.SansSerif
    "SERIF" -> FontFamily.Serif
    "MONOSPACE" -> FontFamily.Monospace
    else -> FontFamily.Default
  }

  val scale = when (appFontSize) {
    "SMALL" -> 0.85f
    "LARGE" -> 1.15f
    else -> 1.0f
  }

  val customTypography = remember(fontFamily, scale) {
    getCustomTypography(fontFamily, scale)
  }

  MaterialTheme(colorScheme = colorScheme, typography = customTypography, content = content)
}
