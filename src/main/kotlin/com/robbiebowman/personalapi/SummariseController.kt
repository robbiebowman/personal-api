package com.robbiebowman.personalapi

import com.robbiebowman.personalapi.auth.SlackAuthenticator.authenticate
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostEphemeralRequest
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.model.Message
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.time.Instant
import javax.servlet.http.HttpServletRequest


@RestController
class SummariseController {

    @Value("\${SLACK_TOKEN}")
    private val slackToken: String? = null

    @Value("\${OPEN_API_KEY}")
    private val openApiKey: String? = null

    @Value("\${SLACK_SIGNING_SECRET}")
    private val slackSigningSecret: String? = null

    private val slack = Slack.getInstance()

    @PostMapping("/summarise", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @ResponseBody
    fun summarise(
        @RequestParam params: MultiValueMap<String, String>,
        httpRequest: HttpServletRequest,
        httpEntity: HttpEntity<String>
    ) {
        println("Got headers: X-Slack-Request-Timestamp: ${httpEntity.headers["X-Slack-Request-Timestamp"]}")
        println("Got headers: X-Slack-Signature: ${httpEntity.headers["X-Slack-Signature"]}")
        println("Got a body: ${httpEntity.body!!}")
        authenticate(slackSigningSecret!!, httpRequest, httpEntity.body!!)
        val client: MethodsClient = slack.methods(slackToken)
        val messages = getMessagesSinceTime(client, channel = "CPDA1JJQ3", since = Instant.ofEpochSecond(1679981897L))
        val users = getUserToNameMap(client, messages)
        val formattedMessages = getFormattedMessages(messages, users)
        val service = OpenAiService(openApiKey)
        val summary = getSummary(service, formattedMessages)

        val request =
            ChatPostEphemeralRequest.builder().channel("#bot_test") // Use a channel ID `C1234567` is preferable
                .text(summary).user("UAV7CQMCY").build()
        client.chatPostEphemeral(request)
    }

    private fun getSummary(service: OpenAiService, formattedMessages: String): String {
        val completionRequest = ChatCompletionRequest.builder().model("gpt-3.5-turbo").messages(
            listOf(
                ChatMessage("system", "You are a helpful assistant to a very busy socialite."),
                ChatMessage(
                    "user", "Briefly summarize the following conversation: \n \n $formattedMessages"
                ),
            )
        ).user("testing").n(1).build()
        return service.createChatCompletion(completionRequest).choices.first().message.content
    }

    private fun getFormattedMessages(
        messages: List<Message>, users: Map<String, String?>
    ): String {
        val formattedMessages =
            messages.sortedBy { it.ts }.joinToString("\n") { (users[it.user] ?: it.username) + ": " + it.text }
        return users.keys.fold(formattedMessages) { m, u -> val username = users[u]; m.replace("<@$u>", username ?: u) }
    }

    private fun getUserToNameMap(
        client: MethodsClient, messages: List<Message>
    ): Map<String, String?> {
        val messagingUsers = messages.mapNotNull { it.user }
        val mentionedUsers =
            Regex("<@(\\w+)>").findAll(messages.joinToString { it.text }).map { it.groupValues[1] }.toList()
        return messagingUsers.plus(mentionedUsers).distinct()
            .associateWith { client.usersInfo(UsersInfoRequest.builder().user(it).build()).user?.realName }
    }

    private fun getMessagesSinceTime(client: MethodsClient, channel: String, since: Instant): List<Message> {
        val response = client.conversationsHistory(
            ConversationsHistoryRequest.builder().channel(channel).oldest(since.epochSecond.toString()).build()
        )
        return response.messages
    }
}