package com.example.data.api

enum class LlmProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val apiKeyRequired: Boolean
) {
    LLAMA_CPP(
        displayName = "llama.cpp server",
        defaultBaseUrl = "http://192.168.1.100:8080/v1/",
        defaultModel = "local-model",
        apiKeyRequired = false // optional! works with or without --api-key
    ),
    GEMINI(
        displayName = "google gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/",
        defaultModel = "gemini-2.5-flash",
        apiKeyRequired = true
    ),
    OPENROUTER(
        displayName = "openrouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1/",
        defaultModel = "anthropic/claude-3.5-sonnet",
        apiKeyRequired = true
    ),
    OPENAI(
        displayName = "openai",
        defaultBaseUrl = "https://api.openai.com/v1/",
        defaultModel = "gpt-4o-mini",
        apiKeyRequired = true
    )
}

data class LlmSettings(
    val provider: LlmProvider = LlmProvider.LLAMA_CPP,
    val apiKey: String = "",
    val baseUrl: String = LlmProvider.LLAMA_CPP.defaultBaseUrl,
    val modelName: String = LlmProvider.LLAMA_CPP.defaultModel,
    val systemPrompt: String = "you are a helpful assistant. keep answers concise and conversational for voice synthesis."
)