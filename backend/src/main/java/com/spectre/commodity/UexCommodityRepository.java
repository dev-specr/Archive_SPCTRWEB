package com.spectre.commodity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UexCommodityRepository extends JpaRepository<UexCommodity, Long> {
    List<UexCommodity> findAllByNameContainingIgnoreCaseOrderByNameAsc(String q);
    List<UexCommodity> findAllByOrderByNameAsc();
}

