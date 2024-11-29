package org.example.incidentmanager.controller;

import org.example.incidentmanager.model.Incident;
import org.example.incidentmanager.service.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/incident")
@CrossOrigin(origins = "http://localhost:3000")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    // 创建事件接口，添加@Valid注解进行参数校验，并处理校验结果
    @PostMapping
    public ResponseEntity<?> createIncident(@Valid @RequestBody Incident incident, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errorMessages = new ArrayList<>();
            bindingResult.getFieldErrors().forEach(fieldError -> errorMessages.add(fieldError.getDefaultMessage()));
            return new ResponseEntity<>(errorMessages, HttpStatus.BAD_REQUEST);
        }
        Incident createdIncident = incidentService.createIncident(incident);
        return new ResponseEntity<>(createdIncident, HttpStatus.CREATED);
    }

    // 删除事件的API
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIncident(@PathVariable Long id) {

        if (id == null) {
            throw new IllegalArgumentException("Incident id cannot be null");
        }

        incidentService.deleteIncident(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 修改事件的API
    @PutMapping
    public ResponseEntity<?> updateIncident(@Valid @RequestBody Incident incident, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errorMessages = new ArrayList<>();
            bindingResult.getFieldErrors().forEach(fieldError -> errorMessages.add(fieldError.getDefaultMessage()));
            return new ResponseEntity<>(errorMessages, HttpStatus.BAD_REQUEST);
        }
        Incident updatedIncident = incidentService.updateIncident(incident);
        return new ResponseEntity<>(updatedIncident, HttpStatus.OK);
    }

    // 获取所有事件的API
    @GetMapping
    public ResponseEntity<List<Incident>> getIncidents() {
        List<Incident> incidents = incidentService.getAllIncidents();
        return new ResponseEntity<>(incidents, HttpStatus.OK);
    }
}