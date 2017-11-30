package com.onyx.interactors.relationship.impl

import com.onyx.descriptor.EntityDescriptor
import com.onyx.descriptor.RelationshipDescriptor
import com.onyx.exception.OnyxException
import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.context.SchemaContext
import com.onyx.interactors.relationship.data.RelationshipTransaction
import com.onyx.interactors.relationship.RelationshipInteractor
import com.onyx.interactors.relationship.data.RelationshipReference
import com.onyx.extension.*

/**
 * Created by timothy.osborn on 2/5/15.
 *
 * Handles actions on a to one relationship
 */
class ToOneRelationshipInteractor @Throws(OnyxException::class) constructor(entityDescriptor: EntityDescriptor, relationshipDescriptor: RelationshipDescriptor, context: SchemaContext) : AbstractRelationshipInteractor(entityDescriptor, relationshipDescriptor, context), RelationshipInteractor {

    /**
     * Save Relationship for entity
     *
     * @param entity Entity to save relationships for
     * @param transaction Prevents recursion
     */
    @Throws(OnyxException::class)
    override fun saveRelationshipForEntity(entity: IManagedEntity, transaction: RelationshipTransaction) {
        val relationshipObject:IManagedEntity? = entity[context, entityDescriptor, relationshipDescriptor.name]
        val relationshipReferenceMap:MutableMap<RelationshipReference, MutableSet<RelationshipReference>> = entity.relationshipReferenceMap(context, relationshipDescriptor.name)!!
        val currentRelationshipReference: RelationshipReference? = relationshipObject?.toRelationshipReference(context)
        val parentRelationshipReference = entity.toRelationshipReference(context)

        // Cascade Save. Make sure it is either ALL, or SAVE.  Also ensure that we haven't already saved it before
        if (relationshipDescriptor.shouldSaveEntity
                && relationshipObject != null
                && !transaction.contains(relationshipObject, context)) {
            currentRelationshipReference!!.identifier = relationshipObject.save(context).second
            relationshipObject.saveIndexes(context, currentRelationshipReference.referenceId)
            relationshipObject.saveRelationships(context, RelationshipTransaction(entity, context))
        }

        val existingReference = relationshipReferenceMap[parentRelationshipReference]?.firstOrNull()

        // Cascade Delete. Make sure it is either ALL, or DELETE.
        if (relationshipDescriptor.shouldDeleteEntity) {
            if (currentRelationshipReference == null && existingReference != null) {
                val existingRefManagedObject = existingReference.toManagedEntity(context, relationshipDescriptor.inverseClass)

                if (existingRefManagedObject != null && !transaction.contains(existingRefManagedObject, context)) {
                    existingRefManagedObject.deleteAllIndexes(context, existingRefManagedObject.referenceId(context))
                    existingRefManagedObject.deleteRelationships(context, transaction)
                    existingRefManagedObject.recordInteractor(context).deleteWithId(existingRefManagedObject.identifier(context)!!)
                }
            }
        } else if(relationshipDescriptor.shouldDeleteEntityReference && existingReference != null && existingReference != currentRelationshipReference) {
            deleteInverseRelationshipReference(entity, parentRelationshipReference, existingReference)
        }

        if (relationshipObject != null) {
            saveInverseRelationship(entity, relationshipObject, parentRelationshipReference, currentRelationshipReference!!)
            relationshipReferenceMap.put(parentRelationshipReference, hashSetOf(currentRelationshipReference))
        } else {
            relationshipReferenceMap.put(parentRelationshipReference, hashSetOf())
        }
    }

    /**
     * Hydrate relationship for entity
     *
     * @param entity           Entity to hydrate
     * @param transaction      Relationship transaction prevents recursion
     * @param force            Force hydrate
     */
    @Throws(OnyxException::class)
    override fun hydrateRelationshipForEntity(entity: IManagedEntity, transaction: RelationshipTransaction, force: Boolean) {
        transaction.add(entity, context)

        val existingRelationshipReferenceObjects: MutableSet<RelationshipReference> = entity.relationshipReferenceMap(context, relationshipDescriptor.name)?.get(entity.toRelationshipReference(context)) ?: HashSet()

        if(!existingRelationshipReferenceObjects.isEmpty()) {
            val relationshipEntity:IManagedEntity? = existingRelationshipReferenceObjects.first().toManagedEntity(context, relationshipDescriptor.inverseClass)
            relationshipEntity?.hydrateRelationships(context, transaction)
            val existingRelationshipObject:IManagedEntity? = entity[context, entityDescriptor, relationshipDescriptor.name]
            if(existingRelationshipObject != null && relationshipEntity != null)
                existingRelationshipObject.copy(relationshipEntity, context)
            else
                entity[context, entityDescriptor, relationshipDescriptor.name] = relationshipEntity
        } else {
            entity[context, entityDescriptor, relationshipDescriptor.name] = null
        }
    }

    /**
     * Batch Save all relationship ids
     *
     * @param entity Entity to update
     * @param relationshipIdentifiers Relationship references
     */
    @Throws(OnyxException::class)
    override fun updateAll(entity: IManagedEntity, relationshipIdentifiers: MutableSet<RelationshipReference>) = Unit
}
