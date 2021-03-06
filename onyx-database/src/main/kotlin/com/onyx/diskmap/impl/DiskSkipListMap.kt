package com.onyx.diskmap.impl

import com.onyx.diskmap.impl.base.skiplist.AbstractIterableSkipList
import com.onyx.diskmap.data.Header
import com.onyx.diskmap.data.SkipNode
import com.onyx.diskmap.store.Store
import com.onyx.exception.AttributeTypeMismatchException
import com.onyx.persistence.query.QueryCriteriaOperator
import com.onyx.extension.common.forceCompare
import com.onyx.extension.common.getAny
import com.onyx.lang.concurrent.ClosureReadWriteLock
import com.onyx.lang.concurrent.impl.DefaultClosureReadWriteLock
import com.onyx.lang.concurrent.impl.EmptyClosureReadWriteLock
import java.lang.reflect.Field
import java.util.*

/**
 * Created by Tim Osborn on 1/7/17.
 *
 *
 * This class was added to enhance the existing index within Onyx Database.  The bitmap was very efficient but, it was a hog
 * as far as how much space it took over.  As far as in-memory data structures, this will be the go-to algorithm.  The
 * data structure is based on a SkipList.
 *
 * @param <K> Key Object Type
 * @param <V> Value Object Type
 * @since 1.2.0
 */
open class DiskSkipListMap<K, V>(fileStore:Store, header: Header, detached: Boolean = false) : AbstractIterableSkipList<K, V>(fileStore, header, detached) {

    override val size: Int
        get() = longSize().toInt()

    private var mapReadWriteLock: ClosureReadWriteLock = if (detached) EmptyClosureReadWriteLock() else DefaultClosureReadWriteLock()

    /**
     * Remove an item within the map
     *
     * @param key Key Identifier
     * @return The value that was removed
     */
    override fun remove(key: K): V? = mapReadWriteLock.writeLock { super.remove(key) }

    /**
     * Put a value into a map based on its key.
     *
     * @param key   Key identifier of the value
     * @param value Underlying value
     * @return The value of the object that was just put into the map
     */
    override fun put(key: K, value: V): V = mapReadWriteLock.writeLock { super.put(key, value) }

    /**
     * Get an item based on its key
     *
     * @param key Identifier
     * @return The corresponding value
     */
    override operator fun get(key: K): V?  = mapReadWriteLock.optimisticReadLock { super.get(key) }

    /**
     * Iterates through the entire skip list to see if it contains the value you are looking for.
     *
     *
     * Note, this is not efficient.  It will basically do a bubble search.
     *
     * @param value Value you are looking for
     * @return Whether the value was found
     */
    override fun containsValue(value: V): Boolean = mapReadWriteLock.readLock{
        values.forEach { next ->
            if (next == value)
                return@readLock true
        }
        return@readLock false
    }

    /**
     * Put all the elements from the map into the skip list map
     *
     * @param from Map to convert from
     */
    override fun putAll(from: Map<out K, V>) = from.forEach { this.put(it.key, it.value) }

    /**
     * Clear all the elements of the array.  If it is not detached we must handle
     * the head of teh data structure
     *
     * @since 1.2.0
     */
    override fun clear() = mapReadWriteLock.writeLock {
        super.clear()

        if (!this.detached) {
            head = SkipNode.create(fileStore)
            this.reference.firstNode = head!!.position
            updateHeaderFirstNode(reference, this.reference.firstNode)
            reference.recordCount.set(0L)
            updateHeaderRecordCount(0L)
        }
    }

    /**
     * Get the record id of a corresponding data.  Note, this points to the SkipListNode position.  Not the actual
     * record position.
     *
     * @param key Identifier
     * @return The position of the record reference if it exists.  Otherwise -1
     * @since 1.2.0
     */
    override fun getRecID(key: K): Long = mapReadWriteLock.optimisticReadLock { find(key)?.position ?: -1 }

