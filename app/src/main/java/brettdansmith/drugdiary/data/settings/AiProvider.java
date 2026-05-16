package brettdansmith.drugdiary.data.settings;

public enum AiProvider {
    OPENAI("openai", "ChatGPT (OpenAI)", "gpt-5.2"),
    ANTHROPIC("anthropic", "Claude (Anthropic)", "claude-opus-4-20250514"),
    GEMINI("gemini", "Gemini (Google)", "gemini-2.5-pro"),
    DEEPSEEK("deepseek", "DeepSeek", "deepseek-reasoner"),
    XAI("xai", "Grok (xAI)", "grok-2-1212"),
    GROQ("groq", "Groq", "llama-4-scout-17b-16e-instruct"),
    MISTRAL("mistral", "Mistral AI", "mistral-large-latest"),
    PERPLEXITY("perplexity", "Perplexity Sonar", "sonar-pro"),
    OPENROUTER("openrouter", "OpenRouter (All Models)", "openai/gpt-5");

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
