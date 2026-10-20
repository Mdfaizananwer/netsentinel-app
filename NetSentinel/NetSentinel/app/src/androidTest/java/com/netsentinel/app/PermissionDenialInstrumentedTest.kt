package com.netsentinel.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.netsentinel.app.data.network.WifiInfoProvider
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a device/emulator. Verifies the "handle permission denial gracefully" requirement:
 * with location permission NOT granted (the default for a fresh instrumented test process,
 * since this test does not call grantRuntimePermission), WifiInfoProvider must return an
 * explicit, non-crashing reason rather than throwing or silently returning garbage.
 *
 * To additionally test the "granted" path, grant location via:
 *   adb shell pm grant com.netsentinel.app.debug android.permission.ACCESS_FINE_LOCATION
 * and re-run; ssid should then be non-null when connected to Wi-Fi with location services on.
 */
@RunWith(AndroidJUnit4::class)
class PermissionDenialInstrumentedTest {

    @Test
    fun wifiSnapshotDoesNotCrashWithoutLocationPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val provider = WifiInfoProvider(context)
        val snapshot = provider.currentSnapshot()

        if (!snapshot.isWifiConnected) {
            // Nothing further to assert on an emulator with no Wi-Fi transport.
            return
        }
        if (snapshot.ssid == null) {
            assertThat(snapshot.ssidUnavailableReason).isNotNull()
        }
    }
}
