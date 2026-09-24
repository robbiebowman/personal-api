package com.robbiebowman.personalapi

import com.fasterxml.jackson.core.type.TypeReference
import com.robbiebowman.claude.MessageContent
import com.robbiebowman.personalapi.util.claudeMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClaudeMapperTests {

    @Test
    fun ignoresThinkingBlocksBeforeToolUse() {
        val content = claudeMapper().readValue(
            """[
                {"type":"thinking","thinking":"reasoning","signature":"signature"},
                {"type":"tool_use","id":"tool-1","name":"defineCrosswordClues","input":{"clues":{"clues":[]}}}
            ]""".trimIndent(),
            object : TypeReference<List<MessageContent?>>() {}
        )

        val toolUse = content.filterIsInstance<MessageContent.ToolUse>().single()
        assertEquals("defineCrosswordClues", toolUse.name)
    }
}
