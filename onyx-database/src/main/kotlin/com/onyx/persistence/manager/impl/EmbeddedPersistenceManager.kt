package com.onyx.persistence.manager.impl

import com.onyx.exception.*
import com.onyx.fetch.PartitionQueryController
import com.onyx.fetch.PartitionReference
import com.onyx.helpers.*
import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.collections.LazyQueryCollection
import com.onyx.persistence.context.SchemaContext
import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.Query
import com.onyx.query.CachedResults
import com.onyx.record.AbstractRecordController
import com.onyx.relationship.EntityRelationshipManager
import com.onyx.relationship.RelationshipReference
import com.onyx.stream.QueryMapStream
import com.onyx.stream.QueryStream
import com.onyx.util.ReflectionUtil

import java.util.*

/**
 * Persistence manager supplies a public API for performing database persistence and querying operations.  This specifically is used for an embedded database.
 *
 * @author Tim Osborn
 * @see com.onyx.persistence.manager.PersistenceManager
 *
 * @since 1.0.0
 *
 *
 * PersistenceManagerFactory factory = new EmbeddedPersistenceManagerFactory();
 * factory.setCredentials("username", "password");
 * factory.setLocation("/MyDatabaseLocation")
 * factory.initialize();
 *
 * PersistenceManager manager = factory.getPersistenceManager(); // Get the Persistence manager from the persistence manager factory
 *
 * factory.close(); //Close the in memory database
 *
 */
class EmbeddedPersistenceManager(context: SchemaContext) : PersistenceManager {

    override var context: SchemaContext = context
        set(value) {
            field = value
            value.systemPersistenceManager = this
        }

    var isJournalingEnabled: Boolean = false

