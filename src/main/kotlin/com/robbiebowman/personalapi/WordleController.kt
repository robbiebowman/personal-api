package com.robbiebowman.personalapi

import com.robbiebowman.wordle.SolverEngine
import com.robbiebowman.wordle.Suggestion
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class WordleController {

    data class ApiSuggestion(val bestGuess: String, val someRemainingAnswers: List<String>, val remainingAnswerCount: Int)
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
}