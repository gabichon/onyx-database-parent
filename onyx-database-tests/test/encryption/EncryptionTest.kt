package encryption

import com.onyx.interactors.encryption.impl.DefaultEncryptionInteractor
import org.junit.Test

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 *
 * @author Chris Osborn
 */
class EncryptionTest {

    @Test
    fun shouldEncrypt() {
        val encryptedText = DefaultEncryptionInteractor.encrypt("admin_admin")
        assertNotNull(encryptedText, "Encrypted text should not be null")
    }

    @Test
    fun shouldDecrypt() {
        val str = "admin_admin"
        val encryptedText = DefaultEncryptionInteractor.encrypt(str)
        val decryptedText = DefaultEncryptionInteractor.decrypt(encryptedText!!)
        assertEquals(str,  decryptedText, "Decrypted text should match original")
    }

    @Test
    fun shouldFail() {
        val str = "admin_admin"
        val str2 = "admin_password"
        val decryptedText = DefaultEncryptionInteractor.decrypt(DefaultEncryptionInteractor.encrypt(str2)!!)

        assertNotEquals(str, decryptedText, "Decrypted should not match")
    }

    @Test
    fun shouldWriteToFile() {
        val textToSave = "asdfasdf"

        //Create a temporary file
        val file = File(FILE_LOCATION + "/tmp")
        file.parentFile.mkdirs()
        file.createNewFile()
        //Next, write the encrypted bytes to the file.

        FileOutputStream(file).use { fileStream ->
            fileStream.write(DefaultEncryptionInteractor.encrypt(textToSave)!!.toByteArray(StandardCharsets.UTF_8))
            fileStream.close()
        }

        val savedText = String(Files.readAllBytes(Paths.get(file.absolutePath)))

        //make assertions
        assertEquals(savedText, DefaultEncryptionInteractor.encrypt(textToSave), "Encrypted text does not match")

        //cleanup file
        file.delete()
    }

    companion object {
        private val FILE_LOCATION = "C:/Sandbox/Onyx/Tests/encryption"
    }
}
