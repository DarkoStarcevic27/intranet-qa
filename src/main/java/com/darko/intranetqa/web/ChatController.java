package com.darko.intranetqa.web;

import com.darko.intranetqa.config.ChatClientConfig;
import com.darko.intranetqa.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat endpoints. Retrieval is scoped per request: the
 * {@link QuestionAnswerAdvisor#FILTER_EXPRESSION} advisor parameter is set
 * from the caller's own document groups (see {@link AuthenticatedUser}),
 * so two users asking the same question can get answers built from
 * different, correctly-permissioned context.
 *
 * <p>{@code /api/chat} returns the whole answer at once; {@code /api/chat/stream}
 * returns it token by token as server-sent events. The filter expression is
 * computed on the request thread <em>before</em> streaming starts, because the
 * security context is not available on the reactive threads that emit tokens.
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final ChatClient streamingChatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final AuthenticatedUser authenticatedUser;

    public ChatController(ChatClient chatClient, ChatModel chatModel,
                          VectorStore vectorStore, AuthenticatedUser authenticatedUser) {
        this.chatClient = chatClient;
        // Tool methods read the caller from a thread-local security context, which does not
        // follow a streamed response onto other threads. The only tool (list documents) is
        // already covered by the UI's document list, so the streaming client has no tools.
        this.streamingChatClient = ChatClient.builder(chatModel)
                .defaultSystem(ChatClientConfig.SYSTEM_PROMPT)
                .build();
        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, Authentication authentication) {
        String filterExpression = authenticatedUser.filterExpression(authentication);

        String answer = chatClient.prompt()
                .user(request.question())
                .advisors(questionAnswerAdvisor)
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression))
                .call()
                .content();

        return new ChatResponse(answer);
    }

    /**
     * Each event's data is a small JSON object ({@code {"t":"..."}}) rather than raw
     * text: SSE parsers strip a leading space from a data line, which would glue words
     * together when a token starts with a space.
     */
    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Token>> stream(@RequestBody ChatRequest request, Authentication authentication) {
        String filterExpression = authenticatedUser.filterExpression(authentication);

        return streamingChatClient.prompt()
                .user(request.question())
                .advisors(questionAnswerAdvisor)
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression))
                .stream()
                .content()
                .map(text -> ServerSentEvent.<Token>builder().data(new Token(text)).build())
                .onErrorResume(e -> Flux.just(ServerSentEvent.<Token>builder()
                        .event("error")
                        .data(new Token(e.getMessage() == null ? "The model call failed." : e.getMessage()))
                        .build()));
    }

    public record ChatRequest(@NotBlank String question) {
    }

    public record ChatResponse(String answer) {
    }

    public record Token(String t) {
    }
}
