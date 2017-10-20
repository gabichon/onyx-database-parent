package com.onyx.client.connection

import com.onyx.extension.common.catchAll
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking

class UnifiedMessageChannel {
    val readChannel: Channel<() -> Unit> = Channel()
    val writeChannel: Channel<() -> Unit> = Channel()

    fun close() {
        runBlocking {
            catchAll { writeChannel.close() }
            catchAll { readChannel.close() }
        }
    }
}
