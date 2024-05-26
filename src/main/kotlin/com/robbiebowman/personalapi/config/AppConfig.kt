package com.robbiebowman.personalapi.config

import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.security.keyvault.secrets.SecretClient
import com.azure.security.keyvault.secrets.SecretClientBuilder
import com.robbiebowman.personalapi.service.BlobStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {

    @Value("\${azure_app_client_id}")
    private val azureAppClientId: String? = null

    @Value("\${azure_app_password}")
    private val azureAppPassword: String? = null

    @Value("\${azure_app_tenant_id}")
    private val azureAppTenantId: String? = null

    @Value("\${azure_key_vault_url}")
    private val azureKeyVaultUrl: String? = null

    @Bean
    fun secretClient(): SecretClient {
        return SecretClientBuilder()
            .vaultUrl(azureKeyVaultUrl)
            .credential(
                ClientSecretCredentialBuilder().tenantId(azureAppTenantId).clientId(azureAppClientId)
                    .clientSecret(azureAppPassword).build()
            )
            .buildClient()
    }
}