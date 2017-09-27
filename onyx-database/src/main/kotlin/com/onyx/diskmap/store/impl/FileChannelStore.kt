package com.onyx.diskmap.store.impl

import com.onyx.buffer.BufferStream
import com.onyx.buffer.BufferStreamable
import com.onyx.diskmap.base.concurrent.AtomicCounter
import com.onyx.diskmap.base.concurrent.DefaultAtomicCounter
import com.onyx.diskmap.store.Store
import com.onyx.extension.common.async
import com.onyx.extension.perform
import com.onyx.extension.withBuffer
import com.onyx.persistence.context.Contexts
import com.onyx.persistence.context.SchemaContext
import com.onyx.util.ReflectionUtil

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Created by timothy.osborn on 3/25/15.
 *
 * The default implementation of a store that includes the i/o of writing to a basic file channel.
 * This is recommended for larger data sets.
 *
 * This class also encapsulates the serialization of objects that are read and written to the store.
 *
 * @since 1.0.0
 */
open class FileChannelStore() : Store {

    override val context by lazy { if(contextId != null) Contexts.get(contextId!!) else null }

    final override var filePath: String = ""
    var deleteOnClose: Boolean = false // Whether to delete this file upon closing database or JVM
    var bufferSliceSize = LARGE_FILE_SLICE_SIZE // Size of each slice

    protected var channel: FileChannel? = null
    protected var contextId:String? = null
    private var fileSizeCounter: AtomicCounter = DefaultAtomicCounter(0)

    constructor(filePath: String = "", context: SchemaContext? = null, deleteOnClose: Boolean = false) : this() {
        this.bufferSliceSize = if(deleteOnClose) SMALL_FILE_SLICE_SIZE else LARGE_FILE_SLICE_SIZE
        this.deleteOnClose = deleteOnClose
        this.filePath = filePath
        this.contextId = context?.contextId

        this.open(filePath = filePath)
        this.determineSize()
    }

    /**
     * Get the size of the file
     */
    override fun getFileSize():Long = fileSizeCounter.get()

    /**
     * Open the data file
     *
     * @param filePath Path of the file to open
     * @return Whether the file was opened or not
     */
    open fun open(filePath: String): Boolean {
        val file = File(filePath)
        try {
            // Create the data file if it does not exist
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }

            // Open the random access file
            val randomAccessFile = RandomAccessFile(filePath, "rw")
            this.channel = randomAccessFile.channel
            this.fileSizeCounter.set(this.channel!!.size())

        } catch (e: FileNotFoundException) {
            return false
        } catch (e: IOException) {
            return false
        }

        return channel!!.isOpen
    }

    /**
     * Set the size after opening a file.  The first 8 bytes are reserved for the size.  The reason why we maintain the size
     * outside of relying of the fileChannel is because it may not be accurate.  In order to force it's accuracy
     * we have to configure the file channel to do so.  That causes the store to be severely slowed down.
     */
    protected fun determineSize() {
        val buffer = this.read(0, 8)
        buffer?.byteBuffer?.rewind()
        buffer.perform {
            if (buffer == null || channel?.size() == 0L) {
                this.allocate(8)
            } else {
                val fSize = buffer.long
                this.fileSizeCounter.set(fSize)
            }
        }
    }

    /**
     * Close the data file
     *
     * @return Whether the file was closed successfully.
     */
    override fun close(): Boolean = try {
        if (!deleteOnClose) {
            this.channel!!.force(true)
        }
        this.channel!!.close()
        async {
            if (deleteOnClose) {
                delete()
            }
        }
        !this.channel!!.isOpen
    } catch (e: IOException) {
        false
    }

    /**
     * Commit all file writes
     */
    override fun commit() { this.channel?.force(true) }

    /**
     * Write an Object Buffer
     *
     * @param buffer Byte buffer to write
     * @param position Position within the volume to write to.
     * @return How many bytes were written
     */
    override fun write(buffer: ByteBuffer, position: Long): Int = channel!!.write(buffer)

    /**
     * Write a serializable object to a volume.  This uses the BufferStream for serialization
     *
     * @param serializable Object
     * @param position Position to write to
     */
    override fun write(serializable: BufferStreamable, position: Long): Int {
        val stream = BufferStream()
        try {
            serializable.write(stream)
            stream.byteBuffer.flip()
            return channel!!.write(stream.byteBuffer, position)
        } finally {
            stream.recycle()
        }
    }

    /**
     * Read a serializable object from the store
     *
     * @param position Position to read from
     * @param size Amount of bytes to read.
     * @param type class type
     * @return The object that was read from the store
     */
    override fun read(position: Long, size: Int, type: Class<*>): Any? {
        if (!validateFileSize(position)) return null

        val buffer = BufferStream(size)
        return buffer.perform {
            channel!!.read(buffer.byteBuffer, position)
            buffer.byteBuffer.rewind()

            return@perform if (BufferStreamable::class.java.isAssignableFrom(type)) {
                val serializable:BufferStreamable = ReflectionUtil.instantiate(type) as BufferStreamable
                serializable.read(buffer)
                serializable
            } else {
                buffer.`object`
            }
        }

    }

    /**
     * Read a serializable object
     *
     * @param position Position to read from
     * @param size Amount of bytes to read.
     * @param serializable object to read into
     * @return same object instance that was sent in.
     */
    override fun read(position: Long, size: Int, serializable: BufferStreamable): Any? {
        if (!validateFileSize(position))
            return null

        val buffer = BufferStream.allocateAndLimit(size)
        return withBuffer(buffer) {
            channel!!.read(buffer, position)
            buffer.flip()
            serializable.read(BufferStream(buffer))
            return@withBuffer serializable
        }
    }

    /**
     * Write a serializable object
     *
     * @param position Position to read from
     * @param size Amount of bytes to read.
     * @return Object Buffer contains bytes read
     */
    override fun read(position: Long, size: Int): BufferStream? {
        if (!validateFileSize(position))
            return null

        val buffer = BufferStream.allocateAndLimit(size)

        channel!!.read(buffer, position)
        buffer.rewind()

        return BufferStream(buffer)

    }

    /**
     * Read the file channel and put it into a buffer at a position
     *
     * @param buffer   Buffer to put into
     * @param position position in store to read
     */
    override fun read(buffer: ByteBuffer, position: Long) {
        channel!!.read(buffer, position)
        buffer.flip()
    }

    /**
     * Validate we are not going to read beyond the allocated file storage.  This would be bad
     * @param position Position to validate
     * @return whether the object you seek is in a valid position
     */
    protected fun validateFileSize(position: Long): Boolean = position < fileSizeCounter.get()

    /**
     * Allocates a spot in the file
     *
     * @param size Allocate space within the store.
     * @return position of started allocated bytes
     */
    override fun allocate(size: Int): Long {
        val buffer = BufferStream.allocateAndLimit(8)
        return withBuffer(buffer) {
            val newFileSize = fileSizeCounter.getAndAdd(size)
            it.putLong(newFileSize + size)
            it.flip()
            this.write(it, 0)
            return@withBuffer newFileSize
        }
    }


    /**
     * Delete File
     */
    override fun delete() {
        val dataFile = File(filePath)
        dataFile.delete()
    }

    /**
     * Reset the storage so that it has a clean slate
     * and truncates all relative data.
     *
     * @since 1.3.0
     */
    override fun reset() {
        fileSizeCounter.set(0)
        this.allocate(8)
    }

    companion object {
        val SMALL_FILE_SLICE_SIZE = 1024 * 256 // 256K
        val LARGE_FILE_SLICE_SIZE = 1024 * 1024 * 4 // 4MB
    }

}
