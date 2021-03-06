package com.onyx.interactors.scanner.impl

import com.onyx.descriptor.EntityDescriptor
import com.onyx.diskmap.DiskMap
import com.onyx.diskmap.factory.DiskMapFactory
import com.onyx.entity.SystemEntity
import com.onyx.exception.OnyxException
import com.onyx.extension.common.async
import com.onyx.interactors.record.data.Reference
import com.onyx.interactors.scanner.TableScanner
import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.context.Contexts
import com.onyx.persistence.context.SchemaContext
import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.Query
import com.onyx.persistence.query.QueryCriteria
import com.onyx.persistence.query.QueryPartitionMode
import java.util.concurrent.Future

/**
 * Created by timothy.osborn on 1/3/15.
 *
 * It can either scan the entire table or a subset of index values
 */
open class PartitionReferenceScanner @Throws(OnyxException::class) constructor(criteria: QueryCriteria, classToScan: Class<*>, descriptor: EntityDescriptor, temporaryDataFile: DiskMapFactory, query: Query, context: SchemaContext, persistenceManager: PersistenceManager) : ReferenceScanner(criteria, classToScan, descriptor, temporaryDataFile, query, context, persistenceManager), TableScanner {

    private var systemEntity: SystemEntity = context.getSystemEntityByName(query.entityType!!.name)!!

    /**
     * Not supported for all references
     *
     * @return Map of identifiers.  The key is the partition reference and the value is the reference within file.
     * @throws OnyxException Query exception while trying to scan elements
     * @since 1.3.0 Simplified to check all criteria rather than only a single criteria
     */
    @Throws(OnyxException::class)
    override fun scan(): MutableMap<Reference, Reference> = HashMap()

    /**
     * Retrieve all references except those that are passed in
     *
     * @param existingValues Existing values to scan from
     * @return Remaining values that meet the criteria
     * @throws OnyxException Exception while scanning entity records
     * @since 2.0.0 To support the .not() method
     */
    @Throws(OnyxException::class)
    override fun scan(existingValues: MutableMap<Reference, Reference>): MutableMap<Reference, Reference> {
        val context = Contexts.get(contextId)!!

        if (query.partition === QueryPartitionMode.ALL) {
            val allMatching = HashMap<Reference, Reference>()
            val units = ArrayList<Future<Map<Reference, Reference>>>()

            systemEntity.partition!!.entries.forEach {
                units.add(
                        async {
                            val matching = HashMap<Reference, Reference>()
                            val partitionDescriptor = context.getDescriptorForEntity(query.entityType, it.value)
                            val dataFile = context.getDataFile(partitionDescriptor)
                            val records = dataFile.getHashMap<DiskMap<Any, IManagedEntity>>(partitionDescriptor.entityClass.name, partitionDescriptor.identifier!!.loadFactor.toInt())
                            (records.references.map { Reference(partitionId, it.position) } - existingValues.keys).forEach { matching.put(it, it) }
                            return@async matching
                        }
                )
            }

            units.forEach { allMatching += it.get() }
            return allMatching
        } else {
            val partitionId = context.getPartitionWithValue(query.entityType!!, query.partition)?.index ?: 0L
            if(partitionId == 0L) // Partition does not exist, lets do a full scan of default partition
                return super.scan()

            val partitionDescriptor = context.getDescriptorForEntity(query.entityType, query.partition)
            val dataFile = context.getDataFile(partitionDescriptor)
            records = dataFile.getHashMap(partitionDescriptor.entityClass.name, partitionDescriptor.identifier!!.loadFactor.toInt())
            return super.scan(existingValues)
        }
    }
}