package com.darko.intranetqa.config;

import com.darko.intranetqa.tools.DocumentLookupTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /** Shared with the streaming endpoint, which builds its own tool-free client. */
    public static final String SYSTEM_PROMPT = """
            You are the internal documentation assistant for the company.
            Answer only from the CONTEXT block you are given plus any tool
            results. If the answer isn't in either, say you don't know
            rather than guessing.
            Answer directly and concisely, as if you simply know the
            information. Never say phrases like "based on the provided
            context" or "the context says".
            End every answer that uses a document with one line in the
            form "Source: <file name>".
            """;

    @Bean
    public ChatClient chatClient(ChatModel chatModel, DocumentLookupTools tools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(tools)
                .build();
    }
}
