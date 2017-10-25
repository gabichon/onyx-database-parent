package com.onyx.persistence.query.impl

import com.onyx.buffer.BufferStream
import com.onyx.buffer.BufferStreamable
import com.onyx.exception.BufferingException
import com.onyx.persistence.query.QueryListenerEvent

/**
 * Created by tosborn1 on 3/27/17.
 *
 * Pojo for a query event push response
 */
class QueryEvent<T>(var type: QueryListenerEvent, var entity: T?) : BufferStreamable {


    /**
     * Read value from buffer
     * @param buffer Buffer Stream to read from
     * @throws BufferingException Hmmm, you messed up
     */
    @Throws(BufferingException::class)
    override fun read(buffer: BufferStream) {
        this.type = QueryListenerEvent.values()[buffer.byte.toInt()]
        @Suppress("UNCHECKED_CAST")
        this.entity = buffer.value as T?
    }

    /**
     * Write to buffer stream
     * @param buffer Buffer IO Stream to write to
     */
    @Throws(BufferingException::class)
    override fun write(buffer: BufferStream) {
        buffer.putByte(this.type.ordinal.toByte())
        buffer.putObject(this.entity)
    }

}