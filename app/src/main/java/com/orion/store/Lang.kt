package com.orion.store

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language choice. An empty tag means "follow the system".
 * On Android 13+ the choice is handed to the system per-app language service (the supported
 * languages are declared in res/xml/locales_config.xml); on older versions it is kept in
 * SharedPreferences and applied by MainActivity.attachBaseContext.
 */
object Lang {
    private const val PREFS = "orion_language"
    private const val KEY = "tag"

    fun tag(ctx: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = ctx.getSystemService(LocaleManager::class.java)?.applicationLocales
            return if (locales == null || locales.isEmpty) "" else locales.get(0)?.toLanguageTag() ?: ""
        }
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
    }

    fun set(ctx: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
        }
    }

    fun isArabic(ctx: Context): Boolean {
        val selected = tag(ctx)
        if (selected.isNotEmpty()) return selected.startsWith("ar", ignoreCase = true)
        return ctx.resources.configuration.locales.get(0)?.language.equals("ar", ignoreCase = true)
    }

    /** Wrap a base context with the stored locale. Only the pre-Android 13 path needs this. */
    fun wrap(ctx: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return ctx
        val tag = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        if (tag.isEmpty()) return ctx
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return ctx.createConfigurationContext(config)
    }
}

/** The activity hosting this context, if any (used to restart the UI after a language change). */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
