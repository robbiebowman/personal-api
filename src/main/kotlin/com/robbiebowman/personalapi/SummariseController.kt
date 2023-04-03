package com.robbiebowman.personalapi

import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.security.keyvault.secrets.SecretClientBuilder
import com.robbiebowman.personalapi.auth.SlackAuthenticator
import com.robbiebowman.personalapi.service.AsyncService
import com.robbiebowman.personalapi.service.SlackSummaryService
import com.slack.api.Slack
import com.slack.api.methods.request.oauth.OAuthAccessRequest
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest
import com.slack.api.webhook.Payload
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
class SummariseController {

    @Value("\${slack_signing_secret}")
    private val slackSigningSecret: String? = null

    @Value("\${slack_token}")
    private val slackToken: String? = null

    @Value("\${slack_client_id}")
    private val slackClientId: String? = null

    @Value("\${slack_client_secret}")
    private val slackClientSecret: String? = null

    @Value("\${azure_app_client_id}")
    private val azureAppClientId: String? = null

    @Value("\${azure_app_password}")
    private val azureAppPassword: String? = null

    @Value("\${azure_app_tenant_id}")
    private val azureAppTenantId: String? = null

    @Value("\${azure_key_vault_url}")
    private val azureKeyVaultUrl: String? = null

    private lateinit var asyncService: AsyncService;

    private lateinit var slackSummaryService: SlackSummaryService;

    @Autowired
    fun setAsyncService(asyncService: AsyncService) {
        this.asyncService = asyncService
    }

    @Autowired
    fun setSlackSummaryService(slackSummaryService: SlackSummaryService) {
        this.slackSummaryService = slackSummaryService
    }

    @GetMapping("/summarise/redirect")
    fun summariseOauth(
        @RequestParam params: MultiValueMap<String, String>,
    ) {
        println("Got to redirect!")
        val secretClient = SecretClientBuilder()
            .vaultUrl(azureKeyVaultUrl)
            .credential(
                ClientSecretCredentialBuilder().tenantId(azureAppTenantId).clientId(azureAppClientId)
                    .clientSecret(azureAppPassword).build()
            )
            .buildClient()

        println("Getting code param")
        val slackTempAuthCode = params["code"]!!.first()
        println("Code: $slackTempAuthCode")

        val response = Slack.getInstance().methods().oauthV2Access(
            OAuthV2AccessRequest.builder().clientId(slackClientId).clientSecret(slackClientSecret).code(slackTempAuthCode).build()
        )
        println("Got response: team id: ${response.team.id}, access token: ${response.accessToken}")

        secretClient.setSecret(response.team.id, response.accessToken)
        println("Saved access token!")
    }

    @PostMapping("/summarise", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @ResponseBody
    fun summarise(
        @RequestParam params: MultiValueMap<String, String>,
        httpRequest: HttpServletRequest,
        httpEntity: HttpEntity<String>,
        response: HttpServletResponse
    ) {
        // Authenticate request
        val timestamp = httpRequest.getHeader("X-Slack-Request-Timestamp").toLong()
        val signature = httpRequest.getHeader("X-Slack-Signature")
        //SlackAuthenticator.authenticate(slackSigningSecret!!, signature, timestamp, httpEntity.body!!)

        // Get relevant form fields
        val channel = params["channel_id"]!!.first()
        val requestingUser = params["user_id"]!!.first()
        val arguments = params["text"]!!.firstOrNull() ?: "6 hours"
        val postPublicly = arguments.endsWith("publicly")
        val duration = parseHuman(arguments.replace("publicly", ""))

        response.status = HttpStatus.OK.value()
        response.contentType = "application/json"
        response.writer.write(
            """
            {
                "blocks": [
            		{
            			"type": "section",
            			"text": {
            				"type": "mrkdwn",
            				"text": "*Got it.*"
            			}
            		},
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": " Give me a few moments to read the messages in the last few hours and write a summary."
                        }
                    }
            	]
            }
        """.trimIndent()
        )
        response.writer.flush()

        asyncService.process {
            slackSummaryService.postSummary(channel, requestingUser, duration, postPublicly)
        }
    }

    // Thank you Andreas
    // https://stackoverflow.com/a/52230282/1256019
    fun parseHuman(text: String): Duration {
        val m: Matcher = Pattern.compile(
            "\\s*(?:(\\d+)\\s*(?:days?|d))?" +
                    "\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h))?" +
                    "\\s*(?:(\\d+)\\s*(?:minutes?|mins?|m))?" +
                    "\\s*(?:(\\d+)\\s*(?:seconds?|secs?|s))?" +
                    "\\s*", Pattern.CASE_INSENSITIVE
        )
            .matcher(text)
        if (!m.matches()) throw IllegalArgumentException("Not valid duration: $text")
        val days = (if (m.start(1) == -1) 0 else m.group(1).toInt())
        val hours = (if (m.start(2) == -1) 0 else m.group(2).toInt())
        val mins = (if (m.start(3) == -1) 0 else m.group(3).toInt())
        val secs = (if (m.start(4) == -1) 0 else m.group(4).toInt())
        return Duration.ofSeconds(((days * 24 + hours) * 60L + mins) * 60L + secs)
    }
}
