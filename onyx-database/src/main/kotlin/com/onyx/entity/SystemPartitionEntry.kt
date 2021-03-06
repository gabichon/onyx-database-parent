package com.onyx.entity

import com.onyx.descriptor.EntityDescriptor
import com.onyx.descriptor.PartitionDescriptor
import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.annotations.*
import com.onyx.persistence.annotations.values.IdentifierGenerator
import com.onyx.persistence.annotations.values.RelationshipType

/**
 * Created by timothy.osborn on 3/5/15.
 *
 * Partition entity for an entity
 */
@Entity(fileName = "system")
data class SystemPartitionEntry @JvmOverloads constructor(

    @Identifier(generator = IdentifierGenerator.SEQUENCE, loadFactor = 3)
    var primaryKey: Int = 0,

    @Index(loadFactor = 3)
    var id: String? = null,

    @Attribute(size = 1024)
    var value: String = "",

    @Attribute(size = 2048)
    var fileName: String = "",

    @Index(loadFactor = 3)
    var index: Long = 0,

    @Relationship(type = RelationshipType.MANY_TO_ONE, inverseClass = SystemPartition::class, inverse = "entries", loadFactor = 3)
    var partition: SystemPartition? = null

) : AbstractSystemEntity(), IManagedEntity {

    constructor(entityDescriptor: EntityDescriptor, descriptor: PartitionDescriptor, partition: SystemPartition, index: Long):this (
        partition = partition,
        id = entityDescriptor.entityClass.name + descriptor.partitionValue,
        value = descriptor.partitionValue,
        fileName = entityDescriptor.fileName + descriptor.partitionValue,
        index = index,
        primaryKey = 0
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SystemPartitionEntry
        if (primaryKey != other.primaryKey) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryKey
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }

}
