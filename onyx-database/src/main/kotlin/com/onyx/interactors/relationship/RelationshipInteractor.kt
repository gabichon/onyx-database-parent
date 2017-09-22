package com.onyx.interactors.relationship

import com.onyx.exception.OnyxException
import com.onyx.scan.PartitionReference
import com.onyx.persistence.IManagedEntity
import com.onyx.interactors.relationship.data.RelationshipReference
import com.onyx.interactors.relationship.data.RelationshipTransaction

/**
 * Created by timothy.osborn on 2/5/15.
 *
 * Contract on how to manipulate relationships
 */
interface RelationshipInteractor {

    /**
     * Saves a relationship for an entity
     *
     * @param entity Entity being saved
     * @param transaction Prevents recursion
     * @throws OnyxException Error saving realationship object
     */
    @Throws(OnyxException::class)
    fun saveRelationshipForEntity(entity: IManagedEntity, transaction: RelationshipTransaction)

    /**
     * Delete Relationship entity
     *
     * @param entity Entity to delete relationship for
     * @param transaction prevents recursion
     * @throws OnyxException Error deleting relationship
     */
    @Throws(OnyxException::class)
    fun deleteRelationshipForEntity(entity:IManagedEntity, transaction: RelationshipTransaction)

    /**
     * Hydrate a relationship values.  Force the hydration if specified
     *
     * @param entity Entity to hydrate
     * @param transaction keeps track of cyclical references and prevents infinite loop
     * @param force force hydrate
     * @throws OnyxException error hydrating relationship
     */
    @Throws(OnyxException::class)
    fun hydrateRelationshipForEntity(entity: IManagedEntity, transaction: RelationshipTransaction, force: Boolean)

    /**
     * Retrieves the identifiers for a given entity
     *
     * @param referenceId reference of the entity with relationships
     * @return List of relationship references
     */
    @Throws(OnyxException::class)
    fun getRelationshipIdentifiersWithReferenceId(referenceId: Long?): List<RelationshipReference>

    /**
     * Retrieves the identifiers for a given entity
     *
     * @param referenceId reference of the entity within partition with relationships
     * @return List of relationship references
     */
    @Throws(OnyxException::class)
    fun getRelationshipIdentifiersWithReferenceId(referenceId: PartitionReference): List<RelationshipReference>

    /**
     * Batch Save all relationship ids
     *
     * @param entity Entity to update
     * @param relationshipIdentifiers list of entity references
     */
    @Throws(OnyxException::class)
    fun updateAll(entity: IManagedEntity, relationshipIdentifiers: MutableSet<RelationshipReference>)
}
