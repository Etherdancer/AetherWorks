package org.example.aetherworks.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import org.example.aetherworks.storage.db.dao.ContentDao
import org.example.aetherworks.storage.db.dao.MessageDao
import org.example.aetherworks.storage.db.dao.PeerDao
import org.example.aetherworks.storage.db.dao.SecurityLogDao
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.KnownPeer
import org.example.aetherworks.storage.db.entity.Message
import org.example.aetherworks.storage.db.entity.SecurityLogEntry

@Database(
    entities = [
        ContentUnit::class,
        KnownPeer::class,
        Message::class,
        SecurityLogEntry::class
    ],
    version = 1,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun peerDao(): PeerDao
    abstract fun messageDao(): MessageDao
    abstract fun securityLogDao(): SecurityLogDao

    companion object {
        @Volatile
        private var INSTANCE_PRIVATE: AetherDatabase? = null

        @Volatile
        private var INSTANCE_SHARED: AetherDatabase? = null

        fun getPrivateDatabase(context: Context, passhprase: ByteArray): AetherDatabase {
            return INSTANCE_PRIVATE ?: synchronized(this) {
                val factory = SupportFactory(passhprase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AetherDatabase::class.java,
                    "aether_private.db"
                )
                .openHelperFactory(factory)
                .build()
                INSTANCE_PRIVATE = instance
                instance
            }
        }

        fun getSharedDatabase(context: Context, passhprase: ByteArray): AetherDatabase {
            return INSTANCE_SHARED ?: synchronized(this) {
                val factory = SupportFactory(passhprase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AetherDatabase::class.java,
                    "aether_shared.db"
                )
                .openHelperFactory(factory)
                .build()
                INSTANCE_SHARED = instance
                instance
            }
        }
    }
}
