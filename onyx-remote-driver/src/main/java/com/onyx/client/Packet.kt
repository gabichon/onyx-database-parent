package com.onyx.client

import com.onyx.buffer.BufferStreamable
import java.nio.ByteBuffer

/**
 * Indicates a part of a network message.
 *
 * @since 2.0.0 Effort to cleanup the logic on the Network peers and make the parsing of packets simpler
 */
class Packet (private var packetSize:Short, var messageId:Short, var packetBuffer: ByteBuffer):BufferStreamable {

    /**
     * Constructor including the buffer.  This will automatically read it from the buffer
     */
    constructor(buffer: ByteBuffer) :this(buffer.short, buffer.short, buffer)

    /**
     * Write a packet to a buffer.  The packet metadata is always located at the start of the buffer.
     *
     * @since 2.0.0
     */
    fun write(buffer: ByteBuffer) {
        buffer.position(0)
        buffer.putShort(packetSize)
        buffer.putShort(messageId)
    }

    companion object {
        val PACKET_METADATA_SIZE = java.lang.Short.BYTES * 2
    }
}
