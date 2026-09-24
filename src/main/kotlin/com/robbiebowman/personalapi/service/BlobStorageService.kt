package com.robbiebowman.personalapi.service

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.BlobContainerClientBuilder
import com.azure.storage.blob.models.BlobErrorCode
import com.azure.storage.blob.models.BlobStorageException
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BlobStorageService {

    private val logger = LoggerFactory.getLogger(BlobStorageService::class.java)

    @Value("\${azure_blob_connection_string}")
    private val connectionString: String? = null

    private val gson = Gson()

    private fun <T> logBlobFailure(operation: String, containerName: String, action: () -> T): T {
        return try {
            action()
        } catch (e: RuntimeException) {
            val storageError = e as? BlobStorageException
            logger.error(
                "Blob operation failed: operation={}, container={}, exception={}, cause={}, status={}, errorCode={}",
                operation,
                containerName,
                e.javaClass.name,
                e.cause?.javaClass?.name,
                storageError?.statusCode,
                storageError?.errorCode
            )
            throw e
        }
    }

    fun <T> uploadToBlobStorage(containerName: String, blobName: String, thing: T, overwrite: Boolean = true) {
        logBlobFailure("upload", containerName) {
            val json = gson.toJson(thing)
            val binaryData = BinaryData.fromBytes(json.toByteArray())

            val blobClient = BlobClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .blobName(blobName)
                .buildClient()

            blobClient.upload(binaryData, overwrite)
        }
    }

    fun <T> getFromBlobStorage(containerName: String, blobName: String, clazz: Class<T>): T? {
        return logBlobFailure("download", containerName) {
            val blobClient = BlobClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .blobName(blobName)
                .buildClient()

            try {
                val binaryData = blobClient.downloadContent()
                gson.fromJson(binaryData.toString(), clazz)
            } catch (e: BlobStorageException) {
                if (e.errorCode == BlobErrorCode.BLOB_NOT_FOUND) null
                else throw e
            }
        }
    }

    fun getAllFileNamesInContainer(containerName: String): List<String> {
        return logBlobFailure("list", containerName) {
            val containerClient = BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient()

            containerClient.listBlobs()
                .map { it.name }
                .toList()
        }
    }
}
