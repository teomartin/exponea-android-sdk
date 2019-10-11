package com.exponea.sdk.tracking

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.testutil.ExponeaSDKTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.BeforeClass

open class CampaignSessionTests_Base : ExponeaSDKTest() {

    companion object {

        val UTM_SOURCE = "testSource"
        val UTM_CAMPAIGN = "campaign001"
        val UTM_CONTENT = "campaignTestContent"
        val UTM_MEDIUM = "medium_98765rfghjmnb"
        val UTM_TERM = "term_098765rtyuk"
        val XNPE_CMP = "3456476768iu-ilkujyfgcvbi7gukgvbnp-oilgvjkjyhgdxcvbiu"
        val CAMPAIGN_URL = "http://example.com/route/to/campaing" +
                "?utm_source=" + UTM_SOURCE +
                "&utm_campaign=" + UTM_CAMPAIGN +
                "&utm_content=" + UTM_CONTENT +
                "&utm_medium=" + UTM_MEDIUM +
                "&utm_term=" + UTM_TERM +
                "&xnpe_cmp=" + XNPE_CMP

        val configuration = ExponeaConfiguration()
        val server = MockWebServer()

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            configuration.projectToken = "TestToken"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 10
            configuration.automaticSessionTracking = true
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            server.shutdown()
        }

        fun initExponea(context: Context) {
            skipInstallEvent()
            Exponea.init(context, configuration)
            waitUntilFlushed()
            Exponea.flushMode = FlushMode.MANUAL
        }

        fun createDeeplinkIntent() = Intent().apply {
            this.action = Intent.ACTION_VIEW
            this.addCategory(Intent.CATEGORY_DEFAULT)
            this.addCategory(Intent.CATEGORY_BROWSABLE)
            this.data = Uri.parse(CAMPAIGN_URL)
        }
    }
}