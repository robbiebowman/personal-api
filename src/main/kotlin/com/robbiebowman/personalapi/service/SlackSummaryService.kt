package com.robbiebowman.personalapi.service

import com.azure.security.keyvault.secrets.SecretClient
import com.google.gson.Gson
import com.robbiebowman.personalapi.util.DateUtils
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostEphemeralRequest
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest
import com.slack.api.methods.request.conversations.ConversationsJoinRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.model.Message
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.defaultClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

@Service
class SlackSummaryService {

    // GPT engine info
    private val gptEngine = "gpt-3.5-turbo-16k"
    private val maxTokens = 16_000
    private val maxLengthExplanation =
        "This may be due to the high length of the conversation. Rest assured the forthcoming edition of GPT will increase the max length 8 fold. "

    @Value("\${open_ai_api_key}")
    private val openApiKey: String? = null

    @Value("\${open_ai_system_prompt}")
    private val openAiSystemPrompt: String? = null

    @Value("\${open_ai_user_instruction}")
    private val openAiUserInstruction: String? = null

    private lateinit var secretClient: SecretClient

    private val slack = Slack.getInstance()

    @Autowired
    fun setSecretClient(secretClient: SecretClient) {
        this.secretClient = secretClient
    }

    fun postSummary(
        accessToken: String, channel: String, requestingUser: String, duration: Duration, postPublicly: Boolean
    ) {
        val client = slack.methods(accessToken)
        client.conversationsJoin(ConversationsJoinRequest.builder().channel(channel).build())
        val messages =
            getMessagesSinceTime(client, channel = channel, since = Instant.now().minusMillis(duration.toMillis()))
        if (messages.isEmpty()) {
            sendUserMessage(client, requestingUser, channel, text = "There haven't been any messages!")
            return
        }
        val users = getUserToNameMap(client, messages)
        val formattedMessages = getFormattedMessages(messages, users)
        val gpt = OpenAiService(openApiKey, Duration.ofSeconds(30))
        val summary = try {
            getSummary(gpt, formattedMessages, requestingUser)
        } catch (e: Exception) {
            println(e.message)
            println("Error: ${Gson().toJson(e)}")
            "Unfortunately GPT wasn't able to summarise the conversation." + (if (formattedMessages.split(' ').size > maxTokens * 0.75) maxLengthExplanation else "")
        }

        val humanReadableDuration = DateUtils.durationToHuman(duration)
        if (postPublicly) {
            val explainer =
                "Summary of messages in the past $humanReadableDuration, as requested by <@$requestingUser>\n\n"
            val request = ChatPostMessageRequest.builder().channel(channel).text("$explainer$summary").build()
            client.chatPostMessage(request)
        } else {
            val explainer = "Summary of messages in the past $humanReadableDuration\n\n"
            sendUserMessage(client, requestingUser, channel, "$explainer$summary")
        }
    }

    fun authenticate(slackSigningSecret: String, signature: String, timestamp: Long, rawBody: String) {
        // Validate timestamp
        val now = Instant.now().epochSecond
        if (abs(now - timestamp) > 60 * 5) throw Exception()

        // Validate signature
        val secretKeySpec = SecretKeySpec(slackSigningSecret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val toHash = "v0:$timestamp:$rawBody"
        val hash =
            mac.doFinal(toHash.toByteArray()).joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        val key = "v0=$hash"
        if (key != signature) throw Exception() // Invalid signature
    }

    private fun getSummary(gpt: OpenAiService, formattedMessages: String, requestingUser: String): String {
        val completionRequest = ChatCompletionRequest.builder().model(gptEngine).messages(
            listOf(
                ChatMessage(
                    "system",
                    openAiSystemPrompt
                ),
                ChatMessage(
                    "user", "$openAiUserInstruction \n \n $formattedMessages"
                ),
            )
        ).user(requestingUser).n(1).build()
        val result = gpt.createChatCompletion(completionRequest)
        val content = result.choices.first().message.content
        val contentWithNiceBulletPoints = content.replace(Regex("^- ", RegexOption.MULTILINE), "• ")
        return contentWithNiceBulletPoints
    }

    private fun sendUserMessage(client: MethodsClient, user: String, channel: String, text: String) {
        val request = ChatPostEphemeralRequest.builder().channel(channel).text(text).user(user).build()
        client.chatPostEphemeral(request)
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