package com.onyx.extension

import com.onyx.descriptor.EntityDescriptor
import com.onyx.descriptor.RelationshipDescriptor
import com.onyx.exception.OnyxException
import com.onyx.fetch.PartitionReference
import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.context.SchemaContext
import com.onyx.relationship.EntityRelationshipManager
import com.onyx.relationship.RelationshipController
import com.onyx.relationship.RelationshipReference
import java.util.ArrayList

/**
 * Get relationship controller
 *
 * @param context Schema Context entity belongs to
 * @param name Name of property that corresponds to the relationship
 * @return Corresponding relationship controller for an entity and specified property name
 */
fun IManagedEntity.relationshipController(context: SchemaContext, name:String):RelationshipController {
    val entityDescriptor = descriptor(context)
    return context.getRelationshipController(entityDescriptor.relationships[name]!!)
}

/**
 * Save all relationships within an entity
 *
 * @param context Schema Context entity belongs to
 * @param manager Determines and prevents recursive relationship persistence
 *
 * @since 2.0.0
 */
@JvmOverloads
fun IManagedEntity.saveRelationships(context: SchemaContext, manager: EntityRelationshipManager = EntityRelationshipManager()) {
    val descriptor = descriptor(context)
    if (descriptor.hasRelationships) {
        if (!manager.contains(this, descriptor.identifier)) {
            manager.add(this, descriptor.identifier)
            descriptor.relationships.values.forEach { relationshipController(context, it.name).saveRelationshipForEntity(this, manager) }
        }
    }
}

/**
 * Delete relationship references for an entity
 *
 * @param context Schema Context entity belongs to
 * @param manager Entity relationship manager prevents recursive deletes.  Not required
 * @since 2.0.0
 */
@JvmOverloads
fun IManagedEntity.deleteRelationships(context: SchemaContext, manager: EntityRelationshipManager = EntityRelationshipManager()) {
    val descriptor      = descriptor(context)

    val entityId = RelationshipReference(identifier(context), partitionId(context))
    descriptor.relationships.values.forEach { relationshipController(context, it.name).deleteRelationshipForEntity(entityId, manager) }
}

/**
 * Gets hydrated relationships from the store.  Also note, this will pass 1 to 1 as a list.  It will require further
 * work to aggregate from list if the intent is to hydrate.
 *
 * @param context Schema Context entity belongs to
 * @param relationship Name of relationship
 * @param entityReference Entity reference within entire context.  This should take into account whether it is in a partition.
 *                        If none is passed in, it will look it up.
 *
 * @since 2.0.0
 */
@Throws(OnyxException::class)
fun IManagedEntity.getRelationshipFromStore(context: SchemaContext, relationship: String, entityReference:PartitionReference? = reference(context)): List<IManagedEntity>? {
    val slices = relationship.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    var descriptor: EntityDescriptor? = descriptor(context)
    var relationshipDescriptor: RelationshipDescriptor? = null

    // Iterate through and grab the right descriptor
    try {
        slices
            .find { descriptor!!.relationships[it] != null }
            .let {
                relationshipDescriptor = descriptor!!.relationships[it]
                descriptor = context.getBaseDescriptorForEntity(relationshipDescriptor!!.inverseClass)
            }
    } catch (e: NullPointerException) {
        return null
    }

    val relationshipController = context.getRelationshipController(relationshipDescriptor!!)
    val relationshipReferences = relationshipController.getRelationshipIdentifiersWithReferenceId(entityReference)

    val defaultRecordController = context.getRecordController(descriptor!!)
    val entities = ArrayList<IManagedEntity>()

    relationshipReferences.forEach {
        if (it.partitionId <= 0) {
            val relationshipEntity = defaultRecordController.getWithId(it.identifier)
            if (relationshipEntity != null)
                entities.add(relationshipEntity)
        } else {
            val relationshipEntity = recordController(context).getWithId(it.identifier)
            if (relationshipEntity != null)
                entities.add(relationshipEntity)
        }
    }

    return entities
}