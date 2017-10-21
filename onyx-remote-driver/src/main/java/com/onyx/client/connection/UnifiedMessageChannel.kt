package com.onyx.client.connection

import com.onyx.extension.common.catchAll
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import java.nio.ByteBuffer

class UnifiedMessageChannel {
    val readChannel: Channel<() -> Unit> = Channel()
    val writeChannel: Channel<ByteBuffer> = Channel()

    fun close() {
        runBlocking {
            catchAll { writeChannel.close() }
            catchAll { readChannel.close() }
        }
    }
}
