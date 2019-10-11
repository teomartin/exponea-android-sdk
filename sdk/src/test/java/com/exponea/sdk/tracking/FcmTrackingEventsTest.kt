package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FcmTrackingEventsTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        const val token = "FirebaseCloudToken#"
        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = MockWebServer()
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 10
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        skipInstallEvent()
        Exponea.init(context, configuration)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository

        // Clean event repository for testing purposes
        repo.clear()
    }

    @Test
    fun testTokenEventAdded_ShouldSuccess() {
        // Track token
        Exponea.trackPushToken(
            fcmToken = token
        )

        assertEquals(1, repo.all().size)

        val props = repo.all().first().item.properties
        val token = props?.let {
            it["google_push_notification_id"]
        }
        assertEquals("FirebaseCloudToken#", token)
    }

    @Test
    fun testTrackDeliveredPush_ShouldSuccess() {
        // Track delivered push
        Exponea.trackDeliveredPush()

        // Check if event was added to db
        val event = repo.all().first()

        assertEquals("campaign", event.item.type)
        assertEquals(1, repo.all().size)

        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        // Flush this event and check it was sent successfully
        val lock = CountDownLatch(1)
        Exponea.flushData()
        Exponea.component.flushManager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
            lock.countDown()
        }
        lock.await()

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }

    @Test
    fun testTrackClickedPush_ShouldSuccess() {
        // Track clicked push
        Exponea.trackClickedPush()

        // Check if event was added to db
        val event = repo.all().first()

        assertEquals("campaign", event.item.type)
        assertEquals(1, repo.all().size)

        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        val lock = CountDownLatch(1)
        Exponea.flushData()
        Exponea.component.flushManager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
            lock.countDown()
        }
        lock.await()

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }
}
