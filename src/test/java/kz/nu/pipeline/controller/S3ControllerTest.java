package kz.nu.pipeline.controller;

import kz.nu.pipeline.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ControllerTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private S3Controller s3Controller;

    @Test
    void uploadFile_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.txt", 
                "text/plain", 
                "Hello World".getBytes()
        );
        String expectedFilename = "unique-filename.txt";
        when(s3Service.uploadFile(any())).thenReturn(expectedFilename);

        // Act
        ResponseEntity<Map<String, Object>> response = s3Controller.uploadFile(file);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(expectedFilename, response.getBody().get("filename"));
        assertEquals("test.txt", response.getBody().get("originalFilename"));

        verify(s3Service, times(1)).uploadFile(file);
    }

    @Test
    void uploadFile_EmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.txt", 
                "text/plain", 
                new byte[0]
        );

        // Act
        ResponseEntity<Map<String, Object>> response = s3Controller.uploadFile(emptyFile);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("File cannot be empty", response.getBody().get("message"));

        verify(s3Service, never()).uploadFile(any());
    }

    @Test
    void downloadFile_Success() {
        // Arrange
        String filename = "test-file.txt";
        InputStream mockInputStream = new ByteArrayInputStream("file content".getBytes());
        when(s3Service.fileExists(filename)).thenReturn(true);
        when(s3Service.downloadFile(filename)).thenReturn(mockInputStream);

        // Act
        var response = s3Controller.downloadFile(filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(s3Service, times(1)).fileExists(filename);
        verify(s3Service, times(1)).downloadFile(filename);
    }

    @Test
    void downloadFile_NotFound() {
        // Arrange
        String filename = "non-existent-file.txt";
        when(s3Service.fileExists(filename)).thenReturn(false);

        // Act
        var response = s3Controller.downloadFile(filename);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(s3Service, times(1)).fileExists(filename);
        verify(s3Service, never()).downloadFile(anyString());
    }

    @Test
    void checkFileExists_FileExists() {
        // Arrange
        String filename = "existing-file.txt";
        when(s3Service.fileExists(filename)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = s3Controller.checkFileExists(filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(filename, response.getBody().get("filename"));
        assertTrue((Boolean) response.getBody().get("exists"));

        verify(s3Service, times(1)).fileExists(filename);
    }

    @Test
    void checkFileExists_FileDoesNotExist() {
        // Arrange
        String filename = "non-existent-file.txt";
        when(s3Service.fileExists(filename)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = s3Controller.checkFileExists(filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(filename, response.getBody().get("filename"));
        assertFalse((Boolean) response.getBody().get("exists"));

        verify(s3Service, times(1)).fileExists(filename);
    }

    @Test
    void deleteFile_Success() {
        // Arrange
        String filename = "file-to-delete.txt";
        when(s3Service.fileExists(filename)).thenReturn(true);
        doNothing().when(s3Service).deleteFile(filename);

        // Act
        ResponseEntity<Map<String, Object>> response = s3Controller.deleteFile(filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("File deleted successfully", response.getBody().get("message"));
        assertEquals(filename, response.getBody().get("filename"));

        verify(s3Service, times(1)).fileExists(filename);
        verify(s3Service, times(1)).deleteFile(filename);
    }

    @Test
    void deleteFile_NotFound() {
        // Arrange
        String filename = "non-existent-file.txt";
        when(s3Service.fileExists(filename)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = s3Controller.deleteFile(filename);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(s3Service, times(1)).fileExists(filename);
        verify(s3Service, never()).deleteFile(anyString());
    }
}