    /**
     * Save entity.  Persists a single entity for update or insert.  This method will cascade relationships and persist indexes.
     *
     * @param entity Managed Entity to Save
     * @return Saved Managed Entity
     * @throws OnyxException Exception occurred while persisting an entity
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> saveEntity(entity: IManagedEntity): E {
        context.checkForKillSwitch()

        val descriptor = context.getDescriptorForEntity(entity)
        val recordController = context.getRecordController(descriptor)

        var entityIdentifier: Any? = AbstractRecordController.getIndexValueFromEntity(entity, descriptor.identifier)
        var oldReferenceId = 0L

        if (descriptor.indexes.isNotEmpty()) {
            oldReferenceId = if (entityIdentifier != null) recordController.getReferenceId(entityIdentifier) else 0L
        }

        entityIdentifier = recordController.save(entity)

        journal {
            context.transactionController.writeSave(entity)
        }

        IndexHelper.saveAllIndexesForEntity(context, descriptor, entityIdentifier, oldReferenceId, entity)
        RelationshipHelper.saveAllRelationshipsForEntity(entity, EntityRelationshipManager(), context)

        return entity as E
    }

    /**
     * Batch saves a list of entities.
     *
     *
     * The entities must all be of the same type
     *
     * @param entities List of entities
     * @throws OnyxException Exception occurred while saving an entity within the list.  This will not roll back preceding saves if error occurs.
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun saveEntities(entities: List<IManagedEntity>) {
        context.checkForKillSwitch()

        if (entities.isEmpty())
            return

        val descriptor = context.getDescriptorForEntity(entities[0])
        val recordController = context.getRecordController(descriptor)
        var oldReferenceId = 0L
        var entityIdentifier: Any?

        for (entity in entities) {

            entityIdentifier = AbstractRecordController.getIndexValueFromEntity(entity, descriptor.identifier)
            if (descriptor.indexes.isNotEmpty()) {
                oldReferenceId = if (entityIdentifier != null) recordController.getReferenceId(entityIdentifier) else 0
            }
            entityIdentifier = recordController.save(entity)

            journal {
                context.transactionController.writeSave(entity)
            }

            IndexHelper.saveAllIndexesForEntity(context, descriptor, entityIdentifier, oldReferenceId, entity)
            RelationshipHelper.saveAllRelationshipsForEntity(entity, EntityRelationshipManager(), context)
        }

    }

    /**
     * Deletes a single entity
     *
     *
     * The entity must exist otherwise it will throw an exception.  This will cascade delete relationships and remove index references.
     *
     * @param entity Managed Entity to delete
     * @return Flag indicating it was deleted
     * @throws OnyxException Error occurred while deleting
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun deleteEntity(entity: IManagedEntity): Boolean {
        context.checkForKillSwitch()

        val descriptor = context.getDescriptorForEntity(entity)
        val recordController = context.getRecordController(descriptor)

        // Write Delete transaction to log
        journal {
            context.transactionController.writeDelete(entity)
        }

        val referenceId = recordController.getReferenceId(AbstractRecordController.getIndexValueFromEntity(entity, descriptor.identifier))
        IndexHelper.deleteAllIndexesForEntity(context, descriptor, referenceId)
        RelationshipHelper.deleteAllRelationshipsForEntity(entity, EntityRelationshipManager(), context)

        recordController.delete(entity)

        return true
    }

    /**
     * Execute query and delete entities returned in the results
     *
     * @param query Query used to filter entities with criteria
     * @return Number of entities deleted
     * @throws OnyxException Exception occurred while executing delete query
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun executeDelete(query: Query): Int {
        context.checkForKillSwitch()

        // We want to lock the index controller so that it does not do background indexing
        val descriptor = context.getDescriptorForEntity(query.entityType, query.partition)
        ValidationHelper.validateQuery(descriptor, query, context)
        val queryController = PartitionQueryController(descriptor, this, context)

        try {
            val results = queryController.getReferencesForQuery(query)

            query.resultsCount = results.size

            journal {
                context.transactionController.writeDeleteQuery(query)
            }

            return queryController.deleteRecordsWithReferences(results, query)
        } finally {
            queryController.cleanup()
        }
    }

    /**
     * Updates all rows returned by a given query
     *
     * The query#updates list must not be null or empty
     *
     * @param query Query used to filter entities with criteria
     * @return Number of entities updated
     * @throws OnyxException Exception occurred while executing update query
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun executeUpdate(query: Query): Int {
        context.checkForKillSwitch()

        // This will throw an exception if not valid
        val clazz = query.entityType
        val entity = ReflectionUtil.createNewEntity(clazz)

        // We want to lock the index controller so that it does not do background indexing
        val descriptor = context.getDescriptorForEntity(entity, query.partition)
        ValidationHelper.validateQuery(descriptor, query, context)

        val queryController = PartitionQueryController(descriptor, this, context)

        try {
            val results = queryController.getReferencesForQuery(query)

            query.resultsCount = results.size

            // Write Delete transaction to log
            journal {
                context.transactionController.writeQueryUpdate(query)
            }

            return queryController.performUpdatsForQuery(query, results)
        } finally {
            queryController.cleanup()
        }
    }

    /**
     * Execute query with criteria and optional row limitations
     *
     * @param query Query containing criteria
     * @return Query Results
     * @throws OnyxException Error while executing query
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E> executeQuery(query: Query): List<E> {
        context.checkForKillSwitch()

        val clazz = query.entityType
        val descriptor = context.getDescriptorForEntity(clazz, query.partition)

        ValidationHelper.validateQuery(descriptor, query, context)

        val queryController = PartitionQueryController(descriptor, this, context)
        var cachedResults: CachedResults? = null

        try {
            // Check to see if there are cached query results
            cachedResults = context.queryCacheController.getCachedQueryResults(query)
            var results: Map<*, *>

            // If there are, use the cache rather re-checking criteria
            if (cachedResults != null && cachedResults.references != null) {
                results = cachedResults.references
                query.resultsCount = cachedResults.references.size

                return if (query.selections != null) {
                    ArrayList(queryController.hydrateQuerySelections(query, results).values) as List<E>
                } else {
                    queryController.hydrateResultsWithReferences(query, results) as List<E>
                }
            }
            results = queryController.getReferencesForQuery(query)

            query.resultsCount = results.size

            if (query.shouldSortResults()) {
                results = queryController.sort(query, results)
            }

            // This will go through and get a subset of fields
            if (query.selections != null) {

                // Cache the query results
                cachedResults = context.queryCacheController.setCachedQueryResults(query, results)

                val attributeValues = queryController.hydrateQuerySelections(query, results)
                if (query.isDistinct) {
                    val linkedHashSet = LinkedHashSet(attributeValues.values)
                    return ArrayList(linkedHashSet) as List<E>
                }
                return ArrayList(attributeValues.values) as List<E>
            } else {
                if (cachedResults == null)
                    // Cache the query results
                    cachedResults = context.queryCacheController.setCachedQueryResults(query, results)
                else
                    synchronized(cachedResults) {
                        cachedResults!!.references = results
                    }
                return queryController.hydrateResultsWithReferences(query, results) as List<E>
            }
        } finally {
            if (query.changeListener != null) {
                context.queryCacheController.subscribe(cachedResults, query.changeListener)
            }
            queryController.cleanup()
        }
    }

    /**
     * Execute query with criteria and optional row limitations.  Specify lazy instantiation of query results.
     *
     * @param query Query containing criteria
     * @return LazyQueryCollection lazy loaded results
     * @throws OnyxException Error while executing query
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> executeLazyQuery(query: Query): List<E> {
        context.checkForKillSwitch()

        val clazz = query.entityType
        val entity = ReflectionUtil.createNewEntity(clazz)
        val descriptor = context.getDescriptorForEntity(entity, query.partition)

        ValidationHelper.validateQuery(descriptor, query, context)

        val queryController = PartitionQueryController(descriptor, this, context)
        var cachedResults: CachedResults? = null

        try {
            // Check for cached query results.
            cachedResults = context.queryCacheController.getCachedQueryResults(query)
            var results: Map<*, *>

            // If there are, hydrate the existing rather than looking to the store
            if (cachedResults != null && cachedResults.references != null) {
                results = cachedResults.references
                query.resultsCount = results.size
                return LazyQueryCollection<IManagedEntity>(descriptor, results, context) as List<E>
            }

            // There were no cached results, load them from the store
            results = queryController.getReferencesForQuery(query)

            query.resultsCount = results.size
            if (query.shouldSortResults()) {
                results = queryController.sort(query, results)
            }

            if (cachedResults == null)
                // Cache the query results
                cachedResults = context.queryCacheController.setCachedQueryResults(query, results)
            else
                synchronized(cachedResults) {
                    cachedResults!!.references = results
                }
            return LazyQueryCollection<IManagedEntity>(descriptor, results, context) as List<E>
        } finally {
            if (query.changeListener != null) {
                context.queryCacheController.subscribe(cachedResults, query.changeListener)
            }
            queryController.cleanup()
        }
    }

    /**
     * Hydrates an instantiated entity.  The instantiated entity must have the primary key defined and partition key if the data is partitioned.
     * All relationships are hydrated based on their fetch policy.
     * The entity must also not be null.
     *
     * @param entity Entity to hydrate.
     * @return Managed Entity
     * @throws OnyxException Error when hydrating entity
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> find(entity: IManagedEntity): E {
        context.checkForKillSwitch()

        val descriptor = context.getDescriptorForEntity(entity)
        val recordController = context.getRecordController(descriptor)

        // Find the object
        val results = recordController.get(entity) ?: throw NoResultsException()

        RelationshipHelper.hydrateAllRelationshipsForEntity(results, EntityRelationshipManager(), context)

        ReflectionUtil.copy(results, entity, descriptor)
        return entity as E
    }

    /**
     * Find Entity By Class and ID.
     *
     *
     * All relationships are hydrated based on their fetch policy.  This does not take into account the partition.
     *
     * @param clazz Managed Entity Type.  This must be a cast of IManagedEntity
     * @param id    Primary Key of entity
     * @return Managed Entity
     * @throws OnyxException Error when finding entity
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> findById(clazz: Class<*>, id: Any): E? {
        context.checkForKillSwitch()

        var entity: IManagedEntity? = ReflectionUtil.createNewEntity(clazz)
        val descriptor = context.getDescriptorForEntity(entity!!, "")
        val recordController = context.getRecordController(descriptor)

        // Find the object
        entity = recordController.getWithId(id)

        if (entity == null) {
            return null
        }

        RelationshipHelper.hydrateAllRelationshipsForEntity(entity, EntityRelationshipManager(), context)

        return entity as E?
    }

    /**
     * Find Entity By Class and ID.
     *
     *
     * All relationships are hydrated based on their fetch policy.  This does not take into account the partition.
     *
     * @param clazz       Managed Entity Type.  This must be a cast of IManagedEntity
     * @param id          Primary Key of entity
     * @param partitionId Partition key for entity
     * @return Managed Entity
     * @throws OnyxException Error when finding entity within partition specified
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> findByIdInPartition(clazz: Class<*>, id: Any, partitionId: Any): E? {

        context.checkForKillSwitch()

        var entity: IManagedEntity? = ReflectionUtil.createNewEntity(clazz)
        val descriptor = context.getDescriptorForEntity(entity!!, partitionId)
        val recordController = context.getRecordController(descriptor)

        // Find the object
        entity = recordController.getWithId(id)

        if (entity == null) {
            return null
        }

        RelationshipHelper.hydrateAllRelationshipsForEntity(entity, EntityRelationshipManager(), context)

        return entity as E?
    }

    /**
     * Determines if the entity exists within the database.
     *
     *
     * It is determined by the primary id and partition key
     *
     * @param entity Managed Entity to check
     * @return Returns true if the entity primary key exists. Otherwise it returns false
     * @throws OnyxException Error when finding entity within partition specified
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun exists(entity: IManagedEntity): Boolean {
        context.checkForKillSwitch()
        val descriptor = context.getDescriptorForEntity(entity)
        val recordController = context.getRecordController(descriptor)

        // Find the object
        return recordController.exists(entity)
    }

    /**
     * Determines if the entity exists within the database.
     *
     *
     * It is determined by the primary id and partition key
     *
     * @param entity      Managed Entity to check
     * @param partitionId Partition Value for entity
     * @return Returns true if the entity primary key exists. Otherwise it returns false
     * @throws OnyxException Error when finding entity within partition specified
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun exists(entity: IManagedEntity, partitionId: Any): Boolean {
        context.checkForKillSwitch()

        val descriptor = context.getDescriptorForEntity(entity, partitionId)
        val recordController = context.getRecordController(descriptor)

        return recordController.exists(entity)
    }

    /**
     * Force Hydrate relationship based on attribute name
     *
     * @param entity    Managed Entity to attach relationship values
     * @param attribute String representation of relationship attribute
     * @throws OnyxException Error when hydrating relationship.  The attribute must exist and be a relationship.
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun initialize(entity: IManagedEntity, attribute: String) {
        context.checkForKillSwitch()

        val descriptor = context.getDescriptorForEntity(entity)
        val identifier = AbstractRecordController.getIndexValueFromEntity(entity, descriptor.identifier)
        val partitionValue = PartitionHelper.getPartitionFieldValue(entity, context)

        val entityRelationshipReference = if (partitionValue !== PartitionHelper.NULL_PARTITION && partitionValue != null) {
            val partitionEntry = context.getPartitionWithValue(descriptor.entityClass, PartitionHelper.getPartitionFieldValue(entity, context))
            RelationshipReference(identifier, partitionEntry!!.index)
        } else {
            RelationshipReference(identifier, 0)
        }

        val relationshipDescriptor = descriptor.relationships[attribute] ?: throw RelationshipNotFoundException(RelationshipNotFoundException.RELATIONSHIP_NOT_FOUND, attribute, entity.javaClass.name)
        val relationshipController = context.getRelationshipController(relationshipDescriptor)
        relationshipController.hydrateRelationshipForEntity(entityRelationshipReference, entity, EntityRelationshipManager(), true)
    }

    /**
     * This is a way to batch save all relationships for an entity.  This does not retain any existing relationships and will
     * overwrite all existing with the set you are sending in.  This is useful to optimize batch saving entities with relationships.
     *
     * @param entity                  Parent Managed Entity
     * @param relationship            Relationship attribute
     * @param relationshipIdentifiers Existing relationship identifiers
     * @throws OnyxException Error occurred while saving relationship.
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun saveRelationshipsForEntity(entity: IManagedEntity, relationship: String, relationshipIdentifiers: Set<Any>) {
        val relationships = context.getDescriptorForEntity(entity).relationships
        val relationshipDescriptor = relationships[relationship] ?: throw RelationshipNotFoundException(RelationshipNotFoundException.RELATIONSHIP_NOT_FOUND, relationship, entity.javaClass.name)
        val references = HashSet<RelationshipReference>()

        relationshipIdentifiers.forEach {
            if (it is RelationshipReference) {
                references.add(it)
            } else {
                references.add(RelationshipReference(it, 0))
            }
        }

        val relationshipController = context.getRelationshipController(relationshipDescriptor)
        relationshipController.updateAll(entity, references)
    }

    /**
     * Get entity with Reference Id.  This is used within the LazyResultsCollection and LazyQueryResults to fetch entities with file record ids.
     *
     * @param referenceId Reference location within database
     * @return Managed Entity
     * @throws OnyxException The reference does not exist for that type
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> getWithReferenceId(entityType: Class<*>, referenceId: Long): E {
        context.checkForKillSwitch()

        var entity = ReflectionUtil.createNewEntity(entityType)
        val descriptor = context.getDescriptorForEntity(entity, "")
        val recordController = context.getRecordController(descriptor)

        // Find the object
        entity = recordController.getWithReferenceId(referenceId)

        RelationshipHelper.hydrateAllRelationshipsForEntity(entity, EntityRelationshipManager(), context)
        return entity as E
    }

    /**
     * Get an entity by its partition reference.  This is the same as the method above but for objects that have
     * a reference as part of a partition.  An example usage would be in LazyQueryCollection so that it may
     * hydrate objects in random partitions.
     *
     * @param entityType         Type of managed entity
     * @param partitionReference Partition reference holding both the partition id and reference id
     * @param <E>                The managed entity implementation class
     * @return Managed Entity
     * @throws OnyxException The reference does not exist for that type
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> getWithPartitionReference(entityType: Class<*>, partitionReference: PartitionReference): E? {
        val (_, _, partitionValue) = context.getPartitionWithId(partitionReference.partition) ?: return null
        val descriptor = context.getDescriptorForEntity(entityType, partitionValue)
        val recordController = context.getRecordController(descriptor)
        return recordController.getWithReferenceId(partitionReference.reference) as E
    }

    /**
     * Retrieves an entity using the primaryKey and partition
     *
     * @param clazz       Entity Type
     * @param id          Entity Primary Key
     * @param partitionId - Partition Identifier.  Not to be confused with partition key.  This is a unique id within the partition System table
     * @return Managed Entity
     * @throws OnyxException error occurred while attempting to retrieve entity.
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <E : IManagedEntity> findByIdWithPartitionId(clazz: Class<*>, id: Any, partitionId: Long): E {
        context.checkForKillSwitch()

        var entity = ReflectionUtil.createNewEntity(clazz)
        val descriptor = context.getDescriptorForEntity(entity, "")
        val partitionContext = PartitionContext(context, descriptor)
        val recordController = partitionContext.getRecordControllerForPartition(partitionId)

        // Find the object
        entity = recordController.getWithId(id)

        RelationshipHelper.hydrateAllRelationshipsForEntity(entity, EntityRelationshipManager(), context)

        return entity as E
    }

    /**
     * Get Map representation of an entity with reference id
     *
     * @param entityType Original type of entity
     * @param reference  Reference location within a data structure
     * @return Map of key key pair of the entity.  Key being the attribute name.
     */
    @Throws(OnyxException::class)
    override fun getMapWithReferenceId(entityType: Class<*>, reference: Long): Map<*, *> {
        context.checkForKillSwitch()

        val entity = ReflectionUtil.createNewEntity(entityType)
        val descriptor = context.getDescriptorForEntity(entity, "")
        val recordController = context.getRecordController(descriptor)

        // Find the object
        return recordController.getMapWithReferenceId(reference)
    }

