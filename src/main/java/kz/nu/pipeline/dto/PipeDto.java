package kz.nu.pipeline.dto;

import kz.nu.pipeline.model.Pipe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipeDto {
    
    private Long id;
    private String pipeNumber;
    private BigDecimal diameter;
    private BigDecimal length;
    private BigDecimal wallThickness;
    private String material;
    private String grade;
    private String manufacturer;
    private LocalDate productionDate;
    private BigDecimal weight;
    private String location;
    private Pipe.PipeStatus status;
    private String remarks;
    private String batchNumber;
    private String qualityClass;
    private String coatingType;
    private BigDecimal pressureRating;
    private LocalDate createdDate;
    private LocalDate updatedDate;
} 