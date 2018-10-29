package com.brandon3055.spawnmanager;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import com.brandon3055.brandonscore.utils.LogHelperBC;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

/**
 * Created by brandon3055 on 27/10/18.
 */
public class SpawnDataManager {

    public static final Map<ResourceLocation, EntityEntry> DEFAULT_SPAWNS = new HashMap<>();
    public static Map<ResourceLocation, EntityEntry> entitySpawnMap = new HashMap<>();

    public static void saveDefaultSpawns() {
        if (DEFAULT_SPAWNS.isEmpty()){
            getSpawns(DEFAULT_SPAWNS);
        }
    }

    public static void generateDefaultSpawnConfig() {
        getSpawns(entitySpawnMap);
        //        entitySpawnMap.clear();//This should already be empty at this point but just in case.
//
//        //Pull out every single spawn into its ows EntitySpawn
//        for (Map.Entry<ResourceLocation, Biome> entry : ForgeRegistries.BIOMES.getEntries()) {
//            ResourceLocation biomeName = entry.getKey();
//            Biome biome = entry.getValue();
//
//            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
//                List<SpawnListEntry> spawnList = biome.getSpawnableList(creatureType);
//                for (SpawnListEntry spawn : spawnList) {
//                    ResourceLocation entityID = EntityList.getKey(spawn.entityClass);
//                    if (entityID != null) {
//                        EntityEntry entityEntry = entitySpawnMap.computeIfAbsent(entityID, name -> new EntityEntry(spawn.entityClass, entityID));
//                        entityEntry.spawns.add(new EntitySpawn(spawn, creatureType, biomeName, biome));
//                    }
//                }
//            }
//        }
//
//        //Combine identical spawns.
//        for (EntityEntry entityEntry : entitySpawnMap.values()) {
//            LinkedList<EntitySpawn> spawns = new LinkedList<>(entityEntry.spawns);
//            entityEntry.spawns.clear();
//
//            while (!spawns.isEmpty()) {
//                EntitySpawn spawn = spawns.removeFirst();
//
//                Iterator<EntitySpawn> i = spawns.iterator();
//                while (i.hasNext()) {
//                    EntitySpawn test = i.next();
//                    if (test == spawn) continue;
//
//                    if (spawn.isSpawnIdentical(test)) {
//                        spawn.biomeMap.putAll(test.biomeMap);
//                        i.remove();
//                    }
//                }
//
//                entityEntry.spawns.add(spawn);
//            }
//        }
    }

    private static void getSpawns(Map<ResourceLocation, EntityEntry> map) {
        map.clear();//This should already be empty at this point but just in case.

        //Pull out every single spawn into its ows EntitySpawn
        for (Map.Entry<ResourceLocation, Biome> entry : ForgeRegistries.BIOMES.getEntries()) {
            ResourceLocation biomeName = entry.getKey();
            Biome biome = entry.getValue();

            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                List<SpawnListEntry> spawnList = biome.getSpawnableList(creatureType);
                for (SpawnListEntry spawn : spawnList) {
                    ResourceLocation entityID = EntityList.getKey(spawn.entityClass);
                    if (entityID != null) {
                        EntityEntry entityEntry = map.computeIfAbsent(entityID, name -> new EntityEntry(spawn.entityClass, entityID));
                        entityEntry.spawns.add(new EntitySpawn(spawn, creatureType, biomeName, biome));
                    }
                }
            }
        }

