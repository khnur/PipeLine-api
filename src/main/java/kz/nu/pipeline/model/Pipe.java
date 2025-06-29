package kz.nu.pipeline.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "pipe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pipe {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pipe_number")
    private String pipeNumber;
    
    @Column(name = "diameter")
    private BigDecimal diameter;
    
    @Column(name = "length")
    private BigDecimal length;
    
    @Column(name = "wall_thickness")
    private BigDecimal wallThickness;
    
    @Column(name = "material")
    private String material;
    
    @Column(name = "grade")
    private String grade;
    
    @Column(name = "manufacturer")
    private String manufacturer;
    
    @Column(name = "production_date")
    private LocalDate productionDate;
    
    @Column(name = "weight")
    private BigDecimal weight;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PipeStatus status;
    
    @Column(name = "remarks")
    private String remarks;
    
    @Column(name = "batch_number")
    private String batchNumber;
    
    @Column(name = "quality_class")
    private String qualityClass;
    
    @Column(name = "coating_type")
    private String coatingType;
    
    @Column(name = "pressure_rating")
    private BigDecimal pressureRating;
    
    @Column(name = "created_date")
    private LocalDate createdDate;
    
    @Column(name = "updated_date")
    private LocalDate updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDate.now();
        updatedDate = LocalDate.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDate.now();
    }
    
    public enum PipeStatus {
        NEW,
        IN_STOCK,
        IN_USE,
        DAMAGED,
        SCRAPPED,
        UNDER_INSPECTION
    }
} 