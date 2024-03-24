package io.github.dca

inline fun <T, R> T.andThen(block: (T) -> R): R = block(this)
