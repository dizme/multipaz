package org.multipaz.utopia.localization

import org.multipaz.doctypes.localization.LocalizedStrings as DoctypesLocalizedStrings
import org.multipaz.utopia.generated.GeneratedTranslations

/**
 * Provides runtime access to localized strings for the multipaz-utopia module.
 *
 * Translations are looked up in the utopia-specific [GeneratedTranslations] map.
 * The lookup falls back to English (`en`) and finally to the key itself when no
 * translation is found.
 *
 * The current locale is resolved through the platform integration provided by
 * the multipaz-doctypes module.
 */
object LocalizedStrings {
    /**
     * Returns the normalized current platform locale used for translation lookup.
     */
    fun getCurrentLocale(): String = DoctypesLocalizedStrings.getCurrentLocale()

    /**
     * Returns all available locales for which translations are bundled.
     */
    fun getAllLocales(): List<String> = GeneratedTranslations.allLanguages

    /**
     * Returns a localized value for [key] using the current platform locale.
     */
    fun getString(key: String): String = getString(key, getCurrentLocale())

    /**
     * Returns a localized value for [key] using the provided [locale].
     */
    fun getString(key: String, locale: String): String {
        val normalizedLocale = normalizeLocale(locale)
        val translations = GeneratedTranslations.getMapForLocale(normalizedLocale)
        return translations[key] ?: GeneratedTranslations.getMapForLocale("en")[key] ?: key
    }

    /**
     * Returns a localized value for [key] using [locale], replacing placeholders
     * in the form `{placeholder}` with values from [placeholders].
     */
    fun getString(key: String, locale: String, placeholders: Map<String, String>): String {
        val template = getString(key, locale)
        return placeholders.entries.fold(template) { acc, (placeholder, value) ->
            acc.replace("{$placeholder}", value)
        }
    }

    private fun normalizeLocale(locale: String): String {
        return when (locale.substringBefore("-").substringBefore("_")) {
            "zh" -> "zh-rCN"
            else -> locale
        }
    }
}
