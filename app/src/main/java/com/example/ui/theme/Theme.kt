package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StreamsxainyColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = PureWhite,
    primaryContainer = LavenderAccent,
    onPrimaryContainer = ObsidianBg,
    secondary = LavenderAccent,
    onSecondary = ObsidianBg,
    tertiary = AmberStar,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = PureWhite,
    surface = ObsidianBg,
    onSurface = PureWhite,
    surfaceVariant = SurfacePoster,
    onSurfaceVariant = MutedText,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfacePoster,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = ChipBackground,
    outline = SubtleOutline,
    outlineVariant = HairlineBorder,
    scrim = ScrimDark
)

@Composable
fun StreamsxainyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StreamsxainyColorScheme,
        typography = StreamsxainyTypography,
        shapes = StreamsxainyShapes,
        content = content
    )
}
