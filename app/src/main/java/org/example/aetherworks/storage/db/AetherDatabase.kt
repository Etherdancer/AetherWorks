package org.example.aetherworks.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.example.aetherworks.storage.db.dao.ContentDao
import org.example.aetherworks.storage.db.dao.MessageDao
import org.example.aetherworks.storage.db.dao.PeerDao
import org.example.aetherworks.storage.db.dao.SecurityLogDao
import org.example.aetherworks.storage.db.dao.RelayPacketDao
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.ContentFtsEntity
import org.example.aetherworks.storage.db.entity.KnownPeer
import org.example.aetherworks.storage.db.entity.Message
import org.example.aetherworks.storage.db.entity.SecurityLogEntry
import org.example.aetherworks.storage.db.entity.RelayPacket

@Database(
    entities = [
        ContentUnit::class,
        KnownPeer::class,
        Message::class,
        SecurityLogEntry::class,
        ContentFtsEntity::class,
        RelayPacket::class
    ],
    version = 12,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun peerDao(): PeerDao
    abstract fun messageDao(): MessageDao
    abstract fun securityLogDao(): SecurityLogDao
    abstract fun relayPacketDao(): RelayPacketDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `content_units_fts` USING FTS4(`title`, `body`, `categoryFlags`, `authorAlias`, content=`content_units`)")
                database.execSQL("INSERT INTO content_units_fts(content_units_fts, docid, `title`, `body`, `categoryFlags`, `authorAlias`) SELECT 'rebuild', rowid, `title`, `body`, `categoryFlags`, `authorAlias` FROM content_units")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE vault_media ADD COLUMN lastPlaybackPosition INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE vault_media ADD COLUMN playbackSpeed REAL NOT NULL DEFAULT 1.0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `shopping_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `isChecked` INTEGER NOT NULL, `reminderTimeMs` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `quantity` INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `reminderTime` INTEGER")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recipes` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `ingredients` TEXT NOT NULL,
                        `steps` TEXT NOT NULL,
                        `prepTimeMinutes` INTEGER NOT NULL,
                        `cookTimeMinutes` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `relay_packets` (
                        `packetId` TEXT NOT NULL,
                        `hashedRecipientId` TEXT NOT NULL,
                        `senderAlias` TEXT,
                        `encryptedPayload` BLOB NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `ttlExpiration` INTEGER NOT NULL,
                        PRIMARY KEY(`packetId`)
                    )
                """)
            }
        }

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
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration()
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
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE_SHARED = instance
                instance
            }
        }

        fun getPrivateDatabase(): AetherDatabase {
            return INSTANCE_PRIVATE ?: throw IllegalStateException("Private database not initialized")
        }

        fun getSharedDatabase(): AetherDatabase {
            return INSTANCE_SHARED ?: throw IllegalStateException("Shared database not initialized")
        }
    }
}
