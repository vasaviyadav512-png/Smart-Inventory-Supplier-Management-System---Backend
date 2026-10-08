package com.smartinventory.repository;

import com.smartinventory.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findAllByOrderBySaleDateDescIdDesc();
    List<Sale> findBySaleDateGreaterThanEqual(LocalDate from);
    List<Sale> findByProductIdAndSaleDateGreaterThanEqual(Long productId, LocalDate from);
}
