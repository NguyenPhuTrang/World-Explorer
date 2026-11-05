package com.worldexplorer.world_explorer.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final String CLOUDINARY_HOST = "res.cloudinary.com";
    private static final String UPLOAD_PATH = "/image/upload/";

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // Upload image to Cloudinary
    public Map<String, Object> uploadImage(
            MultipartFile file,
            String folder
    ) throws IOException {

        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image"
                )
        );
    }

    // Delete image from Cloudinary
    public Map<String, Object> deleteImage(
            String publicId
    ) throws IOException {

        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("Public ID must not be blank");
        }

        return cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "invalidate", true
                )
        );
    }

    // Extract Public ID from Cloudinary image URL
    public String extractPublicId(String imageUrl) {

        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Image URL must not be blank");
        }

        URI uri;

        try {
            uri = URI.create(imageUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Cloudinary image URL");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !CLOUDINARY_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Invalid Cloudinary image URL");
        }

        String path = uri.getPath();
        int uploadIndex = path.indexOf(UPLOAD_PATH);

        if (uploadIndex == -1) {
            throw new IllegalArgumentException("Invalid Cloudinary image URL");
        }

        String publicId = path.substring(uploadIndex + UPLOAD_PATH.length());

        // Remove version prefix
        publicId = publicId.replaceFirst("^v\\d+/", "");

        // Remove file extension
        int extensionIndex = publicId.lastIndexOf(".");

        if (extensionIndex != -1) {
            publicId = publicId.substring(0, extensionIndex);
        }

        if (publicId.isBlank()) {
            throw new IllegalArgumentException("Unable to extract Cloudinary public ID");
        }

        return publicId;
    }
}