    /**
     * Hydrate a record with its record ID.  If the record value exists it will be returned
     *
     * @param recordId Position to find the record reference
     * @return The value within the map
     * @since 1.2.0
     */
    override fun getWithRecID(recordId: Long): V? = mapReadWriteLock.optimisticReadLock {
        if (recordId <= 0)
            return@optimisticReadLock null
        val node:SkipNode = findNodeAtPosition(recordId) ?: return@optimisticReadLock null
        return@optimisticReadLock findValueAtPosition(node.record)
    }

    /**
     * Get Map representation of key object
     *
     * @param recordId Record reference within storage structure
     * @return Map of key values
     * @since 1.2.0
     */
    override fun getMapWithRecID(recordId: Long): Map<String, Any?>? = mapReadWriteLock.optimisticReadLock {
        val node:SkipNode = findNodeAtPosition(recordId) ?: return@optimisticReadLock null
        return@optimisticReadLock getRecordValueAsDictionary(node.record)
    }

    /**
     * Get Map representation of key object.  If it is in the cache, use reflection to get it from the cache.  Otherwise,
     * just hydrate the value within the store
     *
     * @param attribute Attribute name to fetch
     * @param reference  Record reference within storage structure
     * @return Map of key values
     * @since 1.2.0
     * @since 1.3.0 Optimized to require the reflection field so it does not have to re-instantiate one.
     */
    @Throws(AttributeTypeMismatchException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getAttributeWithRecID(attribute: Field, reference: Long): T = mapReadWriteLock.optimisticReadLock {
        val node:SkipNode = findNodeAtPosition(reference) ?: return@optimisticReadLock null as T
        val value = findValueAtPosition(node.record) ?: return@optimisticReadLock null as T
        @Suppress("RemoveExplicitTypeArguments") // This is needed to compile
        return@optimisticReadLock value.getAny<T>(attribute)
    }

    @Throws(AttributeTypeMismatchException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getAttributeWithRecID(field: Field, reference: SkipNode): T = mapReadWriteLock.optimisticReadLock {
        val value = findValueAtPosition(reference.record) ?: return@optimisticReadLock null as T
        @Suppress("RemoveExplicitTypeArguments") // This is needed to compile
        return@optimisticReadLock value.getAny<T>(field)
    }

    /**
     * Find all references above and perhaps equal to the key you are sending in.  The underlying data structure
     * is sorted so this should be very efficient
     *
     * @param index        The index value to compare.  This must be comparable.  It does not work with hash codes.
     * @param includeFirst Whether above and equals to
     * @return A Set of references
     * @since 1.2.0
     */
    override fun above(index: K, includeFirst: Boolean): Set<Long> = mapReadWriteLock.readLock {
        val results = HashSet<Long>()
        var node:SkipNode? = nearest(index)

        if(node != null && !node.isRecord && node.right> 0)
            node = findNodeAtPosition(node.right)

        while(node != null && node.isRecord) {
            val nodeKey:K = node.getKey(fileStore)
            when {
                index.forceCompare(nodeKey) && includeFirst -> results.add(node.position)
                index.forceCompare(nodeKey, QueryCriteriaOperator.GREATER_THAN) -> results.add(node.position)
            }
            node = if (node.right > 0) findNodeAtPosition(node.right) else null
        }

        return@readLock results
    }

    /**
     * Find all references below and perhaps equal to the key you are sending in.  The underlying data structure
     * is sorted so this should be very efficient
     *
     * @param index        The index value to compare.  This must be comparable.  It does not work with hash codes.
     * @param includeFirst Whether above and equals to
     * @return A Set of references
     * @since 1.2.0
     */
    override fun below(index: K, includeFirst: Boolean): Set<Long> = mapReadWriteLock.readLock {
        val results = HashSet<Long>()
        var node:SkipNode? = nearest(index)

        if(node != null && !node.isRecord && node.right> 0)
            node = findNodeAtPosition(node.right)

        while(node != null && node.isRecord) {
            val nodeKey:K = node.getKey(fileStore)

            when {
                index.forceCompare(nodeKey) && includeFirst -> results.add(node.position)
                index.forceCompare(nodeKey, QueryCriteriaOperator.LESS_THAN) -> results.add(node.position)
            }
            node = if (node.left > 0) findNodeAtPosition(node.left) else null
        }

        return@readLock results
    }

}
