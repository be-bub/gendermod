package com.bebub.gendermod.client;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientGenderCache {
    private static final Map<UUID, String> GENDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> STERILE_CACHE = new ConcurrentHashMap<>();
    private static final String CACHE_FILE = "gendermod_cache.dat";

    public static void put(UUID uuid, String gender, boolean sterile) {
        GENDER_CACHE.put(uuid, gender);
        STERILE_CACHE.put(uuid, sterile);
        save();
    }

    public static String getGender(UUID uuid) {
        return GENDER_CACHE.get(uuid);
    }
    
    public static boolean isSterile(UUID uuid) {
        return STERILE_CACHE.getOrDefault(uuid, false);
    }

    public static void load() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) return;
        
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                UUID uuid = new UUID(dis.readLong(), dis.readLong());
                String gender = dis.readUTF();
                boolean sterile = dis.readBoolean();
                GENDER_CACHE.put(uuid, gender);
                STERILE_CACHE.put(uuid, sterile);
            }
        } catch (IOException e) {}
    }

    public static void save() {
        File file = new File(CACHE_FILE);
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(GENDER_CACHE.size());
            for (Map.Entry<UUID, String> entry : GENDER_CACHE.entrySet()) {
                dos.writeLong(entry.getKey().getMostSignificantBits());
                dos.writeLong(entry.getKey().getLeastSignificantBits());
                dos.writeUTF(entry.getValue());
                dos.writeBoolean(STERILE_CACHE.getOrDefault(entry.getKey(), false));
            }
        } catch (IOException e) {}
    }

    public static void clear() {
        GENDER_CACHE.clear();
        STERILE_CACHE.clear();
        save();
    }
}