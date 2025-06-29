package kz.nu.pipeline.service;

import kz.nu.pipeline.dto.ExcelUploadResponseDto;
import kz.nu.pipeline.dto.PipeDto;
import kz.nu.pipeline.model.Pipe;
import kz.nu.pipeline.repository.PipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipeService {

    private final PipeRepository pipeRepository;

    @Transactional
    public PipeDto createPipe(PipeDto pipeDto) {
        Optional.ofNullable(pipeDto.getPipeNumber())
                .filter(StringUtils::isNotBlank)
                .filter(this::pipeNumberExists)
                .ifPresent(pipeNumber -> {
                    log.warn("Pipe number already exists: {}", pipeNumber);
                    throw new IllegalArgumentException("Pipe number already exists: " + pipeNumber);
                });
        pipeDto.setId(null);
        Pipe pipe = this.convertToEntity(pipeDto);
        Pipe savedPipe = pipeRepository.save(pipe);
        return this.convertToDto(savedPipe);
    }

    @Transactional
    public PipeDto updatePipe(Long id, PipeDto pipeDto) {
        Pipe existingPipe = pipeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pipe not found with id: " + id));

        this.updatePipeFields(existingPipe, pipeDto);
        Pipe updatedPipe = pipeRepository.save(existingPipe);
        return this.convertToDto(updatedPipe);
    }

    public Optional<PipeDto> getPipeById(Long id) {
        return pipeRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<PipeDto> getPipeByNumber(String pipeNumber) {
        return pipeRepository.findByPipeNumber(pipeNumber)
                .map(this::convertToDto);
    }

    public List<PipeDto> getAllPipes() {
        return pipeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePipe(Long id) {
        pipeRepository.deleteById(id);
    }

    @Transactional
    public ExcelUploadResponseDto processExcelFile(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<PipeDto> processedPipes = new ArrayList<>();
        int totalRecords = 0;
        int successfulRecords = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                totalRecords++;

                try {
                    PipeDto pipeDto = parseRowToPipeDto(row);
                    
                    // Check if pipe number already exists
                    if (pipeDto.getPipeNumber() != null && 
                        pipeRepository.existsByPipeNumber(pipeDto.getPipeNumber())) {
                        errors.add("Row " + (row.getRowNum() + 1) + ": Pipe number already exists: " + pipeDto.getPipeNumber());
                        continue;
                    }

                    PipeDto savedPipe = createPipe(pipeDto);
                    processedPipes.add(savedPipe);
                    successfulRecords++;

                } catch (Exception e) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    log.error("Error processing row {}: {}", row.getRowNum() + 1, e.getMessage());
                }
            }

        } catch (IOException e) {
            log.error("Error processing Excel file: {}", e.getMessage());
            return ExcelUploadResponseDto.failure("Error reading Excel file: " + e.getMessage(), 
                    Collections.singletonList(e.getMessage()));
        }

        if (errors.isEmpty()) {
            return ExcelUploadResponseDto.success(totalRecords, successfulRecords, processedPipes);
        } else {
            ExcelUploadResponseDto response = ExcelUploadResponseDto.success(totalRecords, successfulRecords, processedPipes);
            response.setErrors(errors);
            response.setFailedRecords(totalRecords - successfulRecords);
            return response;
        }
    }

    private PipeDto parseRowToPipeDto(Row row) {
        PipeDto pipeDto = new PipeDto();

        try {
            // Assuming column order based on typical pipe inventory sheets
            pipeDto.setPipeNumber(getCellValueAsString(row.getCell(0)));
            pipeDto.setDiameter(getCellValueAsBigDecimal(row.getCell(1)));
            pipeDto.setLength(getCellValueAsBigDecimal(row.getCell(2)));
            pipeDto.setWallThickness(getCellValueAsBigDecimal(row.getCell(3)));
            pipeDto.setMaterial(getCellValueAsString(row.getCell(4)));
            pipeDto.setGrade(getCellValueAsString(row.getCell(5)));
            pipeDto.setManufacturer(getCellValueAsString(row.getCell(6)));
            pipeDto.setProductionDate(getCellValueAsLocalDate(row.getCell(7)));
            pipeDto.setWeight(getCellValueAsBigDecimal(row.getCell(8)));
            pipeDto.setLocation(getCellValueAsString(row.getCell(9)));
            pipeDto.setStatus(parseStatus(getCellValueAsString(row.getCell(10))));
            pipeDto.setRemarks(getCellValueAsString(row.getCell(11)));
            pipeDto.setBatchNumber(getCellValueAsString(row.getCell(12)));
            pipeDto.setQualityClass(getCellValueAsString(row.getCell(13)));
            pipeDto.setCoatingType(getCellValueAsString(row.getCell(14)));
            pipeDto.setPressureRating(getCellValueAsBigDecimal(row.getCell(15)));

        } catch (Exception e) {
            throw new RuntimeException("Error parsing row data: " + e.getMessage());
        }

        return pipeDto;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> {
                    String stringValue = cell.getStringCellValue().trim();
                    yield stringValue.isEmpty() ? null : new BigDecimal(stringValue);
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid numeric value: " + cell);
        }
    }

    private LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell == null) return null;
        
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateString = cell.getStringCellValue().trim();
                if (dateString.isEmpty()) return null;
                // Add more date parsing logic as needed
                return LocalDate.parse(dateString);
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid date value: " + cell.toString());
        }
        
        return null;
    }

    private Pipe.PipeStatus parseStatus(String statusString) {
        if (statusString == null || statusString.trim().isEmpty()) {
            return Pipe.PipeStatus.NEW;
        }
        
        try {
            return Pipe.PipeStatus.valueOf(statusString.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try to map common status values
            String normalized = statusString.toLowerCase().trim();
            return switch (normalized) {
                case "новый", "new" -> Pipe.PipeStatus.NEW;
                case "на складе", "in stock" -> Pipe.PipeStatus.IN_STOCK;
                case "в использовании", "in use" -> Pipe.PipeStatus.IN_USE;
                case "поврежден", "damaged" -> Pipe.PipeStatus.DAMAGED;
                case "списан", "scrapped" -> Pipe.PipeStatus.SCRAPPED;
                default -> Pipe.PipeStatus.NEW;
            };
        }
    }

    public List<PipeDto> getPipesByStatus(Pipe.PipeStatus status) {
        return pipeRepository.findByStatus(status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PipeDto> getPipesByMaterial(String material) {
        return pipeRepository.findByMaterial(material).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PipeDto> getPipesByLocation(String location) {
        return pipeRepository.findByLocation(location).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PipeDto> getPipesByManufacturer(String manufacturer) {
        return pipeRepository.findByManufacturer(manufacturer).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PipeDto> getPipesByDiameterRange(Double minDiameter, Double maxDiameter) {
        return pipeRepository.findByDiameterRange(minDiameter, maxDiameter).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<PipeDto> getPipesByBatch(String batchNumber) {
        return pipeRepository.findByBatchNumber(batchNumber).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Long countPipesByStatus(Pipe.PipeStatus status) {
        return pipeRepository.countByStatus(status);
    }

    public boolean pipeNumberExists(String pipeNumber) {
        return pipeRepository.existsByPipeNumber(pipeNumber);
    }

    private PipeDto convertToDto(Pipe pipe) {
        return new PipeDto(
                pipe.getId(),
                pipe.getPipeNumber(),
                pipe.getDiameter(),
                pipe.getLength(),
                pipe.getWallThickness(),
                pipe.getMaterial(),
                pipe.getGrade(),
                pipe.getManufacturer(),
                pipe.getProductionDate(),
                pipe.getWeight(),
                pipe.getLocation(),
                pipe.getStatus(),
                pipe.getRemarks(),
                pipe.getBatchNumber(),
                pipe.getQualityClass(),
                pipe.getCoatingType(),
                pipe.getPressureRating(),
                pipe.getCreatedDate(),
                pipe.getUpdatedDate()
        );
    }

    private Pipe convertToEntity(PipeDto pipeDto) {
        Pipe pipe = new Pipe();
        pipe.setId(pipeDto.getId());
        pipe.setPipeNumber(pipeDto.getPipeNumber());
        pipe.setDiameter(pipeDto.getDiameter());
        pipe.setLength(pipeDto.getLength());
        pipe.setWallThickness(pipeDto.getWallThickness());
        pipe.setMaterial(pipeDto.getMaterial());
        pipe.setGrade(pipeDto.getGrade());
        pipe.setManufacturer(pipeDto.getManufacturer());
        pipe.setProductionDate(pipeDto.getProductionDate());
        pipe.setWeight(pipeDto.getWeight());
        pipe.setLocation(pipeDto.getLocation());
        pipe.setStatus(pipeDto.getStatus() != null ? pipeDto.getStatus() : Pipe.PipeStatus.NEW);
        pipe.setRemarks(pipeDto.getRemarks());
        pipe.setBatchNumber(pipeDto.getBatchNumber());
        pipe.setQualityClass(pipeDto.getQualityClass());
        pipe.setCoatingType(pipeDto.getCoatingType());
        pipe.setPressureRating(pipeDto.getPressureRating());
        return pipe;
    }

    private void updatePipeFields(Pipe existingPipe, PipeDto pipeDto) {
        if (pipeDto.getPipeNumber() != null) existingPipe.setPipeNumber(pipeDto.getPipeNumber());
        if (pipeDto.getDiameter() != null) existingPipe.setDiameter(pipeDto.getDiameter());
        if (pipeDto.getLength() != null) existingPipe.setLength(pipeDto.getLength());
        if (pipeDto.getWallThickness() != null) existingPipe.setWallThickness(pipeDto.getWallThickness());
        if (pipeDto.getMaterial() != null) existingPipe.setMaterial(pipeDto.getMaterial());
        if (pipeDto.getGrade() != null) existingPipe.setGrade(pipeDto.getGrade());
        if (pipeDto.getManufacturer() != null) existingPipe.setManufacturer(pipeDto.getManufacturer());
        if (pipeDto.getProductionDate() != null) existingPipe.setProductionDate(pipeDto.getProductionDate());
        if (pipeDto.getWeight() != null) existingPipe.setWeight(pipeDto.getWeight());
        if (pipeDto.getLocation() != null) existingPipe.setLocation(pipeDto.getLocation());
        if (pipeDto.getStatus() != null) existingPipe.setStatus(pipeDto.getStatus());
        if (pipeDto.getRemarks() != null) existingPipe.setRemarks(pipeDto.getRemarks());
        if (pipeDto.getBatchNumber() != null) existingPipe.setBatchNumber(pipeDto.getBatchNumber());
        if (pipeDto.getQualityClass() != null) existingPipe.setQualityClass(pipeDto.getQualityClass());
        if (pipeDto.getCoatingType() != null) existingPipe.setCoatingType(pipeDto.getCoatingType());
        if (pipeDto.getPressureRating() != null) existingPipe.setPressureRating(pipeDto.getPressureRating());
    }
} 