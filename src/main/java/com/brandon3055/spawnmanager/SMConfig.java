package com.brandon3055.spawnmanager;

import com.brandon3055.brandonscore.handlers.FileHandler;
import com.brandon3055.brandonscore.utils.LogHelperBC;
import com.brandon3055.spawnmanager.SpawnDataManager.EntityEntry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Created by brandon3055 on 27/10/18.
 */
public class SMConfig {

    private static File configFile;

    public static void initialize() {
        configFile = new File(FileHandler.brandon3055Folder, "SpawnManager.json");

        SpawnDataManager.saveDefaultSpawns();

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            SpawnDataManager.generateDefaultSpawnConfig();
            save();
        }

        load();
    }

    public static void save() {
        JsonObject jObj = new JsonObject();

        JsonArray spawnArray = new JsonArray();
        SpawnDataManager.entitySpawnMap.values().forEach(entry -> spawnArray.add(entry.toJson()));
        jObj.add("spawn_list", spawnArray);

        try {
            JsonWriter writer = new JsonWriter(new FileWriter(configFile));
            writer.setIndent("  ");
            Streams.write(jObj, writer);
            writer.flush();
            IOUtils.closeQuietly(writer);
        }
        catch (Exception e) {
            LogHelperBC.error("Error saving SpawnManager config");
            e.printStackTrace();
        }
    }

    public static void load() {
        JsonObject jObj;
        try {
            JsonParser parser = new JsonParser();
            FileReader reader = new FileReader(configFile);
            JsonElement element = parser.parse(reader);
            IOUtils.closeQuietly(reader);
            jObj = element.getAsJsonObject();
        }
        catch (Exception e) {
            LogHelperBC.error("Error loading SpawnManager config");
            e.printStackTrace();
            return;
        }

        JsonArray spawnArray = jObj.get("spawn_list").getAsJsonArray();

        SpawnDataManager.entitySpawnMap.clear();
        for (JsonElement element : spawnArray) {
            EntityEntry entry = EntityEntry.fromJson(element.getAsJsonObject());
            SpawnDataManager.entitySpawnMap.put(entry.id, entry);
        }
        SpawnDataManager.onLoaded();
    }
}
