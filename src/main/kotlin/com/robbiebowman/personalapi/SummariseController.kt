package com.robbiebowman.personalapi

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.methods.request.users.UsersListRequest
import com.theokanning.openai.completion.CompletionRequest
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*


@RestController
class SummariseController {

    @Value("\${SLACK_TOKEN}")
    private val slackToken: String? = null

    @Value("\${OPEN_API_KEY}")
    private val openApiKey: String? = null

    private val slack = Slack.getInstance()

    @PostMapping(
        "/summarise",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun summarise(
        @RequestParam params: MultiValueMap<String, String>
    ) {
        val methods: MethodsClient = slack.methods(slackToken)

        val response = methods.conversationsHistory(
            ConversationsHistoryRequest.builder().channel("CPDA1JJQ3").oldest("1679981897").includeAllMetadata(true)
                .build()
        )
        response.messages

        val users = response.messages.mapNotNull { it.user }.distinct()
            .associateWith { methods.usersInfo(UsersInfoRequest.builder().user(it).build()).user?.realName }

        val messages =
            response.messages.sortedBy { it.ts }.joinToString("\n") { (users[it.user] ?: it.username) + ": " + it.text }
        val service = OpenAiService(openApiKey)
        val completionRequest = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(
                listOf(
                    ChatMessage("system", "You are a helpful assistant to a very busy socialite."),
                    ChatMessage("user", "Can you summarize the following convesation please? \n \n $messages"),
                )
            )
            .user("testing")
            .n(1)
            .build()
        service.createChatCompletion(completionRequest).choices.forEach(::println)

        val request = ChatPostMessageRequest.builder()
            .channel("#bot_test") // Use a channel ID `C1234567` is preferable
            .text(":wave: Hi from a bot written in Java!")
            .build()
        methods.chatPostMessage(request)
    }
}