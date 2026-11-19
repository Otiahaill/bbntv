package com.example.prtv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.example.prtv.R

/**
 * Colours come from brand/res/values/brand_colors.xml. Dark-only (TV-style); keep backgrounds dark and
 * text light. Run scripts/check_brand.py to verify contrast.
 */
@Composable
fun BrandTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = colorResource(R.color.brand_primary),
        onPrimary = colorResource(R.color.brand_on_primary),
        secondary = colorResource(R.color.brand_secondary),
        background = colorResource(R.color.brand_background),
        onBackground = colorResource(R.color.brand_on_background),
        surface = colorResource(R.color.brand_surface),
        onSurface = colorResource(R.color.brand_on_background),
        surfaceVariant = colorResource(R.color.brand_surface_variant),
        onSurfaceVariant = colorResource(R.color.brand_on_surface_variant),
        error = colorResource(R.color.brand_error)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