        //Combine identical spawns.
        for (EntityEntry entityEntry : map.values()) {
            LinkedList<EntitySpawn> spawns = new LinkedList<>(entityEntry.spawns);
            entityEntry.spawns.clear();

            while (!spawns.isEmpty()) {
                EntitySpawn spawn = spawns.removeFirst();

                Iterator<EntitySpawn> i = spawns.iterator();
                while (i.hasNext()) {
                    EntitySpawn test = i.next();
                    if (test == spawn) continue;

                    if (spawn.isSpawnIdentical(test)) {
                        spawn.biomeMap.putAll(test.biomeMap);
                        i.remove();
                    }
                }

                entityEntry.spawns.add(spawn);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void onLoaded() {
        for (ResourceLocation key : ForgeRegistries.ENTITIES.getKeys()) {
            Class<? extends Entity> clazz = EntityList.getClass(key);
            if (!entitySpawnMap.containsKey(key) && clazz != null && EntityLiving.class.isAssignableFrom(clazz)) {
                entitySpawnMap.put(key, new EntityEntry((Class<? extends EntityLiving>) clazz, key));
            }
        }
    }

    public static void applySpawnConfig() {
        //Clear all existing spawns
        for (Biome biome : ForgeRegistries.BIOMES.getValues()) {
            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                biome.getSpawnableList(creatureType).clear();
            }
        }

        entitySpawnMap.values().forEach(EntityEntry::insert);
    }

    public static void serialize(MCDataOutput output) {
        output.writeVarInt(entitySpawnMap.size());
        entitySpawnMap.values().forEach(entry -> entry.serialize(output));
    }

    public static void deSerialize(MCDataInput input) {
        try {
            int entryCount = input.readVarInt();
            entitySpawnMap.clear();
            for (int i = 0; i < entryCount; i++) {
                EntityEntry entry = EntityEntry.deSerialize(input);
                entitySpawnMap.put(entry.id, entry);
            }
            applySpawnConfig();
            SMConfig.save();
        }
        catch (Throwable e) {
            e.printStackTrace();//Just in case
            SMConfig.load();
        }
    }

    public static void resetEntity(ResourceLocation key) {
        if (DEFAULT_SPAWNS.containsKey(key)){
            entitySpawnMap.put(key, DEFAULT_SPAWNS.get(key));
            SMConfig.save();
            SMConfig.load();//Cheaty way of rebuilding the entity spawns map (Do not want objects from the default spawns map to exist in that map)
            applySpawnConfig();
        }
    }

    public static class EntityEntry {
        public Class<? extends EntityLiving> entity;
        public ResourceLocation id;
        public LinkedList<EntitySpawn> spawns = new LinkedList<>();

        EntityEntry() {
        }

        public EntityEntry(Class<? extends EntityLiving> entity, ResourceLocation id) {
            this.entity = entity;
            this.id = id;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", id.toString());
            JsonArray spawnArray = new JsonArray();
            spawns.forEach(spawn -> spawnArray.add(spawn.toJson()));
            obj.add("spawns", spawnArray);
            return obj;
        }

        public static EntityEntry fromJson(JsonObject obj) {
            EntityEntry entry = new EntityEntry();
            try {
                entry.id = new ResourceLocation(JsonUtils.getString(obj, "id"));
                Class<? extends Entity> clazz = EntityList.getClass(entry.id);
                if (clazz != null && EntityLiving.class.isAssignableFrom(clazz)) {
                    //noinspection unchecked ^
                    entry.entity = (Class<? extends EntityLiving>) clazz;
                }

                JsonArray spawns = obj.get("spawns").getAsJsonArray();
                for (JsonElement element : spawns) {
                    entry.spawns.add(EntitySpawn.fromJson(element.getAsJsonObject()));
                }
            }
            catch (Throwable e) {
                LogHelperBC.error("An error occurred while loading an entity spawn entry from json!");
                e.printStackTrace();
            }
            return entry;
        }

        public void serialize(MCDataOutput output) {
            output.writeResourceLocation(id);
            output.writeVarInt(spawns.size());
            spawns.forEach(spawn -> spawn.serialize(output));
        }

        public static EntityEntry deSerialize(MCDataInput input) {
            EntityEntry entry = new EntityEntry();
            entry.id = input.readResourceLocation();
            Class<? extends Entity> clazz = EntityList.getClass(entry.id);
            if (clazz != null && EntityLiving.class.isAssignableFrom(clazz)) {
                //noinspection unchecked ^
                entry.entity = (Class<? extends EntityLiving>) clazz;
            }
            int spawnCount = input.readVarInt();
            for (int i = 0; i < spawnCount; i++) {
                entry.spawns.add(EntitySpawn.deSerialize(input));
            }
            return entry;
        }

        private void insert() {
            if (entity != null) {
                spawns.forEach(spawn -> spawn.insert(entity));
            }
        }
    }

    public static class EntitySpawn {
        public EnumCreatureType type;
        public int weight;
        public int minGroupSize;
        public int maxGroupSize;
        public LinkedHashMap<ResourceLocation, Biome> biomeMap = new LinkedHashMap<>();

        EntitySpawn() {
        }

        public EntitySpawn(EnumCreatureType type, int weight, int minGroupSize, int maxGroupSize) {
            this.type = type;
            this.weight = weight;
            this.minGroupSize = minGroupSize;
            this.maxGroupSize = maxGroupSize;
        }

        public EntitySpawn(SpawnListEntry spawn, EnumCreatureType type, ResourceLocation biomekey, Biome biome) {
            this.weight = spawn.itemWeight;
            this.minGroupSize = spawn.minGroupCount;
            this.maxGroupSize = spawn.maxGroupCount;
            this.type = type;
            this.biomeMap.put(biomekey, biome);
        }

        private boolean isSpawnIdentical(EntitySpawn other) {
            return other.type == type && other.weight == weight && other.minGroupSize == minGroupSize && other.maxGroupSize == maxGroupSize;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", type.name());
            obj.addProperty("weight", weight);
            obj.addProperty("min_group", minGroupSize);
            obj.addProperty("max_group", maxGroupSize);
            JsonArray biomes = new JsonArray();
            biomeMap.keySet().forEach(rs -> biomes.add(rs.toString()));
            obj.add("biomes", biomes);
            return obj;
        }

        public static EntitySpawn fromJson(JsonObject obj) {
            EntitySpawn spawn = new EntitySpawn();
            try {
                spawn.type = EnumCreatureType.valueOf(JsonUtils.getString(obj, "type"));
                spawn.weight = JsonUtils.getInt(obj, "weight");
                spawn.minGroupSize = JsonUtils.getInt(obj, "min_group");
                spawn.maxGroupSize = JsonUtils.getInt(obj, "max_group");
                JsonArray biomes = obj.get("biomes").getAsJsonArray();
                for (JsonElement element : biomes) {
                    ResourceLocation name = new ResourceLocation(element.getAsString());
                    spawn.biomeMap.put(name, ForgeRegistries.BIOMES.getValue(name));
                }
            }
            catch (Throwable e) {
                LogHelperBC.error("An error occurred while loading an entity spawn from json!");
                e.printStackTrace();
            }
            return spawn;
        }

        public void serialize(MCDataOutput output) {
            output.writeEnum(type);
            output.writeVarInt(weight);
            output.writeVarInt(minGroupSize);
            output.writeVarInt(maxGroupSize);
            output.writeVarInt(biomeMap.size());
            biomeMap.keySet().forEach(output::writeResourceLocation);
        }

        public static EntitySpawn deSerialize(MCDataInput input) {
            EntitySpawn spawn = new EntitySpawn();
            spawn.type = input.readEnum(EnumCreatureType.class);
            spawn.weight = input.readVarInt();
            spawn.minGroupSize = input.readVarInt();
            spawn.maxGroupSize = input.readVarInt();
            int biomeCount = input.readVarInt();
            for (int i = 0; i < biomeCount; i++) {
                ResourceLocation rs = input.readResourceLocation();
                spawn.biomeMap.put(rs, ForgeRegistries.BIOMES.getValue(rs));
            }
            return spawn;
        }

        private void insert(Class<? extends EntityLiving> entityclass) {
            for (Biome biome : biomeMap.values()) {
                biome.getSpawnableList(type).add(new SpawnListEntry(entityclass, weight, minGroupSize, maxGroupSize));
            }
        }
    }
}
