package com.fairair.app.ui.chat

import kotlinx.datetime.Clock

internal actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
