package com.bebub.genderbub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TurtleTracker {
    private static final Map<UUID, Long> trackedMobs = new ConcurrentHashMap<>();
    private static final long TRACKING_TIME = 200;
    
    public static void startTracking(UUID uuid, long currentTime) {
        trackedMobs.put(uuid, currentTime + TRACKING_TIME);
    }
    
    public static boolean isTracking(UUID uuid) {
        return trackedMobs.containsKey(uuid);
    }
    
    public static boolean isTimeExpired(UUID uuid, long currentTime) {
        Long endTime = trackedMobs.get(uuid);
        if (endTime == null) return true;
        return currentTime >= endTime;
    }
    
    public static void clear(UUID uuid) {
        trackedMobs.remove(uuid);
    }
}