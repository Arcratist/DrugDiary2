package brettdansmith.drugdiary.data.settings;

public enum AiProvider {
    OPENAI("openai", "ChatGPT (OpenAI)", "gpt-4o-mini"),
    ANTHROPIC("anthropic", "Claude (Anthropic)", "claude-3-5-sonnet-latest"),
    GEMINI("gemini", "Gemini (Google)", "gemini-2.5-flash-lite"),
    DEEPSEEK("deepseek", "DeepSeek", "deepseek-chat"),
    XAI("xai", "Grok (xAI)", "grok-4"),
    GROQ("groq", "Groq", "meta-llama/llama-4-scout-17b-16e-instruct"),
    MISTRAL("mistral", "Mistral AI", "mistral-large-latest"),
    PERPLEXITY("perplexity", "Perplexity Sonar", "sonar"),
    OPENROUTER("openrouter", "OpenRouter (All Models)", "meta-llama/llama-3.1-70b-instruct");

    private final String preferenceValue;
    private final String displayName;
    private final String defaultModel;

    AiProvider(String preferenceValue, String displayName, String defaultModel) {
        this.preferenceValue = preferenceValue;
        this.displayName = displayName;
        this.defaultModel = defaultModel;
    }

    public String preferenceValue() {
        return preferenceValue;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public static AiProvider fromPreference(String value) {
        for (AiProvider provider : values()) {
            if (provider.preferenceValue.equals(value)) {
                return provider;
            }
        }
        return OPENAI;
    }
}

