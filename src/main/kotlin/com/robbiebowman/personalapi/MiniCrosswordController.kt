package com.robbiebowman.personalapi

import com.robbiebowman.Crossword
import com.robbiebowman.CrosswordMaker
import com.robbiebowman.Puzzle
import com.robbiebowman.WordIsolator
import com.robbiebowman.claude.*
import com.robbiebowman.personalapi.service.BlobStorageService
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatFunction
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind.KFunction


@RestController
class MiniCrosswordController {

    @Autowired
    private lateinit var blobService: BlobStorageService

    @Value("\${azure_crossword_container_name}")
    private lateinit var containerName: String

    @Value("\${open_ai_api_key}")
    private val openApiKey: String? = null

    @Value("\${claude_api_key}")
    private val claudeApiKey: String? = null

    private val maker = CrosswordMaker()

    @GetMapping("/mini-crossword/create")
    fun createMiniCrossword(
        @RequestParam(value = "date") date: LocalDate = LocalDate.now(),
    ): PuzzleWithClues {
        if (!isWithinAcceptableDateRange(date)) throw Exception("Invalid date")
        val dir = getCurrentDateDirectoryName(date)
        val puzzleFileName = "${dir}/puzzle.json"
        val cluesFileName = "${dir}/clues.json"

        val puzzle = maker.createCrossword().let { crossword ->
                if (crossword == null) throw NoSuchElementException("Couldn't generate a puzzle")
                val (acrossWords, downWords) = WordIsolator.getWords(crossword)
                Puzzle(crossword, acrossWords, downWords)
            }.also {
                blobService.uploadToBlobStorage(containerName, puzzleFileName, it)
            }

        val clues = run {
                val clues = generateClues(puzzle)
                blobService.uploadToBlobStorage(containerName, cluesFileName, clues)
                clues
            }

        return PuzzleWithClues(clues, puzzle)
    }

    @GetMapping("/mini-crossword")
    fun miniCrossword(
        @RequestParam(value = "date") date: LocalDate = LocalDate.now(),
    ): PuzzleWithClues {
        if (!isWithinAcceptableDateRange(date)) throw Exception("Invalid date")
        val dir = getCurrentDateDirectoryName(date)
        val puzzleFileName = "${dir}/puzzle.json"
        val cluesFileName = "${dir}/clues.json"

        val puzzle = blobService.getFromBlobStorage(containerName, puzzleFileName, Puzzle::class.java)
        val clues = blobService.getFromBlobStorage(containerName, cluesFileName, PuzzleClues::class.java)

        if (clues == null || puzzle == null) {
            throw Exception("Puzzle doesn't exist!")
        }

        return PuzzleWithClues(clues, puzzle)
    }

    @PostMapping("/mini-crossword/fill", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun fillCrossword(@RequestBody puzzleGrid: List<List<String>>, response: HttpServletResponse): Map<String, Any> {
        // Validate input
        if (puzzleGrid.isEmpty() || puzzleGrid.any { it.isEmpty() }) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return mapOf("error" to "Invalid puzzle grid")
        }
        val crossword = puzzleGrid.map { it.map { c ->
            val res = when (c) {
                "" -> '.'
                "#" -> ' '
                else -> c.first()
            }
            res
        }.toTypedArray() }.toTypedArray()
        if (crossword.size > 5 || crossword.maxBy { it.size }.size > 5) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return mapOf("error" to "Puzzle grid too large")
        }
        val filledCrossword = maker.createCrossword(
            initialPuzzle = crossword
        )
        if (filledCrossword == null) {
            response.status = HttpServletResponse.SC_NO_CONTENT
            return mapOf("error" to "Couldn't find a solution to this puzzle")
        }

        return mapOf(
            "status" to "success",
            "filledPuzzle" to filledCrossword
        )
    }

    private fun isWithinAcceptableDateRange(date: LocalDate): Boolean {
        return date.isBefore(LocalDate.now().plusDays(5))
                && date.isAfter(LocalDate.now().minusDays(7))
    }

    private fun defineCrosswordClues(clues: PuzzleClues) {
        TODO()
    }

    private fun generateClues(puzzle: Puzzle): PuzzleClues {
        val claudeClient = ClaudeClientBuilder()
            .withApiKey(claudeApiKey!!)
            .withModel("claude-3-5-sonnet-20240620")
            .withTool(::defineCrosswordClues)
            .withSystemPrompt("Given a list of words from the user, write creative and fun crossword clues for each. Avoid making overly simple or direct clues unless the word is obscure. The clues can be silly.")
            .build()
        val response = claudeClient.getChatCompletion(
            listOf(
                SerializableMessage(
                    Role.User,
                    listOf(
                        MessageContent.TextContent(
                            puzzle.acrossWords.plus(puzzle.downWords).joinToString { it.word })
                    )
                )
            )
        )
        val toolUse = response.content.first { it is MessageContent.ToolUse } as MessageContent.ToolUse
        val clues = claudeClient.derserializeToolUse(toolUse.input["clues"]!!, PuzzleClues::class.java)
        return clues
    }

    data class PuzzleWithClues(val clues: PuzzleClues, val puzzle: Puzzle)
    data class PuzzleClues(val clues: List<Clue>)
    data class Clue(val word: String, val clue: String)

    private fun getCurrentDateDirectoryName(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd")
        return formatter.format(date)
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

@ExceptionHandler(Exception::class)
@ResponseStatus(HttpStatus.BAD_REQUEST, code = HttpStatus.BAD_REQUEST)
fun handleException(
    exception: Exception
): ResponseEntity<String?> {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.message)
}