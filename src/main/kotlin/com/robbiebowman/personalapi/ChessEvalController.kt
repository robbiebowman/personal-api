package com.robbiebowman.personalapi

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import kotlin.random.Random


@RestController
class ChessEvalController {

    val numChessEvalLines = 100_000

    @Value("\${chess_daily_seed}")
    private val chessDailySeed: String? = null

    private val csv = csvReader()

    enum class Difficulties {
        Easy, Medium, Hard
    }

    data class ChessEvaluation(val fen: String, val evaluation: String)

    @GetMapping("/chess-evals")
    fun getRandomChessEvaluations(
        @RequestParam("difficulty") difficulty: Difficulties? = Difficulties.Medium
    ): ChessEvaluation {
        val lineNum = Random.nextInt(numChessEvalLines)
        val filePath = when(difficulty) {
            Difficulties.Easy -> "static/chess/easy-puzzles.csv"
            Difficulties.Medium, null -> "static/chess/medium-puzzles.csv"
            Difficulties.Hard -> "static/chess/hard-puzzles.csv"
        }
        val streamReader = InputStreamReader(this.javaClass.classLoader.getResourceAsStream(filePath)!!)
        val reader = BufferedReader(streamReader)
        for (i in (1..lineNum)) {
            reader.readLine()
        }
        val line = csv.readAll(reader.readLine()).first()
        streamReader.close()
        return ChessEvaluation(line[0], line[1])
    }

    @GetMapping("/chess-evals/daily")
    fun getDailyChessEvaluations(
        @RequestParam("day") dayInput: Int?,
        @RequestParam("month") monthInput: Int?,
        @RequestParam("difficulty") difficulty: Difficulties? = Difficulties.Medium
    ): ChessEvaluation {
        val today = LocalDate.now()
        val day = dayInput ?: today.dayOfMonth
        val month = monthInput ?: today.monthValue
        val inputDate = LocalDate.of(today.year, month, day)
        val isValid = inputDate.minus(2, ChronoUnit.DAYS).isBefore(today) && inputDate.plus(2, ChronoUnit.DAYS).isAfter(today)
        val date = if (isValid) inputDate else today
        val seed = chessDailySeed?.trim()?.toLong() ?: 0
        val lineNum = Random(date.toEpochDay() + seed).nextInt(numChessEvalLines)
        val filePath = when(difficulty) {
            Difficulties.Easy -> "static/chess/easy-puzzles.csv"
            Difficulties.Medium, null -> "static/chess/medium-puzzles.csv"
            Difficulties.Hard -> "static/chess/hard-puzzles.csv"
        }
        val reader = BufferedReader(InputStreamReader(this.javaClass.classLoader.getResourceAsStream(filePath)!!))
        for (i in (1..lineNum)) {
            reader.readLine()
        }
        val line = csv.readAll(reader.readLine()).first()
        return ChessEvaluation(line[0], line[1])
    }
}