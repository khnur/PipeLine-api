package kz.nu.pipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelUploadResponseDto {
    
    private boolean success;
    private String message;
    private int totalRecords;
    private int successfulRecords;
    private int failedRecords;
    private List<String> errors;
    private List<PipeDto> processedPipes;
    
    public static ExcelUploadResponseDto success(int total, int successful, List<PipeDto> pipes) {
        return new ExcelUploadResponseDto(
            true,
            "Excel file processed successfully",
            total,
            successful,
            total - successful,
            null,
            pipes
        );
    }
    
    public static ExcelUploadResponseDto failure(String message, List<String> errors) {
        return new ExcelUploadResponseDto(
            false,
            message,
            0,
            0,
            0,
            errors,
            null
        );
    }
} 