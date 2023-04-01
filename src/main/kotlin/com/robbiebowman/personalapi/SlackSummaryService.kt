package com.robbiebowman.personalapi

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostEphemeralRequest
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.model.Message
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class SlackSummaryService {

    private val gptEngine = "gpt-3.5-turbo"

    @Value("\${SLACK_TOKEN}")
    private val slackToken: String? = null

    @Value("\${OPEN_API_KEY}")
    private val openApiKey: String? = null

    private val slack = Slack.getInstance()

    fun postSummary(channel: String, requestingUser: String?, duration: Duration, postPublicly: Boolean) {
        val client = slack.methods(slackToken)
        val messages = getMessagesSinceTime(client, channel = channel, since = Instant.now().minusMillis(duration.toMillis()))
        val users = getUserToNameMap(client, messages)
        val formattedMessages = getFormattedMessages(messages, users)
        val gpt = OpenAiService(openApiKey)
        val summary = getSummary(gpt, formattedMessages)

        if (postPublicly) {
            val request = ChatPostMessageRequest.builder().channel(channel).text(summary).build()
            client.chatPostMessage(request)
        } else {
            val request = ChatPostEphemeralRequest.builder().channel(channel).text(summary).user(requestingUser).build()
            client.chatPostEphemeral(request)
        }
    }

    private fun getSummary(gpt: OpenAiService, formattedMessages: String): String {
        val completionRequest = ChatCompletionRequest.builder().model(gptEngine).messages(
            listOf(
                ChatMessage(
                    "system",
                    "You are a helpful assistant, helping someone get a summary of the messages they've missed."
                ),
                ChatMessage(
                    "user", "Briefly summarize the following conversation: \n \n $formattedMessages"
                ),
            )
        ).user("testing").n(1).build()
        return gpt.createChatCompletion(completionRequest).choices.first().message.content
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