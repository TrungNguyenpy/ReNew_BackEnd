package com.usedmarket.media

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.usedmarket.common.exception.BadRequestException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CloudinaryService(
    private val cloudinary: Cloudinary
) {

    /**
     * Uploads a file to Cloudinary under the given folder (e.g. "products/{productId}").
     * Shared by every module that stores images/video (ProductImage, ReviewImage,
     * TradeInItem...) so upload behavior stays consistent across the app.
     */
    fun upload(file: MultipartFile, folder: String): CloudinaryUploadResult {
        if (file.isEmpty) {
            throw BadRequestException("Uploaded file is empty")
        }

        val options = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "auto"
        )

        @Suppress("UNCHECKED_CAST")
        val result = cloudinary.uploader().upload(file.bytes, options) as Map<String, Any>

        return CloudinaryUploadResult(
            url = result["secure_url"] as String,
            publicId = result["public_id"] as String
        )
    }

    fun delete(publicId: String) {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
    }
}
