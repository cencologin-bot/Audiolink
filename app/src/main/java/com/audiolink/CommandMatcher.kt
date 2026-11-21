package com.audiolink

/**
 * Regla exacta del comando: el campo debe contener solo "/audio",
 * con como máximo un espacio (antes o después, no ambos).
 * Distingue mayúsculas: "/Audio" no activa.
 */
object CommandMatcher {
    private val regex = Regex("^(/audio ?| /audio)$")

    fun isCommand(text: String): Boolean = regex.matches(text)
}
