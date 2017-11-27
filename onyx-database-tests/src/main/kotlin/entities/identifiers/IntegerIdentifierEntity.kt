package entities.identifiers

import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.annotations.Attribute
import com.onyx.persistence.annotations.Entity
import com.onyx.persistence.annotations.Identifier
import entities.AbstractEntity

/**
 * Created by timothy.osborn on 11/3/14.
 */
@Entity
class IntegerIdentifierEntity : AbstractEntity(), IManagedEntity {
    @Attribute
    @Identifier
    var identifier: Int = 0

    @Attribute
    var correlation: Int = 0
}
