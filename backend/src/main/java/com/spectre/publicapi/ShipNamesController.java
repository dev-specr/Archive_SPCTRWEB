package com.spectre.publicapi;

import com.spectre.ship.ShipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/ships")
@RequiredArgsConstructor
public class ShipNamesController {

    private final ShipService shipService;

    @GetMapping("/names")
    public List<String> names() {
        return shipService.names();
    }
}

