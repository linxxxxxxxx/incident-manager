package org.example.incidentmanager.service;

import com.google.common.cache.CacheLoader;
import org.example.incidentmanager.model.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class IncidentService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);

    private final Map<Long, Incident> incidentMap = new HashMap<>();

    // 获取incidentMap中所有的事件（仅用于测试等特定场）
   public Collection<Incident> getIncidentsForTesting() {
        return incidentMap.values();
    }

    // 获取incidentCache中所有的事件（仅用于测试等特定场）
    public Collection<Incident> getIncidentCacheForTesting() {
        return incidentCache.asMap().values();
    }

    private final AtomicLong nextId = new AtomicLong(1L);

    // 引入读写锁
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    // 使用Guava Cache来缓存事件列表，设置缓存过期时间为48小时（示例，可按需调整）
    private final Cache<Long, Incident> incidentCache = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(2, TimeUnit.DAYS)
            .build();

    // 设定一个过期时间阈值，例如超过48小时未更新的数据视为过期，可按需调整
    private static final long DATA_EXPIRATION_THRESHOLD = 2 * 24 * 60 * 60 * 1000L;

    // 定时清理 incidentMap 和 incidentCache 中过期数据的方法
    @Scheduled(cron = "0 0 2 * * *") // 每天凌晨2点执行清理任务，可根据实际需求调整定时表达式
    public void cleanExpiredData() {
        long currentTime = System.currentTimeMillis();
        writeLock.lock();
        try {
            // 清理 incidentMap 中的过期数据
            Iterator<Map.Entry<Long, Incident>> incidentMapIterator = incidentMap.entrySet().iterator();
            while (incidentMapIterator.hasNext()) {
                Map.Entry<Long, Incident> entry = incidentMapIterator.next();
                if (currentTime - entry.getValue().getUpdatedDate().getTime() > DATA_EXPIRATION_THRESHOLD) {
                    incidentMapIterator.remove();
                    // 同时从缓存中移除对应的事件
                    incidentCache.invalidate(entry.getKey());
                }
            }
            // 主动淘汰部分缓存数据，优化缓存空间（当缓存大小接近最大容量时）
            if (incidentCache.size() >= CacheBuilder.newBuilder().maximumSize(100000).build().size()) {
                // 创建一个用于存放要淘汰的键的集合
                Set<Long> keysToRemove = new HashSet<>();
                Iterator<Long> cacheKeyIterator = incidentCache.asMap().keySet().iterator();
                while (cacheKeyIterator.hasNext()) {
                    Long key = cacheKeyIterator.next();
                    keysToRemove.add(key);
                    if (keysToRemove.size() >= (100000 - 100000 * 0.8)) {
                        // 当要移除的键数量达到需要淘汰的数量（从最大容量到80%最大容量之间的差值）时停止收集键
                        break;
                    }
                }
                // 使用 invalidateAll 方法批量移除缓存中的对应键的数据
                incidentCache.invalidateAll(keysToRemove);
            }
        } finally {
            writeLock.unlock();
            verifyIncidentMapAndCacheConsistency();
        }
    }


    // 创建事件方法
    public Incident createIncident(Incident incident) {
        if (null == incident) {
            logger.error("创建事件对象是null");
            throw new NullPointerException("Incident cannot be null");
        }
        Date currentDate = new Date();
        writeLock.lock();
        try {
            incident.setId(nextId.getAndIncrement());
            incident.setCreatedDate(currentDate);
            incident.setUpdatedDate(currentDate);
            incidentMap.put(incident.getId(), incident);

            try {
                // 将新创建的事件放入缓存
                incidentCache.put(incident.getId(), incident);
            } catch (Exception e) {
                logger.error("Failed to put new incident into cache. Incident: {}, Error: {}", incident, e.getMessage());
                // 从incidentMap中移除刚才插入的数据，保持一致性（可根据业务需求决定是否这样处理，也可采用其他补偿机制）
                incidentMap.remove(incident.getId());
                throw e;
            }

            return incident;
        } finally {
            writeLock.unlock();
        }
    }

    // 修改事件方法
    public Incident updateIncident(Incident updatedIncident) {
        if (null == updatedIncident) {
            logger.error("更新事件对象是null");
            throw new NullPointerException("Incident cannot be null");
        }
        writeLock.lock();
        try {
            if (incidentMap.containsKey(updatedIncident.getId())) {
                Date currentDate = new Date();
                updatedIncident.setUpdatedDate(currentDate);
                updatedIncident.setCreatedDate(incidentMap.get(updatedIncident.getId()).getCreatedDate());
                incidentMap.put(updatedIncident.getId(), updatedIncident);

                int maxRetry = 3; // 设置最大重试次数
                boolean cacheUpdated = false;
                for (int i = 0; i < maxRetry; i++) {
                    try {
                        // 更新缓存中的对应事件
                        incidentCache.put(updatedIncident.getId(), updatedIncident);
                        cacheUpdated = true;
                        break;
                    } catch (Exception e) {
                        logger.error("更新缓存失败，事件id: {}, retry times: {}", updatedIncident.getId(), i + 1, e);
                    }
                }

                if (!cacheUpdated) {
                    // 根据业务需求决定是否回滚incidentMap中的更新操作，这里示例回滚
                    incidentMap.put(updatedIncident.getId(), incidentMap.get(updatedIncident.getId()));
                    logger.error("Failed to update cache after {} retries, rollback incidentMap update for id: {}", maxRetry, updatedIncident.getId());
                }

                return updatedIncident;
            }
            logger.error("尝试更新不存在的事件，事件id: {}", updatedIncident.getId());
            throw new IllegalArgumentException("Incident with id " + updatedIncident.getId() + " not found");
        } finally {
            writeLock.unlock();
        }
    }

    // 删除事件方法
    public void deleteIncident(Long id) {
        writeLock.lock();
        try {
            if (incidentMap.containsKey(id)) {
                incidentMap.remove(id);
                try {
                    // 从缓存中移除对应的事件
                    incidentCache.invalidate(id);
                } catch (Exception e) {
                    logger.error("Failed to invalidate incident from incidentCache during delete. Id: {}, Error: {}", id, e.getMessage());
                    // 根据业务需求添加其他处理逻辑，比如记录下来后续再次尝试删除等
                }
            } else {
                logger.error("尝试删除不存在的事件，事件id: {}", id);
                throw new IllegalArgumentException("Incident with id " + id + " not found");
            }
        } finally {
            writeLock.unlock();
        }
    }

    private long lastCacheUpdateTime = System.currentTimeMillis();
    private static final long CACHE_UPDATE_INTERVAL = 60 * 1000; // 60秒，可按需调整

    // 获取所有事件方法，先从缓存中获取，如果缓存没有则从内存存储（IncidentMap）中获取并放入缓存
    public List<Incident> getAllIncidents() {
        boolean needUpdateCache = (System.currentTimeMillis() - lastCacheUpdateTime > CACHE_UPDATE_INTERVAL)
                && incidentCache.size() < incidentMap.size();
        if (needUpdateCache) {
            try {
                // 使用可中断锁，避免线程长时间等待锁导致死锁等问题
                writeLock.lockInterruptibly();
                Map<Long, Incident> cachedIncidents = new HashMap<>();
                // 直接从 incidentMap 中获取数据添加到缓存和 cachedIncidents 中
                for (Map.Entry<Long, Incident> entry : incidentMap.entrySet()) {
                    Long key = entry.getKey();
                    Incident value = entry.getValue();
                    try {
                        incidentCache.put(key, value); // 捕获单个put操作可能出现的异常
                    } catch (Exception e) {
                        logger.error("Failed to put incident into cache during update. Key: {}, Error: {}", key, e.getMessage());
                        // 根据业务需求决定是否继续处理，这里选择继续尝试其他数据的添加
                    }
                    cachedIncidents.put(key, value);
                }
                lastCacheUpdateTime = System.currentTimeMillis();
                return new ArrayList<>(cachedIncidents.values());
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while trying to acquire write lock for cache update.", e);
                Thread.currentThread().interrupt(); // 恢复中断状态，遵循中断机制规范
                return new ArrayList<>(); // 返回空列表，避免返回不确定状态的数据
            } finally {
                writeLock.unlock();
            }
        } else {
            try {
                readLock.lockInterruptibly(); // 同样使用可中断锁
                try {
                    return new ArrayList<>(incidentCache.asMap().values());
                } finally {
                    readLock.unlock();
                }
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while trying to acquire read lock for cache access.", e);
                Thread.currentThread().interrupt();
                return new ArrayList<>();
            }
        }
    }

    private void verifyIncidentMapAndCacheConsistency() {
        Set<Long> incidentMapKeys = incidentMap.keySet();
        Set<Long> incidentCacheKeys = incidentCache.asMap().keySet();
        if (!incidentMapKeys.equals(incidentCacheKeys)) {
            logger.error("IncidentMap and IncidentCache key sets are not consistent. IncidentMap keys: {}, IncidentCache keys: {}", incidentMapKeys, incidentCacheKeys);
        }

        for (Long key : incidentMapKeys) {
            Incident incidentInMap = incidentMap.get(key);
            Incident incidentInCache = incidentCache.getIfPresent(key);
            if (!incidentInMap.equals(incidentInCache)) {
                logger.error("Data inconsistency found for key: {}. Incident in Map: {}, Incident in Cache: {}", key, incidentInMap, incidentInCache);
            }
        }
    }
}
