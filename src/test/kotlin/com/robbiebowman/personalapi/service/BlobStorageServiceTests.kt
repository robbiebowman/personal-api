package com.robbiebowman.personalapi.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(OutputCaptureExtension::class)
class BlobStorageServiceTests {

    @Test
    fun logsBlobFailureWithoutLoggingTheConnectionString(output: CapturedOutput) {
        val service = BlobStorageService()
        val connectionString = "sensitive-invalid-connection-string"
        ReflectionTestUtils.setField(service, "connectionString", connectionString)

        assertThrows(RuntimeException::class.java) {
            service.getFromBlobStorage("periodic-table", "index", String::class.java)
        }

        assertTrue(output.out.contains("Blob operation failed: operation=download, container=periodic-table"))
        assertFalse(output.out.contains(connectionString))
    }
}
