package com.pragun.ElectiSelect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/electives")
public class ElectivesController {

    // Temporary stub to prevent 404 Not Found errors on the frontend
    // until the full Department Electives backend logic is implemented.
    @GetMapping("/dept")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Object>> getDeptElectives() {
        return ResponseEntity.ok(new ArrayList<>());
    }
}
