package kz.nu.pipeline.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${app.aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Upload a file to S3 bucket
     *
     * @param file MultipartFile to upload
     * @return The key (filename) of the uploaded file
     * @throws IOException if file processing fails
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String uniqueFilename = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".")))
                .map(fileExtension -> UUID.randomUUID() + fileExtension)
                .orElseGet(() -> UUID.randomUUID().toString());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFilename)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("File uploaded successfully to S3. Key: {}, ETag: {}", uniqueFilename, response.eTag());
            return uniqueFilename;
        } catch (S3Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from S3 bucket
     *
     * @param filename The key (filename) of the file to download
     * @return InputStream of the file content
     */
    public InputStream downloadFile(String filename) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
            log.info("File downloaded successfully from S3. Key: {}", filename);
            return response;
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: {}", filename);
            throw new RuntimeException("File not found: " + filename, e);
        } catch (S3Exception e) {
            log.error("Error downloading file from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a file exists in the S3 bucket
     *
     * @param filename The key (filename) to check
     * @return true if a file exists, false otherwise
     */
    public boolean fileExists(String filename) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence in S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check file existence in S3: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from the S3 bucket
     *
     * @param filename The key (filename) of the file to delete
     */
    public void deleteFile(String filename) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3. Key: {}", filename);

        } catch (S3Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
}