    /**
     * Retrieve the quantity of entities that match the query criterium.
     *
     *
     * usage:
     *
     *
     * Query myQuery = new Query();
     * myQuery.setClass(SystemEntity.class);
     * long numberOfSystemEntities = persistenceManager.countForQuery(myQuery);
     *
     *
     * or:
     *
     *
     * Query myQuery = new Query(SystemEntity.class, new QueryCriteria("primaryKey", QueryCriteriaOperator.GREATER_THAN, 3));
     * long numberOfSystemEntitiesWithIdGt3 = persistenceManager.countForQuery(myQuery);
     *
     * @param query The query to apply to the count operation
     * @return The number of entities that meet the query criterium
     * @throws OnyxException Error during query.
     * @since 1.3.0 Implemented with feature request #71
     */
    @Throws(OnyxException::class)
    override fun countForQuery(query: Query): Long {

        val clazz = query.entityType

        // We want to lock the index controller so that it does not do background indexing
        val descriptor = context.getDescriptorForEntity(clazz, query.partition)
        ValidationHelper.validateQuery(descriptor, query, context)

        val cachedResults = context.queryCacheController.getCachedQueryResults(query)
        if (cachedResults != null && cachedResults.references != null)
            return cachedResults.references.size.toLong()

        val queryController = PartitionQueryController(descriptor, this, context)

        try {
            return queryController.getCountForQuery(query)
        } finally {
            queryController.cleanup()
        }
    }

