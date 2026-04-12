package com.bebub.genderbub.client;

import com.bebub.genderbub.GenderMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientGenderCache {
    private static final String CACHE_FILE = "genderbub/genderbub_genders.dat";
    private static final int CACHE_VERSION = 2;
    
    private static final Map<UUID, String> MOB_ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> GENDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> STERILE_CACHE = new ConcurrentHashMap<>();

    private static File getCacheFile() {
        return FMLPaths.CONFIGDIR.get().resolve(CACHE_FILE).toFile();
    }

    public static void put(UUID uuid, String mobId, String gender, boolean sterile) {
        MOB_ID_CACHE.put(uuid, mobId);
        GENDER_CACHE.put(uuid, gender);
        STERILE_CACHE.put(uuid, sterile);
        save();
    }

    public static void remove(UUID uuid) {
        MOB_ID_CACHE.remove(uuid);
        GENDER_CACHE.remove(uuid);
        STERILE_CACHE.remove(uuid);
        save();
    }

    public static String getGender(UUID uuid) {
        return GENDER_CACHE.get(uuid);
    }
    
    public static boolean isSterile(UUID uuid) {
        return STERILE_CACHE.getOrDefault(uuid, false);
    }
    
    public static void clear() {
        MOB_ID_CACHE.clear();
        GENDER_CACHE.clear();
        STERILE_CACHE.clear();
        save();
    }
    
    public static int getSize() {
        return GENDER_CACHE.size();
    }
    
    public static void cleanupByMobList(Set<String> enabledMobs) {
        boolean changed = false;
        
        for (Map.Entry<UUID, String> entry : MOB_ID_CACHE.entrySet()) {
            UUID uuid = entry.getKey();
            String mobId = entry.getValue();
            
            if (!enabledMobs.contains(mobId)) {
                MOB_ID_CACHE.remove(uuid);
                GENDER_CACHE.remove(uuid);
                STERILE_CACHE.remove(uuid);
                changed = true;
                GenderMod.LOGGER.debug("Removed {} ({}) from cache - not in enabled mobs", mobId, uuid);
            }
        }
        
        if (changed) {
            save();
            GenderMod.LOGGER.info("Cleaned up cache, now {} entries", MOB_ID_CACHE.size());
        }
    }

    public static void load() {
        File file = getCacheFile();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                GenderMod.LOGGER.error("Failed to create cache file", e);
            }
            return;
        }
        
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            int version = dis.readInt();
            
            if (version != CACHE_VERSION) {
                GenderMod.LOGGER.info("Cache version mismatch ({} != {}), clearing cache", version, CACHE_VERSION);
                clear();
                return;
            }
            
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                UUID uuid = new UUID(dis.readLong(), dis.readLong());
                String mobId = dis.readUTF();
                String gender = dis.readUTF();
                boolean sterile = dis.readBoolean();
                MOB_ID_CACHE.put(uuid, mobId);
                GENDER_CACHE.put(uuid, gender);
                STERILE_CACHE.put(uuid, sterile);
            }
            GenderMod.LOGGER.info("Loaded {} gender entries from cache", size);
        } catch (IOException e) {
            GenderMod.LOGGER.error("Failed to load cache", e);
        }
    }

    public static void save() {
        File file = getCacheFile();
        try {
            file.getParentFile().mkdirs();
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(CACHE_VERSION);
                dos.writeInt(GENDER_CACHE.size());
                for (Map.Entry<UUID, String> entry : GENDER_CACHE.entrySet()) {
                    UUID uuid = entry.getKey();
                    dos.writeLong(uuid.getMostSignificantBits());
                    dos.writeLong(uuid.getLeastSignificantBits());
                    dos.writeUTF(MOB_ID_CACHE.getOrDefault(uuid, "unknown"));
                    dos.writeUTF(entry.getValue());
                    dos.writeBoolean(STERILE_CACHE.getOrDefault(uuid, false));
                }
            }
        } catch (IOException e) {
            GenderMod.LOGGER.error("Failed to save cache", e);
        }
    }
}