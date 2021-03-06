package com.onyx.interactors.scanner.impl

import com.onyx.descriptor.EntityDescriptor
import com.onyx.diskmap.factory.DiskMapFactory
import com.onyx.diskmap.impl.base.skiplist.AbstractIterableSkipList
import com.onyx.exception.OnyxException
import com.onyx.extension.*
import com.onyx.interactors.record.data.Reference
import com.onyx.interactors.scanner.TableScanner
import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.context.Contexts
import com.onyx.persistence.context.SchemaContext
import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.Query
import com.onyx.persistence.query.QueryCriteria
import kotlin.collections.HashMap

/**
 * Created by timothy.osborn on 1/3/15.
 *
 * It can either scan the entire table or a subset of index values
 */
open class FullTableScanner @Throws(OnyxException::class) constructor(criteria: QueryCriteria, classToScan: Class<*>, descriptor: EntityDescriptor, temporaryDataFile: DiskMapFactory, query: Query, context: SchemaContext, persistenceManager: PersistenceManager) : AbstractTableScanner(criteria, classToScan, descriptor, temporaryDataFile, query, context, persistenceManager), TableScanner {

    /**
     * Full Table Scan
     *
     * @return Map of identifiers.  The key is the partition reference and the value is the reference within file.
     * @throws OnyxException Query exception while trying to scan elements
     * @since 1.3.0 Simplified to check all criteria rather than only a single criteria
     */
    @Throws(OnyxException::class)
    override fun scan(): MutableMap<Reference, Reference> {
        val matching = HashMap<Reference, Reference>()
        val context = Contexts.get(contextId)!!

        @Suppress("UNCHECKED_CAST")
        records.entries.filter {
            val entry = it as AbstractIterableSkipList<Any, IManagedEntity>.SkipListEntry<Any?, IManagedEntity>
            entry.node != null && query.meetsCriteria(entry.value!!, Reference(partitionId, entry.node!!.position), context, descriptor)
        }.forEach {
            val entry = it as AbstractIterableSkipList<Any, IManagedEntity>.SkipListEntry<Any?, IManagedEntity>
            val reference = Reference(partitionId, entry.node!!.position)
            matching.put(reference, reference)
        }

        return matching
    }

    /**
     * Scan records with existing values
     *
     * @param existingValues Existing values to scan from
     * @return Remaining values that meet the criteria
     * @throws OnyxException Exception while scanning entity records
     * @since 1.3.0 Simplified to check all criteria rather than only a single criteria
     */
    @Throws(OnyxException::class)
    override fun scan(existingValues: MutableMap<Reference, Reference>): MutableMap<Reference, Reference> {
        val context = Contexts.get(contextId)!!
        return existingValues.filterTo(HashMap()) {
            val entity = it.value.toManagedEntity(context, descriptor)
            query.meetsCriteria(entity, it.value, context, descriptor)
        }
    }
}