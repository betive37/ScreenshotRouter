package com.example.screenshotrouter.core.model

enum class RouteMode {
    Copy,
    Move;

    companion object {
        fun fromName(name: String?): RouteMode = entries.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        } ?: Copy
    }
}
