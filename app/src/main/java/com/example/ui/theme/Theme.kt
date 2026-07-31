package com.example.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.LocalView

private val EditorialDarkColorScheme = darkColorScheme(
    primary = StadiumGreen,
    onPrimary = DarkBgMain,
    primaryContainer = StadiumGreen.copy(alpha = 0.20f),
    onPrimaryContainer = StadiumGreen,
    secondary = GoldAccent,
    onSecondary = DarkBgMain,
    tertiary = InfoTeal,
    onTertiary = DarkBgMain,
    background = DarkBgMain,
    onBackground = CleanWhite,
    surface = DarkBgSurface,
    onSurface = CoolSlate,
    surfaceVariant = OuterSpace,
    onSurfaceVariant = MutedGrey,
    error = WicketCrimson,
    onError = CleanWhite
)

private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node() {}
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the premium premium dark athletic look
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    SideEffect {
        try {
            view.isHapticFeedbackEnabled = false
        } catch (_: Exception) {
            // Safe fallback if not running on platform standard view
        }
    }

    CompositionLocalProvider(
        LocalIndication provides NoIndication,
        LocalRippleConfiguration provides null
    ) {
        MaterialTheme(
            colorScheme = EditorialDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
