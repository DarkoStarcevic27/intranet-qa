package com.darko.intranetqa.config;

import com.darko.intranetqa.tools.DocumentLookupTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the same tools the chat endpoint uses to any MCP client (an IDE,
 * Claude Desktop, another agent) via spring-ai-starter-mcp-server-webmvc.
 * This is what turns "a service with a chat endpoint" into "a service other
 * AI tools can plug into" — worth mentioning in interviews even if you
 * never wire up a client for it locally.
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider documentTools(DocumentLookupTools documentLookupTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(documentLookupTools)
                .build();
    }
}
