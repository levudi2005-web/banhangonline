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
    private static final String IMAGE_FILE_PATTERN = "(?i)[0-9a-f-]{36}\\.(png|jpe?g|gif|webp|bmp|tiff?|avif|heic|heif|ico)";
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
            throw ApiException.validation("Định dạng ảnh không được hỗ trợ hoặc tệp không phải ảnh hợp lệ. "
                    + "Hỗ trợ PNG, JPG, GIF, WebP, BMP, TIFF, AVIF, HEIC và ICO; SVG chưa được hỗ trợ.");
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
            if (!fileName.matches(IMAGE_FILE_PATTERN)) {
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
        if (!fileName.matches(IMAGE_FILE_PATTERN)) {
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
        if (!fileName.matches("(?i)[0-9a-fA-F-]{36}\\.(png|jpe?g|webp)")) {
            return null;
        }
        return "stores/" + storeId + "/products/" + fileName.toLowerCase(Locale.ROOT);
    }

    private enum ImageFormat {
        PNG("png", "image/png"),
        JPG("jpg", "image/jpeg"),
        GIF("gif", "image/gif"),
        WEBP("webp", "image/webp"),
        BMP("bmp", "image/bmp"),
        TIFF("tiff", "image/tiff"),
        AVIF("avif", "image/avif"),
        HEIC("heic", "image/heic"),
        HEIF("heif", "image/heif"),
        ICO("ico", "image/x-icon");

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
            if (startsWith(bytes, "GIF87a") || startsWith(bytes, "GIF89a")) {
                return GIF;
            }
            if (startsWith(bytes, "RIFF") && bytes.length >= 12 && matchesAscii(bytes, 8, "WEBP")) {
                return WEBP;
            }
            if (startsWith(bytes, "BM")) {
                return BMP;
            }
            if (bytes.length >= 4 && ((bytes[0] == 'I' && bytes[1] == 'I'
                    && bytes[2] == 42 && bytes[3] == 0)
                    || (bytes[0] == 'M' && bytes[1] == 'M' && bytes[2] == 0 && bytes[3] == 42)
                    || (bytes[0] == 'I' && bytes[1] == 'I' && bytes[2] == 43 && bytes[3] == 0)
                    || (bytes[0] == 'M' && bytes[1] == 'M' && bytes[2] == 0 && bytes[3] == 43))) {
                return TIFF;
            }
            if (bytes.length >= 12 && matchesAscii(bytes, 4, "ftyp")) {
                if (matchesAscii(bytes, 8, "avif") || matchesAscii(bytes, 8, "avis")) {
                    return AVIF;
                }
                if (matchesAscii(bytes, 8, "heic") || matchesAscii(bytes, 8, "heix")
                        || matchesAscii(bytes, 8, "hevc") || matchesAscii(bytes, 8, "hevx")) {
                    return HEIC;
                }
                if (matchesAscii(bytes, 8, "mif1") || matchesAscii(bytes, 8, "msf1")) {
                    return HEIF;
                }
            }
            if (bytes.length >= 4 && bytes[0] == 0 && bytes[1] == 0
                    && bytes[2] == 1 && bytes[3] == 0) {
                return ICO;
            }
            return null;
        }

        private static boolean startsWith(byte[] bytes, String signature) {
            return matchesAscii(bytes, 0, signature);
        }

        private static boolean matchesAscii(byte[] bytes, int offset, String signature) {
            if (offset < 0 || bytes.length < offset + signature.length()) {
                return false;
            }
            for (int i = 0; i < signature.length(); i++) {
                if (bytes[offset + i] != (byte) signature.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }
}