    /**
     * This method is used for bulk streaming data entities.  An example of bulk streaming is for analytics or bulk updates included but not limited to model changes.
     *
     * @param query    Query to execute and stream
     * @param streamer Instance of the streamer to use to stream the data
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> stream(query: Query, streamer: QueryStream<T>) {
        val entityList = this.executeLazyQuery<IManagedEntity>(query) as LazyQueryCollection<IManagedEntity>

        entityList.forEachIndexed { index, iManagedEntity ->
            if (streamer is QueryMapStream) {
                streamer.accept(entityList.getDict(index), this)
            } else {
                streamer.accept(iManagedEntity as T, this)
            }
        }
    }

    /**
     * This method is used for bulk streaming.  An example of bulk streaming is for analytics or bulk updates included but not limited to model changes.
     *
     * @param query            Query to execute and stream
     * @param queryStreamClass Class instance of the database stream
     * @since 1.0.0
     */
    @Throws(OnyxException::class)
    override fun stream(query: Query, queryStreamClass: Class<*>) {
        val streamer: QueryStream<*>
        try {
            streamer = queryStreamClass.newInstance() as QueryStream<*>
        } catch (e: InstantiationException) {
            throw StreamException(StreamException.CANNOT_INSTANTIATE_STREAM)
        } catch (e: IllegalAccessException) {
            throw StreamException(StreamException.CANNOT_INSTANTIATE_STREAM)
        }

        this.stream(query, streamer)
    }

