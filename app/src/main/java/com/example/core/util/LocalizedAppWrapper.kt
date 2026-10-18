package com.example.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.data.preferences.AppLanguage
import java.util.Locale

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

@Composable
fun LocalizedAppWrapper(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val activityOwner = LocalActivityResultRegistryOwner.current ?: baseContext.findComponentActivity()

    val (localizedContext, localizedConfig) = remember(language, baseContext, baseConfiguration) {
        val locale = Locale.forLanguageTag(language.code)
        Locale.setDefault(locale)
        val config = Configuration(baseConfiguration).apply {
            setLocale(locale)
        }
        val configCtx = baseContext.createConfigurationContext(config)
        val wrappedCtx = object : ContextWrapper(baseContext) {
            override fun getResources() = configCtx.resources
            override fun getAssets() = configCtx.assets
        }
        wrappedCtx to config
    }

    if (activityOwner != null) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfig,
            LocalActivityResultRegistryOwner provides activityOwner
        ) {
            content()
        }
    } else {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfig
        ) {
            content()
        }
    }
}
