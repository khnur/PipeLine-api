package kz.nu.pipeline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.nu.pipeline.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "APIs for uploading and downloading files from AWS S3")
public class S3Controller {

    private final S3Service s3Service;

    @Operation(summary = "Upload a file to S3", description = "Upload a file to AWS S3 bucket using multipart/form-data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid file or empty file"),
            @ApiResponse(responseCode = "500", description = "Internal server error - S3 upload failed")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        try {
            log.info("Received file upload request. Filename: {}, Size: {} bytes",
                    file.getOriginalFilename(), file.getSize());

            if (file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "File cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String filename = s3Service.uploadFile(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("filename", filename);
            response.put("originalFilename", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("contentType", file.getContentType());

            log.info("File uploaded successfully. S3 Key: {}", filename);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid file upload request: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IOException e) {
            log.error("IO error during file upload: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to process file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Download a file from S3", description = "Download a file from AWS S3 bucket by filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error - S3 download failed")
    })
    @GetMapping("/download/{filename}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @Parameter(description = "Name of the file to download", required = true)
            @PathVariable String filename
    ) {
        try {
            log.info("Received file download request. Filename: {}", filename);

            if (!s3Service.fileExists(filename)) {
                log.warn("File not found: {}", filename);
                return ResponseEntity.notFound().build();
            }

            InputStream fileStream = s3Service.downloadFile(filename);
            InputStreamResource resource = new InputStreamResource(fileStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            log.info("File download initiated successfully. Filename: {}", filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Check if file exists", description = "Check if a file exists in AWS S3 bucket")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File existence check completed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/exists/{filename}")
    public ResponseEntity<Map<String, Object>> checkFileExists(
            @Parameter(description = "Name of the file to check", required = true)
            @PathVariable String filename
    ) {
        try {
            log.info("Checking file existence. Filename: {}", filename);

            boolean exists = s3Service.fileExists(filename);

            Map<String, Object> response = new HashMap<>();
            response.put("filename", filename);
            response.put("exists", exists);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking file existence: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to check file existence: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Delete a file from S3", description = "Delete a file from AWS S3 bucket")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{filename}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @Parameter(description = "Name of the file to delete", required = true)
            @PathVariable String filename
    ) {
        try {
            log.info("Received file deletion request. Filename: {}", filename);

            if (!s3Service.fileExists(filename)) {
                log.warn("File not found for deletion: {}", filename);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "File not found: " + filename);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            s3Service.deleteFile(filename);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");
            response.put("filename", filename);

            log.info("File deleted successfully. Filename: {}", filename);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}