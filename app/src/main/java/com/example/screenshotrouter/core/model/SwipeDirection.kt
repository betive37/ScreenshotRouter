package com.example.screenshotrouter.core.model

enum class SwipeDirection {
    Left,
    Right,
    Up,
    Down;

    companion object {
        fun fromName(name: String?): SwipeDirection? = entries.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
    }
}
