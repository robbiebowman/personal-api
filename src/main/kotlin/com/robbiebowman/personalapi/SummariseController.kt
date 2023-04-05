package com.robbiebowman.personalapi

import com.azure.security.keyvault.secrets.SecretClient
import com.robbiebowman.personalapi.service.AsyncService
import com.robbiebowman.personalapi.service.SlackSummaryService
import com.robbiebowman.personalapi.util.DateUtils
import com.slack.api.Slack
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
class SummariseController {

    @Value("\${slack_signing_secret}")
    private val slackSigningSecret: String? = null

    @Value("\${slack_client_id}")
    private val slackClientId: String? = null

    @Value("\${slack_client_secret}")
    private val slackClientSecret: String? = null

    private lateinit var asyncService: AsyncService;

    private lateinit var slackSummaryService: SlackSummaryService;

    private lateinit var secretClient: SecretClient

    @Autowired
    fun setAsyncService(asyncService: AsyncService) {
        this.asyncService = asyncService
    }

    @Autowired
    fun setSlackSummaryService(slackSummaryService: SlackSummaryService) {
        this.slackSummaryService = slackSummaryService
    }

    @Autowired
    fun setSecretClient(secretClient: SecretClient) {
        this.secretClient = secretClient
    }

    @GetMapping("/summarise/redirect")
    fun summariseOauth(
        @RequestParam params: MultiValueMap<String, String>,
    ) {
        try {
            val slackTempAuthCode = params["code"]!!.first()
            val response = Slack.getInstance().methods().oauthV2Access(
                OAuthV2AccessRequest.builder().clientId(slackClientId).clientSecret(slackClientSecret)
                    .redirectUri("https://www.robbiebowman.com/tireless-assistant").code(slackTempAuthCode).build()
            )
            secretClient.setSecret(response.team.id, response.accessToken)
        } catch (e: Exception) {
            println(e.message)
            println(e)
        }
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
        slackSummaryService.authenticate(slackSigningSecret!!, signature, timestamp, httpEntity.body!!)

        // Get relevant form fields
        val channel = params["channel_id"]!!.first()
        val requestingUser = params["user_id"]!!.first()
        val teamId = params["team_id"]!!.first()
        val arguments = params["text"]!!.firstOrNull() ?: "6 hours"
        val postPublicly = arguments.endsWith("publicly")
        val duration = DateUtils.parseHuman(arguments.replace("publicly", ""))

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
                            "text": "Give me a few moments to read all the messages in the past ${
                DateUtils.durationToHuman(
                    duration
                )
            } and write a summary."
                        }
                    }
            	]
            }
        """.trimIndent()
        )
        response.writer.flush()

        asyncService.process {
            val accessToken = secretClient.getSecret(teamId).value
            slackSummaryService.postSummary(accessToken, channel, requestingUser, duration, postPublicly)
        }
    }
}
