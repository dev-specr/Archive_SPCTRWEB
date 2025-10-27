package com.spectre.ship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShipRepository extends JpaRepository<Ship, Long> {

    Optional<Ship> findFirstByUuid(String uuid);

    Optional<Ship> findFirstByNameIgnoreCase(String name);

    @Query("select distinct s.name from Ship s where s.name is not null and lower(s.name) <> 'unknown' order by s.name asc")
    List<String> listAllNames();

    @Query("select s from Ship s where lower(s.name) like lower(concat('%', :q, '%')) order by s.name asc")
    List<Ship> searchByNameLike(@Param("q") String q);
}
