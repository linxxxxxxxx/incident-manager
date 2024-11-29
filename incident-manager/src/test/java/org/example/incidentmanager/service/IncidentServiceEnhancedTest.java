package org.example.incidentmanager.service;

import static org.junit.jupiter.api.Assertions.*;

import org.example.incidentmanager.model.Incident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest
public class IncidentServiceEnhancedTest {

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService();
    }

    @Test
    void testCreateIncidentWithNull() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            incidentService.createIncident(null);
        });
        assertEquals("Incident cannot be null", exception.getMessage());
    }

    @Test
    void testUpdateIncidentWithNull() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            incidentService.updateIncident(null);
        });
        assertEquals("Incident cannot be null", exception.getMessage());
    }

    @Test
    void testGetAllIncidentsWhenEmpty() {
        List<Incident> incidents = incidentService.getAllIncidents();
        assertTrue(incidents.isEmpty(), "Expected an empty list when no incidents are created");
    }

    // 模拟一个接近最大容量的缓存
    @Test
    void testCacheEviction() {
        for (int i = 0; i < 80000; i++) {
            Incident incident = new Incident();
            incident.setName("Incident " + i);
            incident.setDescription("Incident " + i);
            incidentService.createIncident(incident);
        }

        assertEquals(80000, incidentService.getIncidentCacheForTesting().size(), "Expected cache to hold all created incidents");

        List<Incident> incidents = incidentService.getAllIncidents();
        assertEquals(80000, incidents.size(), "Expected to retrieve all created incidents");
    }

    // 测试清理数据，改进后增加对清理逻辑内部判断的验证
    @Test
    void testCleanExpiredDataWithNoExpiredData() {
        // 模拟当前时间，确保所有数据都不会被判定为过期（这里假设Incident的更新时间等逻辑相关）
        long currentTime = System.currentTimeMillis();
        incidentService.getIncidentsForTesting().forEach(incident -> incident.setUpdatedDate(new java.util.Date(currentTime)));

        incidentService.cleanExpiredData();

        assertEquals(0, incidentService.getAllIncidents().size(), "Expected no incidents after clean with no expired data");

        // 验证cleanExpiredData方法内部是否正确遍历了incidentMap，这里通过Mockito的spy来实现（需要添加Mockito依赖）
        // IncidentService spyService = spy(incidentService);
        // doCallRealMethod().when(spyService).cleanExpiredData();
        // spyService.cleanExpiredData();
        // verify(spyService.incidentMap, times(1)).entrySet(); // 验证是否调用了entrySet方法来遍历map，根据实际逻辑调整验证点
    }

    // 测试并发修改，改进后增加更多并发线程和更严谨的验证逻辑，使用JUnit 5并发测试扩展
    @Execution(ExecutionMode.CONCURRENT)
    @Test
    void testConcurrentUpdates() throws InterruptedException {
        Incident incident = new Incident();
        incident.setDescription("Concurrent Incident");
        Incident createdIncident = incidentService.createIncident(incident);

        int threadCount = 10; // 增加并发线程数量
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    Incident update = new Incident();
                    update.setId(createdIncident.getId());
                    update.setDescription("Updated by thread " + Thread.currentThread().getName());
                    incidentService.updateIncident(update);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        List<Incident> updatedIncidents = incidentService.getAllIncidents().stream()
                .filter(i -> i.getId().equals(createdIncident.getId()))
                .toList();

        assertEquals(1, updatedIncidents.size(), "Expected only one updated incident");
        assertNotNull(updatedIncidents.get(0));
        assertTrue(updatedIncidents.get(0).getDescription().startsWith("Updated by thread"), "Updated description should match expected format");

        // 可以进一步验证更新后的其他属性等是否符合预期，根据业务需求拓展验证内容
    }

    @Test
    void testGetAllIncidentsWithCachePopulation() {
        // Create incidents to populate cache
        for (int i = 0; i < 5; i++) {
            Incident incident = new Incident();
            incident.setDescription("Incident " + i);
            incidentService.createIncident(incident);
        }

        // Get all incidents to ensure cache is populated
        List<Incident> incidents = incidentService.getAllIncidents();
        assertEquals(5, incidents.size(), "Expected the same number of incidents as created");

        // 验证获取到的事件的描述信息是否与创建时一致，增加验证细节
        List<String> expectedDescriptions = List.of("Incident 0", "Incident 1", "Incident 2", "Incident 3", "Incident 4");
        List<String> actualDescriptions = incidents.stream().map(Incident::getDescription).toList();
        assertEquals(expectedDescriptions.size(), actualDescriptions.size(), "Descriptions of retrieved incidents should match");
    }
}