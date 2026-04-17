/*
 * Adbrowser - A modern cross-platform Android file manager powered by ADB.
 * Copyright (C) 2019 HighCapable
 * https://github.com/BetterAndroid/Adbrowser
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2026/4/18.
 */
package com.highcapable.adbrowser.core.adb

import com.highcapable.adbrowser.core.adb.model.pairing.PairingDevice
import com.highcapable.adbrowser.core.adb.model.pairing.QrPairingSession
import com.highcapable.adbrowser.core.logging.LogServiceImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdbClientWirelessTest {

    @Test
    fun createQrPairingSessionReturnsAndroidStudioCompatiblePayload() = runBlocking {
        FakeAdbHarness().use { harness ->
            val client = harness.createClient()

            val result = client.createQrPairingSession()

            assertTrue(result.isOk, result.errorMessage ?: "QR pairing session should be created.")
            val session = assertNotNull(result.data)
            assertTrue(session.serviceName.startsWith("studio-"))
            assertEquals(17, session.serviceName.length)
            assertEquals(
                "WIFI:T:ADB;S:${session.serviceName};P:${session.password};;",
                session.qrContent
            )
        }
    }

    @Test
    fun observePairingDevicesReturnsOnlyPairingMdnsServices() = runBlocking {
        FakeAdbHarness().use { harness ->
            harness.writeMdnsServices(
                """
                studio-abc123 _adb-tls-pairing._tcp. 192.168.0.5:37123
                Pixel-8 _adb-tls-connect._tcp. 192.168.0.5:43210
                """
            )
            val client = harness.createClient()

            val result = client.observePairingDevices(pollIntervalMillis = 100).first()

            assertTrue(result.isOk, result.errorMessage ?: "Pairing devices should be discovered.")
            val devices = result.data.orEmpty()
            assertEquals(1, devices.size)
            assertEquals("studio-abc123", devices.single().serviceName)
            assertEquals("192.168.0.5:37123", devices.single().address)
        }
    }

    @Test
    fun completeQrPairingWaitsForRequestedServiceAndRunsAdbPair() = runBlocking {
        FakeAdbHarness().use { harness ->
            val session = QrPairingSession(
                serviceName = "studio-target42",
                password = "SecretCode42",
                qrContent = "WIFI:T:ADB;S:studio-target42;P:SecretCode42;;"
            )
            harness.writeMdnsServices("")
            harness.writeExpectedPairTarget("192.168.0.9:37123")
            harness.writeExpectedPairCode(session.password)
            val client = harness.createClient()

            val updater = launch {
                delay(200)
                harness.writeMdnsServices(
                    """
                    other-service _adb-tls-pairing._tcp. 192.168.0.7:37123
                    ${session.serviceName} _adb-tls-pairing._tcp. 192.168.0.9:37123
                    """
                )
            }

            val result = client.completeQrPairing(
                session = session,
                timeoutMillis = 3_000L,
                pollIntervalMillis = 100L
            )
            updater.join()

            assertTrue(result.isOk, result.errorMessage ?: "QR pairing should succeed.")
            val pairingDevice = assertNotNull(result.data)
            assertEquals("192.168.0.9:37123", pairingDevice.address)
            assertEquals("pair 192.168.0.9:37123 SecretCode42", harness.readRecordedCommand())
        }
    }

    @Test
    fun pairDeviceRunsAdbPairWithProvidedCode() = runBlocking {
        FakeAdbHarness().use { harness ->
            val device = PairingDevice(
                serviceName = "studio-pair",
                host = "192.168.0.20",
                port = 40000
            )
            harness.writeExpectedPairTarget(device.address)
            harness.writeExpectedPairCode("123456")
            val client = harness.createClient()

            val result = client.pairDevice(device, "123456")

            assertTrue(result.isOk, result.errorMessage ?: "Manual pairing should succeed.")
            assertEquals("pair ${device.address} 123456", harness.readRecordedCommand())
        }
    }

    @Test
    fun connectDeviceRunsAdbConnectWithNormalizedAddress() = runBlocking {
        FakeAdbHarness().use { harness ->
            harness.writeExpectedConnectTarget("192.168.0.30:5555")
            val client = harness.createClient()

            val result = client.connectDevice(" 192.168.0.30:5555 ")

            assertTrue(result.isOk, result.errorMessage ?: "Direct connect should succeed.")
            assertEquals("connect 192.168.0.30:5555", harness.readRecordedCommand())
        }
    }

    @Test
    fun connectDeviceAddsDefaultPortWhenMissing() = runBlocking {
        FakeAdbHarness().use { harness ->
            harness.writeExpectedConnectTarget("192.168.0.30:5555")
            val client = harness.createClient()

            val result = client.connectDevice("192.168.0.30")

            assertTrue(result.isOk, result.errorMessage ?: "Direct connect should use the default adb port.")
            assertEquals("connect 192.168.0.30:5555", harness.readRecordedCommand())
        }
    }

    @Test
    fun connectDeviceTreatsMissingConnectedMarkerAsFailureEvenWhenExitCodeIsZero() = runBlocking {
        FakeAdbHarness().use { harness ->
            harness.writeExpectedConnectTarget("127.0.0.1:5555")
            harness.writeConnectOutput("failed to connect to '127.0.0.1:5555': Connection refused")
            val client = harness.createClient()

            val result = client.connectDevice("127.0.0.1:5555")

            assertTrue(!result.isOk, "Backend should reject adb connect output without a real success marker.")
            assertEquals(
                "failed to connect to '127.0.0.1:5555': Connection refused",
                result.errorMessage
            )
        }
    }

    private class FakeAdbHarness : AutoCloseable {

        private val tempDir = Files.createTempDirectory("adb-client-wireless-test")
        private val scriptPath = tempDir.resolve("fake-adb.sh")
        private val mdnsServicesPath = tempDir.resolve("mdns-services.txt")
        private val recordPath = tempDir.resolve("record.txt")
        private val expectedPairTargetPath = tempDir.resolve("expected-pair-target.txt")
        private val expectedPairCodePath = tempDir.resolve("expected-pair-code.txt")
        private val expectedConnectTargetPath = tempDir.resolve("expected-connect-target.txt")
        private val connectOutputPath = tempDir.resolve("connect-output.txt")

        init {
            scriptPath.toFile().writeText(
                """
                #!/bin/sh
                script_dir="${'$'}(CDPATH= cd -- "${'$'}(dirname -- "${'$'}0")" && pwd)"
                mdns_file="${'$'}script_dir/mdns-services.txt"
                record_file="${'$'}script_dir/record.txt"
                expected_pair_target_file="${'$'}script_dir/expected-pair-target.txt"
                expected_pair_code_file="${'$'}script_dir/expected-pair-code.txt"
                expected_connect_target_file="${'$'}script_dir/expected-connect-target.txt"
                connect_output_file="${'$'}script_dir/connect-output.txt"
                
                command="${'$'}1"
                shift || true
                
                case "${'$'}command" in
                  version)
                    echo "Android Debug Bridge version 1.0.41"
                    exit 0
                    ;;
                  mdns)
                    if [ "${'$'}1" = "services" ]; then
                      if [ -f "${'$'}mdns_file" ]; then
                        cat "${'$'}mdns_file"
                      fi
                      exit 0
                    fi
                    ;;
                  pair)
                    printf 'pair %s %s\n' "${'$'}1" "${'$'}2" > "${'$'}record_file"
                    if [ -f "${'$'}expected_pair_target_file" ] && [ "${'$'}(cat "${'$'}expected_pair_target_file")" != "${'$'}1" ]; then
                      echo "unexpected pair target: ${'$'}1" >&2
                      exit 1
                    fi
                    if [ -f "${'$'}expected_pair_code_file" ] && [ "${'$'}(cat "${'$'}expected_pair_code_file")" != "${'$'}2" ]; then
                      echo "unexpected pair code: ${'$'}2" >&2
                      exit 1
                    fi
                    echo "Successfully paired to ${'$'}1"
                    exit 0
                    ;;
                  connect)
                    printf 'connect %s\n' "${'$'}1" > "${'$'}record_file"
                    if [ -f "${'$'}expected_connect_target_file" ] && [ "${'$'}(cat "${'$'}expected_connect_target_file")" != "${'$'}1" ]; then
                      echo "unexpected connect target: ${'$'}1" >&2
                      exit 1
                    fi
                    if [ -f "${'$'}connect_output_file" ]; then
                      cat "${'$'}connect_output_file"
                    else
                      echo "connected to ${'$'}1"
                    fi
                    exit 0
                    ;;
                esac
                
                echo "unsupported command: ${'$'}command ${'$'}*" >&2
                exit 1
                """.trimIndent()
            )
            scriptPath.toFile().setExecutable(true)
        }

        fun createClient() = AdbClientImpl(
            environment = AdbEnvironment(
                adbExecPath = { scriptPath.toString() },
                useSuperuser = { false }
            ),
            logService = LogServiceImpl()
        )

        fun writeMdnsServices(content: String) {
            Files.writeString(mdnsServicesPath, content.trimIndent().trim())
        }

        fun writeExpectedPairTarget(value: String) {
            Files.writeString(expectedPairTargetPath, value)
        }

        fun writeExpectedPairCode(value: String) {
            Files.writeString(expectedPairCodePath, value)
        }

        fun writeExpectedConnectTarget(value: String) {
            Files.writeString(expectedConnectTargetPath, value)
        }

        fun writeConnectOutput(value: String) {
            Files.writeString(connectOutputPath, value)
        }

        fun readRecordedCommand(): String {
            assertTrue(Files.exists(recordPath), "Fake adb command was not recorded.")
            return recordPath.readText().trim()
        }

        override fun close() {
            tempDir.toFile().deleteRecursively()
        }
    }
}