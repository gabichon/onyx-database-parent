package database.list

import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.query.cont
import com.onyx.persistence.query.eq
import com.onyx.persistence.query.neq
import com.onyx.persistence.query.startsWith
import database.base.PrePopulatedDatabaseTest
import entities.AllAttributeForFetch
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.reflect.KClass
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class FullTableScanTest(override var factoryClass: KClass<*>) : PrePopulatedDatabaseTest(factoryClass) {

    @Test
    fun testBasicAnd() {
        val criteria = ("stringValue" eq "Some test string1")
                        .and ("stringValue" startsWith "Some test str")
                        .and ("id" eq "FIRST ONE1")
        val results = manager.list<AllAttributeForFetch>(AllAttributeForFetch::class.java, criteria)
        assertEquals(1, results.size, "Expected 1 result matching criteria")
        assertEquals("Some test string1", results[0].stringValue, "stringValue was not correct")
    }

    @Test
    fun testBasicAndOr() {
        val criteria = ("stringValue" eq "Some test string1")
                    .or("stringValue" eq "Some test string3")

        val results = manager.list<AllAttributeForFetch>(AllAttributeForFetch::class.java, criteria)
        assertEquals(3, results.size, "Expected 3 entities matching criteria")
    }

    @Test
    fun testBasicOrsSub() {
        val criteria = ("stringValue" cont "Some tes") or ("stringValue" cont "Some test string1")
        assertEquals(4, manager.list<IManagedEntity>(AllAttributeForFetch::class.java, criteria).size, "Expected 4 Results in query")
    }

    @Test
    fun testBasicAndSub() {
        val criteria =        ("stringValue" eq   "Some test string1")
                        .and (("stringValue" cont "Some tes") or ("intValue" neq 2))
                        .or   ("stringValue" eq   "Some test string2")
        assertEquals(2, manager.list<IManagedEntity>(AllAttributeForFetch::class.java, criteria).size, "Expected 2 results from query")
    }

    @Test
    fun testBasicOrSub() {
        val criteria =       ("stringValue"  eq   "Some test string1")
                        .or (("stringValue"  cont "Some tes") or ("intValue" neq 2))
                        .or ( "stringValue"  eq   "Some test string2")
        assertEquals(6, manager.list<IManagedEntity>(AllAttributeForFetch::class.java, criteria).size, "Expected 6 results")
    }
}