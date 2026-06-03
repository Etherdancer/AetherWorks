package org.example.aetherworks

import android.content.Intent
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.example.aetherworks.discovery.P2PServer
import org.example.aetherworks.discovery.P2PClient
import kotlinx.coroutines.runBlocking
import org.example.aetherworks.IAetherIpc

@RunWith(AndroidJUnit4::class)
class P2PIntegrationTest {

    @Test
    fun testP2PIndexFetch() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Mock AIDL service to simulate AetherDatabaseService response
        val mockIpc = object : IAetherIpc.Stub() {
            override fun getIndex(): String = "[{\"contentHash\":\"hash123\",\"title\":\"Test Title\",\"authorAlias\":\"TestAlias\",\"timestamp\":12345,\"thumbnailBase64\":null,\"categoryFlags\":\"Other\",\"emotionFlags\":\"Happy\",\"reputationScore\":0}]"
            override fun getContent(hash: String?): String = ""
            override fun getProfile(): String = ""
            override fun getRelayIndex(currentTimeMillis: Long): String = "[]"
            override fun getRelayPacket(currentTimeMillis: Long, packetId: String?): String = ""
        }
        
        val server = P2PServer(appContext, mockIpc)
        val port = server.start()
        assertTrue("Server started on a valid port", port > 0)
        
        val result = P2PClient.fetchIndex("127.0.0.1", port)
        assertNotNull("Should fetch non-null index", result)
        assertEquals("Should fetch one item", 1, result?.size)
        assertEquals("hash123", result?.first()?.contentHash)
        assertEquals("Test Title", result?.first()?.title)
        
        server.stop()
    }
}
