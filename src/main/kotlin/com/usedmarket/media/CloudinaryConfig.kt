package com.usedmarket.media

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CloudinaryConfig(
    @Value("\${app.cloudinary.cloud-name}") private val cloudName: String,
    @Value("\${app.cloudinary.api-key}") private val apiKey: String,
    @Value("\${app.cloudinary.api-secret}") private val apiSecret: String
) {

    @Bean
    fun cloudinary(): Cloudinary = Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        )
    )
}
