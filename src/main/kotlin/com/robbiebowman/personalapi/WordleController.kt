package com.robbiebowman.personalapi

import com.robbiebowman.wordle.SolverEngine
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class WordleController {

    data class ApiSuggestion(
        val bestGuess: String,
        val someRemainingAnswers: List<String>,
        val remainingAnswerCount: Int
    )

    private val solver = SolverEngine()

    @GetMapping("/wordle")
    fun wordle(
        @RequestParam(value = "guesses", defaultValue = "") guesses: List<String>,
        @RequestParam(value = "results", defaultValue = "") results: List<String>,
        @RequestParam(value = "hardMode", defaultValue = "false") hardMode: Boolean,
    ): ApiSuggestion {
        val bestGuess = solver.getSuggestion(guesses, results, hardMode)
        return ApiSuggestion(bestGuess.word, bestGuess.possibleAnswers.take(20), bestGuess.possibleAnswers.size)
    }

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND, code = HttpStatus.NOT_FOUND)
    fun handleNoSuchElementFoundException(
        exception: NoSuchElementException
    ): ResponseEntity<String?>? {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(exception.message)
    }
}