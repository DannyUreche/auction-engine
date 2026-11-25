package com.DannyU.auction_engine.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

// The one Anthropic API client of the app (AI listing assistant, AI moderation).
// Without ANTHROPIC_API_KEY (app.ai.anthropic-api-key) the AI features are off.
@Component
public class ClaudeClient {

    public static final String MODEL = "claude-opus-5-5";

    private final AnthropicClient client;

    public ClaudeClient(@Value("${app.ai.anthropic-api-key:}") String apiKey) {
        this.client = apiKey == null || apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(90))
                .build();
    }

    public boolean isEnabled() {
        return client != null;
    }

    public AnthropicClient get() {
        if (client == null) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not set");
        }
        return client;
    }
}
