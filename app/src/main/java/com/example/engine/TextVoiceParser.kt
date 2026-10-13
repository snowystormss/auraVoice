package com.example.engine

object TextVoiceParser {
    private val sentenceBoundaryRegex = Regex("(?<=[.!?\\n])\\s+")
    private val markdownStripRegex = Regex("[*_`#~>\\[\\]]")

    fun extractCompleteSentences(buffer: StringBuilder): List<String> {
        val raw = buffer.toString()
        val parts = sentenceBoundaryRegex.split(raw)
        if (parts.size <= 1) return emptyList()

        val completed = mutableListOf<String>()
        for (i in 0 until parts.lastIndex) {
            val cleaned = cleanForSpeech(parts[i])
            if (cleaned.isNotBlank()) {
                completed.add(cleaned)
            }
        }

        buffer.clear()
        buffer.append(parts.last())
        return completed
    }

    fun cleanForSpeech(text: String): String {
        return text
            .replace(markdownStripRegex, "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}