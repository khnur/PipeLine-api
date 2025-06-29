package kz.nu.pipeline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.nu.pipeline.dto.ExcelUploadResponseDto;
import kz.nu.pipeline.dto.PipeDto;
import kz.nu.pipeline.model.Pipe;
import kz.nu.pipeline.service.PipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/pipe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pipe Management", description = "API for managing pipe inventory")
public class PipeController {

    private final PipeService pipeService;

    @PostMapping("/upload-excel")
    @Operation(summary = "Upload Excel file with pipe data",
            description = "Process Excel file containing pipe inventory data and save to database")
    public ResponseEntity<ExcelUploadResponseDto> uploadExcelFile(
            @Parameter(description = "Excel file containing pipe data")
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Received Excel file upload request: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ExcelUploadResponseDto.failure("File is empty", null));
        }

        if (!this.isValidExcelFile(file)) {
            return ResponseEntity.badRequest()
                    .body(ExcelUploadResponseDto.failure("Invalid file format. Please upload Excel file (.xlsx or .xls)", null));
        }

        try {
            ExcelUploadResponseDto response = pipeService.processExcelFile(file);

            if (response.isSuccess()) {
                log.info("Excel file processed successfully: {} total, {} successful, {} failed",
                        response.getTotalRecords(), response.getSuccessfulRecords(), response.getFailedRecords());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Excel file processing completed with errors: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            }

        } catch (Exception e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExcelUploadResponseDto.failure("Internal server error: " + e.getMessage(), null));
        }
    }

    @PostMapping
    @Operation(summary = "Create a new pipe", description = "Add a new pipe to the inventory")
    public ResponseEntity<PipeDto> createPipe(@RequestBody PipeDto pipeDto) {
        log.info("Creating new pipe: {}", pipeDto.getPipeNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(pipeService.createPipe(pipeDto));
    }

    @GetMapping
    @Operation(summary = "Get all pipes", description = "Retrieve all pipes from the inventory")
    public ResponseEntity<List<PipeDto>> getAllPipes() {
        log.info("Retrieving all pipes");
        return ResponseEntity.ok(pipeService.getAllPipes());

    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pipe by ID", description = "Retrieve a specific pipe by its ID")
    public ResponseEntity<PipeDto> getPipeById(@PathVariable Long id) {
        log.info("Retrieving pipe with ID: {}", id);
        Optional<PipeDto> pipe = pipeService.getPipeById(id);
        return pipe.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

    }

    @GetMapping("/number/{pipeNumber}")
    @Operation(summary = "Get pipe by number", description = "Retrieve a specific pipe by its pipe number")
    public ResponseEntity<PipeDto> getPipeByNumber(@PathVariable String pipeNumber) {
        log.info("Retrieving pipe with number: {}", pipeNumber);
        Optional<PipeDto> pipe = pipeService.getPipeByNumber(pipeNumber);
        return pipe.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pipe", description = "Update an existing pipe")
    public ResponseEntity<PipeDto> updatePipe(@PathVariable Long id, @RequestBody PipeDto pipeDto) {
        log.info("Updating pipe with ID: {}", id);
        return ResponseEntity.ok(pipeService.updatePipe(id, pipeDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pipe", description = "Remove a pipe from the inventory")
    public ResponseEntity<Void> deletePipe(@PathVariable Long id) {
        log.info("Deleting pipe with ID: {}", id);
        pipeService.deletePipe(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get pipes by status", description = "Retrieve pipes filtered by status")
    public ResponseEntity<List<PipeDto>> getPipesByStatus(@PathVariable Pipe.PipeStatus status) {
        log.info("Retrieving pipes with status: {}", status);
        return ResponseEntity.ok(pipeService.getPipesByStatus(status));
    }

    @GetMapping("/material/{material}")
    @Operation(summary = "Get pipes by material", description = "Retrieve pipes filtered by material")
    public ResponseEntity<List<PipeDto>> getPipesByMaterial(@PathVariable String material) {
        log.info("Retrieving pipes with material: {}", material);
        return ResponseEntity.ok(pipeService.getPipesByMaterial(material));
    }

    @GetMapping("/location/{location}")
    @Operation(summary = "Get pipes by location", description = "Retrieve pipes filtered by location")
    public ResponseEntity<List<PipeDto>> getPipesByLocation(@PathVariable String location) {
        log.info("Retrieving pipes at location: {}", location);
        return ResponseEntity.ok(pipeService.getPipesByLocation(location));
    }

    @GetMapping("/manufacturer/{manufacturer}")
    @Operation(summary = "Get pipes by manufacturer", description = "Retrieve pipes filtered by manufacturer")
    public ResponseEntity<List<PipeDto>> getPipesByManufacturer(@PathVariable String manufacturer) {
        log.info("Retrieving pipes from manufacturer: {}", manufacturer);
        return ResponseEntity.ok(pipeService.getPipesByManufacturer(manufacturer));
    }

    @GetMapping("/diameter-range")
    @Operation(summary = "Get pipes by diameter range", description = "Retrieve pipes within specified diameter range")
    public ResponseEntity<List<PipeDto>> getPipesByDiameterRange(
            @RequestParam Double minDiameter,
            @RequestParam Double maxDiameter
    ) {
        log.info("Retrieving pipes with diameter range: {} - {}", minDiameter, maxDiameter);
        return ResponseEntity.ok(pipeService.getPipesByDiameterRange(minDiameter, maxDiameter));
    }

    @GetMapping("/batch/{batchNumber}")
    @Operation(summary = "Get pipes by batch number", description = "Retrieve pipes from specific batch")
    public ResponseEntity<List<PipeDto>> getPipesByBatch(@PathVariable String batchNumber) {
        log.info("Retrieving pipes from batch: {}", batchNumber);
        return ResponseEntity.ok(pipeService.getPipesByBatch(batchNumber));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Count pipes by status", description = "Get count of pipes by status")
    public ResponseEntity<Long> countPipesByStatus(@PathVariable Pipe.PipeStatus status) {
        log.info("Counting pipes with status: {}", status);
        return ResponseEntity.ok(pipeService.countPipesByStatus(status));
    }

    @GetMapping("/exists/{pipeNumber}")
    @Operation(summary = "Check if pipe number exists", description = "Check if a pipe number already exists in the system")
    public ResponseEntity<Boolean> checkPipeNumberExists(@PathVariable String pipeNumber) {
        log.info("Checking if pipe number exists: {}", pipeNumber);
        return ResponseEntity.ok(pipeService.pipeNumberExists(pipeNumber));
    }

    private boolean isValidExcelFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                        contentType.equals("application/vnd.ms-excel") ||
                        file.getOriginalFilename() != null && (
                                file.getOriginalFilename().endsWith(".xlsx") ||
                                        file.getOriginalFilename().endsWith(".xls")
                        )
        );
    }
} 