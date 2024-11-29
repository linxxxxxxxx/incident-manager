package org.example.incidentmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.incidentmanager.model.Incident;
import org.example.incidentmanager.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.type.TypeReference;

@WebMvcTest(IncidentController.class)
public class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentService incidentService;

    @Autowired
    private ObjectMapper objectMapper;

    // 测试创建正常事件API（正常情况）
    @Test
    public void createIncident_ValidIncident_ShouldReturnCreatedIncident() throws Exception {
        Date dateTime = new Date();
        Incident incidentToCreate = Incident.builder()
                .name("Test Incident")
                .description("This is a test Incident")
                .createdDate(dateTime)
                .updatedDate(dateTime)
                .build();

        Incident savedIncident = Incident.builder()
                .id(1L)
                .name("Test Incident")
                .description("This is a test Incident")
                .createdDate(dateTime)
                .updatedDate(dateTime)
                .build();

        Mockito.when(incidentService.createIncident(incidentToCreate)).thenReturn(savedIncident);

        mockMvc.perform(MockMvcRequestBuilders.post("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentToCreate)))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Test Incident"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("This is a test Incident"))
                .andDo(print());
    }

    // 测试创建不正常事件API（不正常情况）
    @Test
    public void createIncident_InvalidIncident_ShouldReturnBadRequest() throws Exception {
        // 名称为空的场景
        Incident incidentToCreateWithoutName = Incident.builder()
                .description("This is a test Incident")
                .build();

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentToCreateWithoutName)))
                .andExpect(status().isBadRequest())
                .andDo(print()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<String> responseMessages = objectMapper.readValue(responseBody, new TypeReference<>() {
        });
        assertThat(responseMessages).contains("Name is required");

        // 描述为空的场景
        Incident incidentToCreateWithoutDescription = Incident.builder()
                .name("Test Incident")
                .build();

        mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentToCreateWithoutDescription)))
                .andExpect(status().isBadRequest())
                .andDo(print()).andReturn();

        responseBody = mvcResult.getResponse().getContentAsString();
        responseMessages = objectMapper.readValue(responseBody, new TypeReference<>() {
        });
        assertThat(responseMessages).contains("Description is required");
    }

    // 测试创建事件API（模拟服务层抛出异常情况）
    @Test
    public void createIncident_ValidIncident_WithException() throws Exception {
        Incident incidentToCreate = Incident.builder()
                .name("Test Incident")
                .description("This is a test Incident")
                .build();
        Mockito.when(incidentService.createIncident(incidentToCreate)).thenThrow(new RuntimeException("模拟创建事件出错"));

        mockMvc.perform(MockMvcRequestBuilders.post("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentToCreate)))
                .andExpect(status().isInternalServerError())
                .andDo(print());
    }

    // 测试获取所有事件API（正常情况）
    @Test
    public void getIncidents_ShouldReturnIncidents() throws Exception {
        List<Incident> incidents = Arrays.asList(
                Incident.builder()
                        .name("Test Incident 1")
                        .description("This is a test Incident 1")
                        .build(),
                Incident.builder()
                        .name("Test Incident 2")
                        .description("This is a test Incident 2")
                        .build()
        );
        Mockito.when(incidentService.getAllIncidents()).thenReturn(incidents);

        mockMvc.perform(MockMvcRequestBuilders.get("/incident"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Test Incident 1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].description").value("This is a test Incident 1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Test Incident 2"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].description").value("This is a test Incident 2"))
                .andDo(print());
    }

    // 测试修改事件API（正常情况）
    @Test
    public void updateIncident_ValidIncident_ShouldReturnIncident() throws Exception {
        Incident updatedIncident = Incident.builder()
                .id(1L)
                .name("Updated Incident")
                .description("Updated Description")
                .build();
        Mockito.when(incidentService.updateIncident(updatedIncident)).thenReturn(updatedIncident);

        mockMvc.perform(MockMvcRequestBuilders.put("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedIncident)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Updated Incident"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Updated Description"))
                .andDo(print());
    }

    // 测试修改不正常事件API（不正常情况）
    @Test
    public void updateIncident_InvalidIncident_ShouldReturnBadRequest() throws Exception {
        // 名称为空的场景
        Incident incidentToCreateWithoutName = Incident.builder()
                .description("Updated Description")
                .build();

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.put("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentToCreateWithoutName)))
                .andExpect(status().isBadRequest())
                .andDo(print()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<String> responseMessages = objectMapper.readValue(responseBody, new TypeReference<>() {
        });
        assertThat(responseMessages).contains("Name is required");

        // 描述为空的场景
        Incident incidentToCreateWithoutDescription = Incident.builder()
                .name("Test Incident")
                .build();

        mvcResult = mockMvc.perform(MockMvcRequestBuilders.put("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentToCreateWithoutDescription)))
                .andExpect(status().isBadRequest())
                .andDo(print()).andReturn();

        responseBody = mvcResult.getResponse().getContentAsString();
        responseMessages = objectMapper.readValue(responseBody, new TypeReference<>() {
        });
        assertThat(responseMessages).contains("Description is required");
    }

    // 测试修改事件API（找不到要修改的事件情况）
    @Test
    public void updateIncident_NotFound() throws Exception {
        Incident updatedIncident = Incident.builder()
                .id(1L)
                .name("Updated Incident")
                .description("Updated Description")
                .build();
        Mockito.when(incidentService.updateIncident(updatedIncident)).thenThrow(new IllegalArgumentException("模拟修改事件出错，事件不存在"));

        mockMvc.perform(MockMvcRequestBuilders.put("/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedIncident)))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    // 测试删除事件API（正常情况）
    @Test
    public void deleteIncident() throws Exception {
        Long IncidentId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/incident/" + IncidentId))
                .andExpect(status().isOk())
                .andDo(print());

        Mockito.verify(incidentService, Mockito.times(1)).deleteIncident(IncidentId);
    }

    // 测试删除事件API（要删除的事件不存在情况）
    @Test
    public void deleteIncident_NotFound() throws Exception {
        Long IncidentId = 1L;
        Mockito.doThrow(new IllegalArgumentException("模拟删除事件出错，事件不存在")).when(incidentService).deleteIncident(IncidentId);

        mockMvc.perform(MockMvcRequestBuilders.delete("/incident/" + IncidentId))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
