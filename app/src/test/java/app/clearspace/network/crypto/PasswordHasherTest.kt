package app.clearspace.network.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun testHashingAndVerification() {
        val password = "my_super_secret_password".toCharArray()
        
        val (salt, hash) = PasswordHasher.hash(password, clearPassword = false)
        
        // Verify with correct password
        val isCorrect = PasswordHasher.verify("my_super_secret_password".toCharArray(), salt, hash)
        assertTrue("Verification should succeed for correct password", isCorrect)
        
        // Verify with wrong password
        val isWrong = PasswordHasher.verify("wrong_password".toCharArray(), salt, hash)
        assertFalse("Verification should fail for incorrect password", isWrong)
    }
}
