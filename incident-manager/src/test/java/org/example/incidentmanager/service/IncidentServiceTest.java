package org.example.incidentmanager.service;

import org.example.incidentmanager.model.Incident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
public class IncidentServiceTest {

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService();
    }

    @Test
    void testCreateIncident() {
        Incident incident = new Incident();
        incident.setDescription("Test incident");

        Incident createdIncident = incidentService.createIncident(incident);
        assertNotNull(createdIncident.getId());
        assertEquals("Test incident", createdIncident.getDescription());
    }

    @Test
    void testUpdateIncident() {
        Incident incident = new Incident();
        incident.setDescription("Test incident");
        Incident createdIncident = incidentService.createIncident(incident);

        createdIncident.setDescription("Updated description");
        Incident updatedIncident = incidentService.updateIncident(createdIncident);

        assertEquals("Updated description", updatedIncident.getDescription());
    }

    @Test
    void testUpdateNonExistentIncident() {
        Incident incident = new Incident();
        incident.setId(999L);
        incident.setDescription("Non-existent incident");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            incidentService.updateIncident(incident);
        });
        assertEquals("Incident with id 999 not found", exception.getMessage());
    }

    @Test
    void testDeleteIncident() {
        Incident incident = new Incident();
        incident.setDescription("Test incident");
        Incident createdIncident = incidentService.createIncident(incident);

        incidentService.deleteIncident(createdIncident.getId());
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            incidentService.deleteIncident(createdIncident.getId());
        });
        assertEquals("Incident with id " + createdIncident.getId() + " not found", exception.getMessage());
    }

    @Test
    void testDeleteNonExistentIncident() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            incidentService.deleteIncident(999L);
        });
        assertEquals("Incident with id 999 not found", exception.getMessage());
    }

    @Test
    void testGetAllIncidents() {
        Incident incident1 = new Incident();
        incident1.setDescription("Incident 1");
        incidentService.createIncident(incident1);

        Incident incident2 = new Incident();
        incident2.setDescription("Incident 2");
        incidentService.createIncident(incident2);

        List<Incident> incidents = incidentService.getAllIncidents();
        assertEquals(2, incidents.size());
        assertTrue(incidents.stream().anyMatch(i -> i.getDescription().equals("Incident 1")));
        assertTrue(incidents.stream().anyMatch(i -> i.getDescription().equals("Incident 2")));
    }

}