    /**
     * Un-register a query listener.  This will remove the listener from observing changes for that query.
     * If you do not un-register queries, they will not expire nor will they be de-registered autmatically.
     * This could cause performance degredation if removing the registration is neglected.
     *
     * @param query Query with a listener attached
     *
     * @throws OnyxException Un expected error when attempting to unregister listener
     *
     * @since 1.3.0 Added query subscribers as an enhancement.
     */
    @Throws(OnyxException::class)
    override fun removeChangeListener(query: Query): Boolean {
        val clazz = query.entityType

        // We want to lock the index controller so that it does not do background indexing
        val descriptor = context.getDescriptorForEntity(clazz, query.partition)
        ValidationHelper.validateQuery(descriptor, query, context)

        return context.queryCacheController.unsubscribe(query)
    }

    /**
     * Listen to a query and register its subscriber
     *
     * @param query Query with query listener
     * @since 1.3.1
     */
    @Throws(OnyxException::class)
    override fun listen(query: Query) {
        val clazz = query.entityType

        // We want to lock the index controller so that it does not do background indexing
        val descriptor = context.getDescriptorForEntity(clazz, query.partition)
        ValidationHelper.validateQuery(descriptor, query, context)

        context.queryCacheController.subscribe(query)
    }

    /**
     * Run Journaling code if it is enabled
     *
     * @since 2.0.0 Added as a fancy unit
     */
    private fun journal(body:() -> Unit) {
        if(isJournalingEnabled)
            body.invoke()
    }
}