package memory.list

import category.InMemoryDatabaseTests
import com.onyx.exception.OnyxException
import com.onyx.persistence.query.QueryCriteria
import com.onyx.persistence.query.QueryCriteriaOperator
import entities.AllAttributeEntity
import entities.AllAttributeForFetch
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Created by timothy.osborn on 11/8/14.
 */
@Category(InMemoryDatabaseTests::class)
class ContainsTest : memory.base.PrePopulatedDatabaseTest() {

    @Test
    @Throws(OnyxException::class, InstantiationException::class, IllegalAccessException::class)
    fun testStringContains() {
        val criteriaList = QueryCriteria("stringValue", QueryCriteriaOperator.CONTAINS, "Some test strin")
        val results = manager.list<AllAttributeEntity>(AllAttributeForFetch::class.java, criteriaList)
        Assert.assertEquals(4, results.size.toLong())
    }

    @Test
    @Throws(OnyxException::class, InstantiationException::class, IllegalAccessException::class)
    fun testContainsStringId() {
        val criteriaList = QueryCriteria("id", QueryCriteriaOperator.STARTS_WITH, "FIRST ONE")
        val results = manager.list<AllAttributeEntity>(AllAttributeForFetch::class.java, criteriaList)
        Assert.assertEquals(6, results.size.toLong())
    }

    @Test
    @Throws(OnyxException::class, InstantiationException::class, IllegalAccessException::class)
    fun testStringStartsWith() {
        val criteriaList = QueryCriteria("stringValue", QueryCriteriaOperator.CONTAINS, "ome test strin")
        val results = manager.list<AllAttributeEntity>(AllAttributeForFetch::class.java, criteriaList)
        Assert.assertEquals(4, results.size.toLong())
    }

    @Test
    @Throws(OnyxException::class, InstantiationException::class, IllegalAccessException::class)
    fun testContainsStartsWith() {
        val criteriaList = QueryCriteria("id", QueryCriteriaOperator.CONTAINS, "IRST ONE")
        val results = manager.list<AllAttributeEntity>(AllAttributeForFetch::class.java, criteriaList)
        Assert.assertEquals(6, results.size.toLong())
    }
}

