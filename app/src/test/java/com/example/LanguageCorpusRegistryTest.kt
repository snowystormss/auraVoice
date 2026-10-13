package com.example

import com.example.engine.LanguageCorpusRegistry
import org.junit.Assert.*
import org.junit.Test

class LanguageCorpusRegistryTest {

    @Test
    fun testJapaneseLanguageCorpusRegistration() {
        val japanese = LanguageCorpusRegistry.getByCode("ja-JP")
        assertEquals("ja-JP", japanese.code)
        assertEquals("JA", japanese.shortCode)
        assertEquals("Japanese", japanese.displayName)
        assertEquals("日本語", japanese.nativeName)
        assertEquals("ATR 503 Phonetically Balanced", japanese.corpusName)
        assertTrue("Japanese training sentences should not be empty", japanese.trainingSentences.isNotEmpty())
        assertTrue("Japanese sample phrases should not be empty", japanese.samplePhrases.isNotEmpty())
        assertTrue(japanese.previewSentence.contains("桜"))
    }

    @Test
    fun testSpanishLanguageCorpusRegistration() {
        val spanish = LanguageCorpusRegistry.getByCode("es-ES")
        assertEquals("es-ES", spanish.code)
        assertEquals("ES", spanish.shortCode)
        assertEquals("Spanish", spanish.displayName)
        assertEquals("Español", spanish.nativeName)
        assertEquals("Corpus Fonético Español", spanish.corpusName)
        assertTrue(spanish.trainingSentences.isNotEmpty())
    }

    @Test
    fun testAllSupportedLanguagesHaveValidDataAndNoEmojis() {
        val supported = LanguageCorpusRegistry.supportedLanguages
        assertTrue("Should support at least 6 languages", supported.size >= 6)

        // Emoji regex check: ensures no emojis in codes, shortCodes, or display names
        val emojiRegex = Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+")

        for (lang in supported) {
            assertFalse("ShortCode must not contain emojis: ${lang.shortCode}", emojiRegex.containsMatchIn(lang.shortCode))
            assertFalse("DisplayName must not contain emojis: ${lang.displayName}", emojiRegex.containsMatchIn(lang.displayName))
            assertFalse("CorpusName must not contain emojis: ${lang.corpusName}", emojiRegex.containsMatchIn(lang.corpusName))
            assertTrue("Code must have format xx-XX: ${lang.code}", lang.code.contains("-"))
            assertTrue("Training sentences must not be empty", lang.trainingSentences.isNotEmpty())
            assertTrue("Sample phrases must not be empty", lang.samplePhrases.isNotEmpty())
        }
    }

    @Test
    fun testFallbackForUnknownCode() {
        val fallback = LanguageCorpusRegistry.getByCode("unknown-XX")
        assertEquals("en-US", fallback.code)
        assertEquals("EN", fallback.shortCode)
    }

    @Test
    fun testShortCodeLookup() {
        val ja = LanguageCorpusRegistry.getByCode("JA")
        assertEquals("ja-JP", ja.code)

        val es = LanguageCorpusRegistry.getByCode("ES")
        assertEquals("es-ES", es.code)
    }
}
