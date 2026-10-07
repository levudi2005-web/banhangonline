package com.banhangonline.product.storage;

import com.banhangonline.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductImageStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductImageStorageService.class);
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private final S3StorageProperties properties;
    private final LocalImageStorageProperties localProperties;
    private final S3Client client;

    @org.springframework.beans.factory.annotation.Autowired
    public ProductImageStorageService(S3StorageProperties properties,
                                      LocalImageStorageProperties localProperties,
                                      org.springframework.beans.factory.ObjectProvider<S3Client> clients) {
        this.properties = properties;
        this.localProperties = localProperties;
        this.client = clients.getIfAvailable();
    }

    public ProductImageStorageService(S3StorageProperties properties,
                                      org.springframework.beans.factory.ObjectProvider<S3Client> clients) {
        this(properties, new LocalImageStorageProperties(), clients);
    }

    public String upload(Long storeId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("Chọn ảnh sản phẩm trước khi tải lên.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE",
                    "Ảnh sản phẩm không được vượt quá 5 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException error) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_READ_FAILED",
                    "Không thể đọc tệp ảnh đã chọn.");
        }
        ImageFormat format = ImageFormat.from(bytes);
        if (format == null) {
            throw ApiException.validation("Chỉ hỗ trợ ảnh PNG, JPG hoặc WEBP hợp lệ.");
        }
        if (localProperties.isEnabled() && !properties.isEnabled()) {
            return uploadLocal(storeId, bytes, format);
        }
        S3Client s3 = requireClient();
        String key = "stores/" + storeId + "/products/" + UUID.randomUUID() + "." + format.extension;
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(properties.getBucket().trim())
                            .key(key)
                            .contentType(format.contentType)
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (SdkException error) {
            LOGGER.warn("Product image upload failed for store {}: {}", storeId, error.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "IMAGE_STORAGE_FAILED",
                    "Không thể lưu ảnh vào kho lưu trữ. Vui lòng thử lại.");
        }
        return publicUrl(key);
    }

    public boolean deleteIfManaged(Long storeId, String imageUrl) {
        if (imageUrl == null) {
            return false;
        }
        if (localProperties.isEnabled() && !properties.isEnabled()) {
            return deleteLocal(storeId, imageUrl);
        }
        String key = managedObjectKey(storeId, imageUrl);
        if (key == null) {
            return false;
        }
        S3Client s3 = requireClient();
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket().trim())
                    .key(key)
                    .build());
            return true;
        } catch (SdkException error) {
            LOGGER.warn("Product image deletion failed for store {}: {}", storeId, error.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "IMAGE_DELETE_FAILED",
                    "Ảnh đã được gỡ khỏi sản phẩm nhưng chưa thể xóa khỏi kho lưu trữ.");
        }
    }

    public boolean isManagedImageUrl(Long storeId, String imageUrl) {
        if (imageUrl == null) {
            return false;
        }
        if (localProperties.isEnabled() && !properties.isEnabled()) {
            String prefix = "/uploads/stores/" + storeId + "/products/";
            if (!imageUrl.startsWith(prefix)) {
                return false;
            }
            String fileName = imageUrl.substring(prefix.length());
            if (!fileName.matches("[0-9a-fA-F-]{36}\\.(png|jpg|webp)")) {
                return false;
            }
            Path root = localRoot();
            Path target = root.resolve("stores").resolve(storeId.toString()).resolve("products")
                    .resolve(fileName).normalize();
            return target.startsWith(root) && Files.isRegularFile(target);
        }
        return managedObjectKey(storeId, imageUrl) != null;
    }

    private S3Client requireClient() {
        if (!properties.isEnabled() || client == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_STORAGE_UNAVAILABLE",
                    "Tính năng tải ảnh chưa được cấu hình.");
        }
        return client;
    }

    private String uploadLocal(Long storeId, byte[] bytes, ImageFormat format) {
        Path root = localRoot();
        String fileName = UUID.randomUUID() + "." + format.extension;
        Path directory = root.resolve("stores").resolve(storeId.toString()).resolve("products").normalize();
        Path target = directory.resolve(fileName).normalize();
        if (!directory.startsWith(root) || !target.startsWith(root)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_STORAGE_FAILED",
                    "Không thể lưu ảnh vào kho lưu trữ.");
        }
        try {
            Files.createDirectories(directory);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException error) {
            LOGGER.warn("Local product image upload failed for store {}: {}", storeId, error.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_STORAGE_FAILED",
                    "Không thể lưu ảnh vào kho lưu trữ.");
        }
        return "/uploads/stores/" + storeId + "/products/" + fileName;
    }

    private boolean deleteLocal(Long storeId, String imageUrl) {
        final String prefix = "/uploads/stores/" + storeId + "/products/";
        if (!imageUrl.startsWith(prefix)) {
            return false;
        }
        String fileName = imageUrl.substring(prefix.length());
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.(png|jpg|webp)")) {
            return false;
        }
        Path root = localRoot();
        Path target = root.resolve("stores").resolve(storeId.toString()).resolve("products")
                .resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            return false;
        }
        try {
            return Files.deleteIfExists(target);
        } catch (IOException error) {
            LOGGER.warn("Local product image deletion failed for store {}: {}", storeId, error.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_DELETE_FAILED",
                    "Ảnh đã được gỡ khỏi sản phẩm nhưng chưa thể xóa khỏi kho lưu trữ.");
        }
    }

    private Path localRoot() {
        return Path.of(localProperties.getDirectory()).toAbsolutePath().normalize();
    }

    private String publicUrl(String key) {
        return properties.getPublicBaseUrl().trim().replaceAll("/+$", "") + "/" + key;
    }

    private String managedObjectKey(Long storeId, String imageUrl) {
        String baseUrl = properties.getPublicBaseUrl().trim().replaceAll("/+$", "");
        String storePrefix = baseUrl + "/stores/" + storeId + "/products/";
        if (!imageUrl.startsWith(storePrefix)) {
            return null;
        }
        String fileName = imageUrl.substring(storePrefix.length());
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.(png|jpg|webp)")) {
            return null;
        }
        return "stores/" + storeId + "/products/" + fileName.toLowerCase(Locale.ROOT);
    }

    private enum ImageFormat {
        PNG("png", "image/png"),
        JPG("jpg", "image/jpeg"),
        WEBP("webp", "image/webp");

        private final String extension;
        private final String contentType;

        ImageFormat(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        private static ImageFormat from(byte[] bytes) {
            if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89
                    && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
                return PNG;
            }
            if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
                return JPG;
            }
            if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                    && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
