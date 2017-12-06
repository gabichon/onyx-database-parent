package com.onyx.entity

import com.onyx.persistence.ManagedEntity
import com.onyx.persistence.annotations.Attribute
import com.onyx.persistence.annotations.Entity
import com.onyx.persistence.annotations.Identifier
import com.onyx.persistence.annotations.Relationship
import com.onyx.persistence.annotations.values.CascadePolicy
import com.onyx.persistence.annotations.values.FetchPolicy
import com.onyx.persistence.annotations.values.IdentifierGenerator
import com.onyx.persistence.annotations.values.RelationshipType

@Suppress("UNUSED")
@Entity(fileName = "query.dat")
class SystemDirectory(

        @Identifier(generator = IdentifierGenerator.SEQUENCE)
        var directoryId:Long = 0,

        @Attribute
        override var name:String = "",

        @Relationship(type = RelationshipType.ONE_TO_MANY,
                inverseClass = SystemDirectory::class,
                loadFactor = 2,
                fetchPolicy = FetchPolicy.EAGER,
                inverse = "parent",
                cascadePolicy = CascadePolicy.NONE)
        var children:MutableList<SystemDirectory> = ArrayList(),

        @Relationship(type = RelationshipType.MANY_TO_ONE,
                inverseClass = SystemDirectory::class,
                loadFactor = 2,
                inverse = "children",
                cascadePolicy = CascadePolicy.NONE)
        var parent: SystemDirectory? = null,

        @Relationship(type = RelationshipType.ONE_TO_MANY,
                inverse = "directory",
                inverseClass = SystemQuery::class,
                fetchPolicy = FetchPolicy.EAGER,
                loadFactor = 2,
                cascadePolicy = CascadePolicy.NONE)
        var queries:MutableList<SystemQuery> = ArrayList()

) : ManagedEntity(), NamedEntity {

    @Suppress("UNCHECKED_CAST")
    val allChildren: List<NamedEntity>
        get() = (children + queries) as List<NamedEntity>
}
