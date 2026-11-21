package com.audiolink

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandMatcherTest {
    @Test fun acceptsExactCommand() = assertTrue(CommandMatcher.isCommand("/audio"))
    @Test fun acceptsOneTrailingSpace() = assertTrue(CommandMatcher.isCommand("/audio "))
    @Test fun acceptsOneLeadingSpace() = assertTrue(CommandMatcher.isCommand(" /audio"))

    @Test fun rejectsWithoutSlash() = assertFalse(CommandMatcher.isCommand("audio"))
    @Test fun rejectsPlural() = assertFalse(CommandMatcher.isCommand("/audios"))
    @Test fun rejectsDigits() = assertFalse(CommandMatcher.isCommand("/audio123"))
    @Test fun rejectsPrefixWord() = assertFalse(CommandMatcher.isCommand("mi/audio"))
    @Test fun rejectsPrefixText() = assertFalse(CommandMatcher.isCommand("texto/audio"))
    @Test fun rejectsOtherTextBefore() = assertFalse(CommandMatcher.isCommand("hola /audio"))
    @Test fun rejectsTwoSpaces() = assertFalse(CommandMatcher.isCommand(" /audio "))
    @Test fun rejectsUppercase() = assertFalse(CommandMatcher.isCommand("/Audio"))
    @Test fun rejectsEmpty() = assertFalse(CommandMatcher.isCommand(""))
}
