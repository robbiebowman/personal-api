package com.robbiebowman.personalapi

import com.azure.security.keyvault.secrets.SecretClient
import com.robbiebowman.CrosswordMaker
import com.robbiebowman.Puzzle
import com.robbiebowman.WordIsolator
import com.robbiebowman.personalapi.service.BlobStorageService
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatFunction
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@RestController
class MiniCrosswordController {

    @Autowired
    private lateinit var secretClient: SecretClient

    @Autowired
    private lateinit var blobService: BlobStorageService

    @Value("\${azure_crossword_container_name}")
    private lateinit var containerName: String

    @Value("\${open_ai_api_key}")
    private val openApiKey: String? = null

    private val maker = CrosswordMaker()

    @GetMapping("/mini-crossword")
    fun miniCrossword(): PuzzleWithClues {
        val dir = getCurrentDateDirectoryName()
        val puzzleFileName = "${dir}/puzzle.json"
        val cluesFileName = "${dir}/clues.json"

        val puzzle = blobService.getFromBlobStorage(containerName, puzzleFileName, Puzzle::class.java)
            ?: maker.createCrossword().let { crossword ->
                if (crossword == null) throw NoSuchElementException("Couldn't generate a puzzle")
                val (acrossWords, downWords) = WordIsolator.getWords(crossword)
                Puzzle(crossword, acrossWords, downWords)
            }.also {
                blobService.uploadToBlobStorage(containerName, puzzleFileName, it)
            }

        val clues = blobService.getFromBlobStorage(containerName, cluesFileName, PuzzleClues::class.java)
            ?: run {
                val clues = generateClues(puzzle)
                blobService.uploadToBlobStorage(containerName, cluesFileName, clues)
                clues
            }

        return PuzzleWithClues(clues, puzzle)
    }

    private fun generateClues(puzzle: Puzzle): PuzzleClues {
        val gpt = OpenAiService(openApiKey, Duration.ofSeconds(30))
        val crosswordClueFunction = ChatFunction.builder()
            .name("define_crossword_clues")
            .description("")
            .executor(PuzzleClues::class.java) {}
            .build()
        val req = ChatCompletionRequest
            .builder()
            .model("gpt-4")
            .functions(listOf(crosswordClueFunction))
            .functionCall(ChatCompletionRequest.ChatCompletionRequestFunctionCall("auto"))
            .messages(
                listOf(
                    ChatMessage(
                        "system",
                        "Given a list of words from the user, create challenging crossword clues for each. The more common the word, the more difficult and cryptic the clue should be. Avoid clues about the word being within another word. Try to make a challenging and fun set of clues."
                    ),
                    ChatMessage("user", puzzle.acrossWords.plus(puzzle.downWords).joinToString { it.word })
                )
            )
            .build()
        val response = gpt.createChatCompletion(req)
        val clues = response.choices.first().message.functionCall.arguments["clues"].map {
            Clue(
                it["word"].asText(),
                it["clue"].asText()
            )
        }
        return PuzzleClues(clues)
    }

    data class PuzzleWithClues(val clues: PuzzleClues, val puzzle: Puzzle)
    data class PuzzleClues(val clues: List<Clue>)
    data class Clue(val word: String, val clue: String)

    private fun getCurrentDateDirectoryName(): String {
        val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd")
        return formatter.format(LocalDate.now())
    }
}

@ExceptionHandler(NoSuchElementException::class)
@ResponseStatus(HttpStatus.NOT_FOUND, code = HttpStatus.NOT_FOUND)
fun handleNoSuchElementFoundException(
    exception: NoSuchElementException
): ResponseEntity<String?> {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(exception.message)

}