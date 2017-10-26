package web.exception

import category.WebServerTests
import com.onyx.exception.*
import com.onyx.persistence.IManagedEntity
import entities.ValidateRequiredIDEntity
import entities.ValidationEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import web.base.BaseTest

import java.io.IOException

/**
 * Created by timothy.osborn on 1/21/15.
 */
@Category(WebServerTests::class)
class TestEntityValidation : BaseTest() {

    @Before
    @Throws(InitializationException::class)
    fun before() {
        initialize()
    }

    @After
    @Throws(IOException::class)
    fun after() {
        shutdown()
    }

    @Test(expected = AttributeNonNullException::class)
    @Throws(OnyxException::class)
    fun testNullValue() {
        val validationEntity = ValidationEntity()
        validationEntity.id = 3L
        manager.saveEntity<IManagedEntity>(validationEntity)
    }

    @Test(expected = AttributeSizeException::class)
    @Throws(OnyxException::class)
    fun testAttributeSizeException() {
        val validationEntity = ValidationEntity()
        validationEntity.id = 3L
        validationEntity.requiredString = "ASFD"
        validationEntity.maxSizeString = "ASD1234569a"
        manager.saveEntity<IManagedEntity>(validationEntity)
    }

    @Test
    @Throws(OnyxException::class)
    fun testValidAttributeSizeException() {
        val validationEntity = ValidationEntity()
        validationEntity.id = 3L
        validationEntity.requiredString = "ASFD"
        validationEntity.maxSizeString = "ASD1234569"
        manager.saveEntity<IManagedEntity>(validationEntity)
    }

    @Test(expected = IdentifierRequiredException::class)
    @Throws(OnyxException::class)
    fun testRequiredIDException() {
        val validationEntity = ValidateRequiredIDEntity()
        validationEntity.requiredString = "ASFD"
        validationEntity.maxSizeString = "ASD1234569"
        manager.saveEntity<IManagedEntity>(validationEntity)
    }

}
