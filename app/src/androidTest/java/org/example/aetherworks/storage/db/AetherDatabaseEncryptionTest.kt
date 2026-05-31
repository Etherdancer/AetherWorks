package org.example.aetherworks.storage.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteException
import org.example.aetherworks.storage.db.entity.SecurityLogEntry
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AetherDatabaseEncryptionTest {

    private lateinit var context: Context
    private val dbName = "aether_private.db"
    private val goodPassphrase = "correct_horse_battery_staple".toByteArray()
    private val badPassphrase = "wrong_password".toByteArray()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(dbName).delete()
    }

    @After
    fun teardown() {
        context.getDatabasePath(dbName).delete()
    }

    @Test
    fun testEncryptedDatabase_rejectsWrongKey() = runBlocking {
        // Create DB with good passphrase
        val goodDb = AetherDatabase.getPrivateDatabase(context, goodPassphrase)
        goodDb.securityLogDao().insert(SecurityLogEntry(eventType = "TEST", timestamp = 0L, detail = "Init"))
        goodDb.close()

        // Attempt to open with bad passphrase
        val badDb = AetherDatabase.getPrivateDatabase(context, badPassphrase)
        
        // Operation should throw an exception from SQLCipher
        assertThrows(SQLiteException::class.java) {
            runBlocking {
                badDb.securityLogDao().getRecentLogs(1)
            }
        }
    }
}
