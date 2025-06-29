package kz.nu.pipeline.exception;

import kz.nu.pipeline.dto.ErrorDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/test-path");
    }

    @Test
    void handleRuntimeException_NotFound_ShouldReturnNotFoundStatus() {
        // Given
        RuntimeException exception = new RuntimeException("Pipe not found with id: 123");

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleRuntimeException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Pipe not found with id: 123", response.getBody().getMessage());
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleRuntimeException_Generic_ShouldReturnInternalServerError() {
        // Given
        RuntimeException exception = new RuntimeException("Some generic error");

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleRuntimeException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("An unexpected error occurred"));
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleIOException_ShouldReturnBadRequest() {
        // Given
        IOException exception = new IOException("File processing error");

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleIOException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("File Processing Error", response.getBody().getError());
        assertEquals("Error processing file: File processing error", response.getBody().getMessage());
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleHttpMessageNotReadableException_ShouldReturnBadRequest() {
        // Given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Invalid JSON");

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleHttpMessageNotReadableException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid request body format", response.getBody().getMessage());
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_ShouldReturnMethodNotAllowed() {
        // Given
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("POST");

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleHttpRequestMethodNotSupportedException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().getStatus());
        assertEquals("Method Not Allowed", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("POST"));
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleMaxUploadSizeExceededException_ShouldReturnPayloadTooLarge() {
        // Given
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(1000000);

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleMaxUploadSizeExceededException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(413, response.getBody().getStatus());
        assertEquals("File Too Large", response.getBody().getError());
        assertEquals("File size exceeds the maximum allowed limit", response.getBody().getMessage());
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleIllegalArgumentException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid argument: Invalid parameter value", response.getBody().getMessage());
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new Exception("Unexpected error");

        // When
        ResponseEntity<ErrorDto> response = globalExceptionHandler.handleGenericException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
        assertEquals("/test-path", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }
}