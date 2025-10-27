package com.spectre.commodity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommodityPriceRepository extends JpaRepository<CommodityPrice, Long> {

    Optional<CommodityPrice> findByCommodityIgnoreCaseAndLocationIgnoreCase(String commodity, String location);

    List<CommodityPrice> findAllByCommodityIgnoreCase(String commodity);

    List<CommodityPrice> findAllByUexCommodityId(Long uexCommodityId);

    Optional<CommodityPrice> findByUexCommodityIdAndUexLocationId(Long uexCommodityId, Long uexLocationId);

    @Query("select distinct c.commodity from CommodityPrice c order by c.commodity asc")
    List<String> listCommodities();
}
