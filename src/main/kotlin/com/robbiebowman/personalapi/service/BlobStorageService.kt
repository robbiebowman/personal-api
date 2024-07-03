package com.robbiebowman.personalapi.service

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.models.BlobErrorCode
import com.azure.storage.blob.models.BlobStorageException
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BlobStorageService(
) {

    @Value("\${azure_blob_connection_string}")
    private val connectionString: String? = null

    private val gson = Gson()

    fun <T> uploadToBlobStorage(containerName: String, blobName: String, thing: T, overwrite: Boolean = true) {
        val json = gson.toJson(thing)
        val binaryData = BinaryData.fromBytes(json.toByteArray())

        val blobClient = BlobClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .blobName(blobName)
            .buildClient()

        blobClient.upload(binaryData, overwrite)
    }

    fun <T> getFromBlobStorage(containerName: String, blobName: String, clazz: Class<T>): T? {
        val blobClient = BlobClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .blobName(blobName)
            .buildClient()

        return try {
            val binaryData = blobClient.downloadContent()
            gson.fromJson(binaryData.toString(), clazz)
        } catch (e: BlobStorageException) {
            if (e.errorCode == BlobErrorCode.BLOB_NOT_FOUND) null
            else throw e
        }
    }

}