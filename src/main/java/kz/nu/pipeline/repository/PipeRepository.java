package kz.nu.pipeline.repository;

import kz.nu.pipeline.model.Pipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PipeRepository extends JpaRepository<Pipe, Long> {
    
    Optional<Pipe> findByPipeNumber(String pipeNumber);
    
    List<Pipe> findByStatus(Pipe.PipeStatus status);
    
    List<Pipe> findByMaterial(String material);
    
    List<Pipe> findByLocation(String location);
    
    List<Pipe> findByManufacturer(String manufacturer);
    
    @Query("SELECT p FROM Pipe p WHERE p.diameter BETWEEN :minDiameter AND :maxDiameter")
    List<Pipe> findByDiameterRange(@Param("minDiameter") Double minDiameter, @Param("maxDiameter") Double maxDiameter);
    
    @Query("SELECT p FROM Pipe p WHERE p.batchNumber = :batchNumber")
    List<Pipe> findByBatchNumber(@Param("batchNumber") String batchNumber);
    
    @Query("SELECT COUNT(p) FROM Pipe p WHERE p.status = :status")
    Long countByStatus(@Param("status") Pipe.PipeStatus status);
    
    boolean existsByPipeNumber(String pipeNumber);
} 