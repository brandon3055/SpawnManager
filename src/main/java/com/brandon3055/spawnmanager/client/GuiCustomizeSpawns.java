package com.brandon3055.spawnmanager.client;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.packet.PacketCustom;
import com.brandon3055.brandonscore.client.gui.modulargui.GuiElementManager;
import com.brandon3055.brandonscore.client.gui.modulargui.MGuiElementBase;
import com.brandon3055.brandonscore.client.gui.modulargui.ModularGuiScreen;
import com.brandon3055.brandonscore.client.gui.modulargui.baseelements.GuiButton;
import com.brandon3055.brandonscore.client.gui.modulargui.baseelements.GuiScrollElement;
import com.brandon3055.brandonscore.client.gui.modulargui.baseelements.GuiSlideControl;
import com.brandon3055.brandonscore.client.gui.modulargui.guielements.GuiBorderedRect;
import com.brandon3055.brandonscore.client.gui.modulargui.guielements.GuiLabel;
import com.brandon3055.brandonscore.client.gui.modulargui.guielements.GuiSelectDialog;
import com.brandon3055.brandonscore.client.gui.modulargui.guielements.GuiTextField;
import com.brandon3055.brandonscore.client.gui.modulargui.lib.GuiAlign;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.spawnmanager.SpawnManager;
import com.brandon3055.spawnmanager.SpawnDataManager.EntityEntry;
import com.brandon3055.spawnmanager.SpawnDataManager.EntitySpawn;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.brandon3055.brandonscore.client.gui.modulargui.baseelements.GuiSlideControl.SliderRotation.VERTICAL;
import static net.minecraft.util.text.TextFormatting.GRAY;

/**
 * Created by brandon3055 on 28/10/18.
 */
public class GuiCustomizeSpawns extends ModularGuiScreen {
    private Map<ResourceLocation, EntityEntry> entitySpawnMap = new HashMap<>();
    private EntityPlayer player;

    public GuiScrollElement entityList;
    public GuiTextField search;
    public EntityConfigurator configurator;
    private ResourceLocation selectedEntity = null;

    public GuiCustomizeSpawns(EntityPlayer player) {
        this.player = player;
        //Request spawn config from server.
        new PacketCustom(SpawnManager.NET_CHANNEL, 1).sendToServer();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void addElements(GuiElementManager manager) {
        GuiLabel guiTitle = new GuiLabel("Configure Mob Spawn Rules");
        guiTitle.setTextColour(0xFFFFFF);
        guiTitle.addAndFireReloadCallback(guiLabel -> guiLabel.setPosAndSize(0, 4, width, 8));
        manager.add(guiTitle);

        GuiBorderedRect lb = new GuiBorderedRect().setColours(0x80000000, 0xFF808080);
        lb.addAndFireReloadCallback(element -> element.setPosAndSize(5, 15, 100, height - 36));
        lb.setInsets(1, 1, 1, 1);
        manager.add(lb);


        GuiSlideControl scrollBar = new GuiSlideControl(VERTICAL);
        scrollBar.setBackgroundElement(new GuiBorderedRect().setColours(0x80000000, 0xFF000000, 0xFF808080, 0xFFFFFFFF));
        scrollBar.setSliderElement(new MGuiElementBase() {
            @Override
            public void renderElement(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
                drawShadedRect(this.xPos(), this.yPos(), this.xSize(), this.ySize(), 1, 0XFF9F9F9F, 0XFFC7C7C7, 0XFF555555, 0XFF6F6F6F);
            }
        });

        entityList = new GuiScrollElement();
        entityList.setVerticalScrollBar(scrollBar);
        entityList.setListMode(GuiScrollElement.ListMode.VERT_LOCK_POS);
        entityList.addAndFireReloadCallback(element -> element.setPosAndSize(lb.getInsetRect()));
        entityList.setStandardScrollBehavior();
//        entityList.setInsets(, 0, 0, 0);
        lb.addChild(entityList);
        scrollBar.addAndFireReloadCallback(sb -> {
            sb.setPosAndSize(lb.maxXPos() + 2, lb.yPos(), 10, lb.ySize());
            sb.getBackgroundElement().setPosAndSize(sb);
            sb.getSliderElement().setXSize(sb.xSize() - 2);
        });

        search = new GuiTextField();
        search.setColours(0xFF000000, 0xFFFFFFFF);
        search.addAndFireReloadCallback(field -> field.setPosAndSize(lb.xPos(), lb.maxYPos() + 2, 112, 14));
        search.setChangeListener(this::updateList);
        manager.add(search);

        GuiLabel searchLabel = new GuiLabel("Filter Entity or @mod");
        searchLabel.setTextColour(0x909090).setTrim(false);
        searchLabel.addAndFireReloadCallback(guiLabel -> guiLabel.setPosAndSize(search).translate(3, 0));
        searchLabel.setEnabledCallback(() -> !search.isFocused() && search.getText().isEmpty());
        searchLabel.setAlignment(GuiAlign.LEFT);
        manager.add(searchLabel);

        configurator = new EntityConfigurator(this);
        configurator.addAndFireReloadCallback(c -> c.setPosAndSize(scrollBar.maxXPos() + 4, scrollBar.yPos(), width - scrollBar.maxXPos() - 8, height - scrollBar.yPos() - 4));
        configurator.setEnabledCallback(() -> selectedEntity != null);
        manager.add(configurator);

    }

    @Override
    public void reloadGui() {
        super.reloadGui();
        entityList.getVerticalScrollBar().setXPos(entityList.maxXPos() + 1);
    }

    private void updateList() {
        double prevPos = entityList.getVerticalScrollBar().getRawPos();
        int prevCount = entityList.getScrollingElements().size();
        entityList.resetScrollPositions();
        entityList.clearElements();

        for (ResourceLocation key : entitySpawnMap.keySet()) {
            String trans = EntityList.getTranslationName(key);

            String filter = search.getText();
            if (trans != null && !filter.isEmpty()) {
                if (filter.startsWith("@")) {
                    if (!key.getResourceDomain().contains(filter.substring(1).toLowerCase())) {
                        continue;
                    }
                }
                else if (!trans.toLowerCase().contains(filter.toLowerCase())) {
                    continue;
                }
            }

            //Until the button is added to the list its position is 0, 0 so all positions are relative to 0, 0.
            GuiButton button = new GuiButton();
            button.setToggleMode(true);
            GuiBorderedRect background = new GuiBorderedRect().setColours(0xFF000000, 0xFFFFFFFF);
            background.setEnabledCallback(button::getToggleState);
            button.addChild(background);
            button.setToggleStateSupplier(() -> selectedEntity != null && selectedEntity.equals(key));

            int ySize;
            int xSize = entityList.xSize();

            GuiLabel entityName = new GuiLabel(trans);
            entityName.setWrap(true).setAlignment(GuiAlign.LEFT);
            entityName.setHeightForText(xSize - 4);
            entityName.setPos(2, 2);
            button.addChild(entityName);

            GuiLabel modName = new GuiLabel(key.getResourceDomain());
            modName.setWrap(true).setAlignment(GuiAlign.LEFT);
            modName.setHeightForText(xSize - 4);
            modName.setPos(2, entityName.maxYPos() + 2);
            modName.setTextColour(0x909090);
            button.addChild(modName);

            ySize = modName.maxYPos() + 2;

            button.setSize(xSize, ySize);
            background.setPosAndSize(button);
            button.setListener(() -> selectEntity(key));
            entityList.addElement(button);
        }

        if (entityList.getScrollingElements().size() == prevCount) {
            entityList.getVerticalScrollBar().updateRawPos(prevPos);
        }
    }

    private void selectEntity(ResourceLocation key) {
        selectedEntity = key;
        configurator.selectionChanged(key, key == null ? null : entitySpawnMap.get(key));
    }

    public void handleDataPacket(MCDataInput input) {
        int prevCount = entitySpawnMap.size();
        entitySpawnMap.clear();
        int entryCount = input.readVarInt();
        for (int i = 0; i < entryCount; i++) {
            EntityEntry entry = EntityEntry.deSerialize(input);
            entitySpawnMap.put(entry.id, entry);
        }

        updateList();
        if (prevCount != entitySpawnMap.size() || (selectedEntity != null && !entitySpawnMap.containsKey(selectedEntity))) {
            selectEntity(null);
        }
        else {
            if (input.readBoolean()) {
                configurator.selectionChanged(selectedEntity, entitySpawnMap.get(selectedEntity));
            }
            configurator.updateFields();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
//        drawBackground(0);
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static class EntityConfigurator extends MGuiElementBase<EntityConfigurator> {
        private GuiCustomizeSpawns gui;
        private GuiScrollElement spawnListElement;
        private GuiScrollElement biomeListElement;
        private GuiTextField biomeSearch;
        private GuiTextField weightField;
        private GuiTextField minGroupField;
        private GuiTextField maxGroupField;
        private GuiButton creatureType;
        private GuiButton addAllBiomes;
        private GuiButton removeAllBiomes;
        private GuiButton addBomesByType;
        private GuiButton removeBomesByType;
        private GuiButton resetEntitySpawns;
        private GuiButton addSpawn;
        private GuiButton removeSpawn;

        private int spawnCount = -1;
        private int selectedSpawn = -1;

        public EntityConfigurator(GuiCustomizeSpawns gui) {
            this.gui = gui;
        }

        @Override
        public void addChildElements() {
            addChild(new GuiBorderedRect().setColours(0x80000000, 0xFF808080).addAndFireReloadCallback(rect -> rect.setPosAndSize(this)));

            GuiLabel listLabel = new GuiLabel("Spawn List");
            listLabel.setInsets(0, 0, 9, 0);
            listLabel.setPosAndSize(xPos() + 2, yPos() + 2, 80, 18).setWrap(true);
            listLabel.setHoverText("Each entity may have one or more spawn entries.",//
                    GRAY + "Each spawn entry has a spawn weight which sets how likely the entity is to spawn relative to other entities.",//
                    GRAY + "Also a min and max group size that determines the size of the group that will spawn.",//
                    GRAY + "Use multiple spawn entries to set different weights / group size for different biomes.");
            addChild(listLabel);

            GuiBorderedRect lb = new GuiBorderedRect().setColours(0x80000000, 0xFF808080);
            lb.addAndFireReloadCallback(element -> element.setPosAndSize(xPos() + 2, listLabel.maxYPos() + 2, 80, ySize() - 24 - 30));
            lb.setInsets(1, 1, 1, 1);
            addChild(lb);

            spawnListElement = new GuiScrollElement();
            spawnListElement.setListMode(GuiScrollElement.ListMode.VERT_LOCK_POS);
            spawnListElement.addAndFireReloadCallback(element -> element.setPosAndSize(lb.getInsetRect()));
            spawnListElement.setStandardScrollBehavior();
            lb.addChild(spawnListElement);
            spawnListElement.getVerticalScrollBar().setHidden(true);

            addSpawn = new GuiButton("Add Spawn").setHoverText("Add a new spawn entry for this entity");
            addSpawn.addAndFireReloadCallback(field -> field.setPosAndSize(lb.xPos(), lb.maxYPos() + 1, lb.xSize(), 14));
            addSpawn.setVanillaButtonRender(true);
            addSpawn.setListener(() -> {
                if (gui.selectedEntity == null) return;
                EntityEntry entry = gui.entitySpawnMap.get(gui.selectedEntity);
                if (entry == null) return;
                entry.spawns.add(new EntitySpawn(EnumCreatureType.CREATURE, 0, 0, 0));
                sendChanges(true);
            });
            lb.addChild(addSpawn);

            removeSpawn = new GuiButton("Remove Spawn").setHoverText("Remove the selected spawn entry for this entity");
            removeSpawn.addAndFireReloadCallback(field -> field.setPosAndSize(addSpawn.xPos(), addSpawn.maxYPos() + 1, lb.xSize(), 14));
            removeSpawn.setVanillaButtonRender(true).setTrim(false);
            removeSpawn.setEnabledCallback(() -> selectedSpawn != -1 && gui.selectedEntity != null);
            removeSpawn.setListener(() -> {
                if (gui.selectedEntity == null || selectedSpawn == -1) return;
                EntityEntry entry = gui.entitySpawnMap.get(gui.selectedEntity);
                if (entry == null || selectedSpawn >= entry.spawns.size()) return;
                entry.spawns.remove(selectedSpawn);
                sendChanges(true);
            });
            lb.addChild(removeSpawn);

            resetEntitySpawns = new GuiButton("Reset Entity").setHoverText("Completely reset this entity to its default spawn configuration.");
            resetEntitySpawns.addAndFireReloadCallback(field -> field.setPosAndSize(maxXPos()- 102, maxYPos() -16, 100, 14));
            resetEntitySpawns.setVanillaButtonRender(true);
            lb.addChild(resetEntitySpawns);

            addConfigFields();
        }

        private void selectionChanged(ResourceLocation key, EntityEntry entry) {
            spawnListElement.clearElements();
            spawnListElement.resetScrollPositions();
            selectedSpawn = -1;

            if (key == null) {
                return;
            }

            for (EntitySpawn spawn_ : entry.spawns) {
                int index = entry.spawns.indexOf(spawn_);
                Supplier<EntitySpawn> spawn = () -> { //This should dynamically update even when the data objects are replaced.
                    EntityEntry ee = gui.entitySpawnMap.get(key);
                    return ee != null && ee.spawns.size() > index ? ee.spawns.get(index) : new EntitySpawn(EnumCreatureType.CREATURE, -1, -1, -1);
                };

                GuiButton button = new GuiButton();
                button.setToggleMode(true);
                GuiBorderedRect background = new GuiBorderedRect().setColours(0xFF000000, 0xFFFFFFFF);
                background.setEnabledCallback(button::getToggleState);
                button.addChild(background);
                button.setToggleStateSupplier(() -> selectedSpawn == index);

                int ySize;
                int xSize = spawnListElement.xSize();

                GuiLabel spawnDetails = new GuiLabel(" \n ");
                spawnDetails.setWrap(true).setAlignment(GuiAlign.LEFT);
                spawnDetails.setHeightForText(xSize - 4);
                spawnDetails.setPos(2, 2);
                spawnDetails.setDisplaySupplier(() -> TextFormatting.GOLD + "Weight, Min, Max\n" + TextFormatting.DARK_AQUA + spawn.get().weight + "," + spawn.get().minGroupSize + "," + spawn.get().maxGroupSize);
                button.addChild(spawnDetails);

                GuiLabel spawnType = new GuiLabel();
                spawnType.setDisplaySupplier(() -> spawn.get().type.name());
                spawnType.setWrap(true).setAlignment(GuiAlign.LEFT);
                spawnType.setHeightForText(xSize - 4);
                spawnType.setPos(2, spawnDetails.maxYPos() + 2);
                spawnType.setTextColour(0x909090);
                button.addChild(spawnType);

                ySize = spawnType.maxYPos() + 2;

                button.setSize(xSize, ySize);
                background.setPosAndSize(button);
                button.setListener(() -> setSelectedSpawn(index));
                spawnListElement.addElement(button);
            }

            updateFields();
        }

        private void setSelectedSpawn(int spawn) {
            selectedSpawn = spawn;
            updateFields();
        }

        private void addConfigFields() {
            GuiLabel biomeListLabel = new GuiLabel("Spawnable Biomes").setHoverText("Sets the biomes to which this spawn entry applies.", TextFormatting.GREEN + "Green indicates this biome is enabled,", TextFormatting.RED + "Red indicates this biome is disabled.");
            biomeListLabel.setPosAndSize(spawnListElement.maxXPos() + 2, yPos() + 2, 87, 18).setTrim(false);
            biomeListLabel.setInsets(0, 0, 9, 0);
            addChild(biomeListLabel);

            addChild(new GuiLabel(GRAY + "Mouseover for more info.").setPosAndSize(spawnListElement.xPos(), yPos() + 12, biomeListLabel.maxXPos() - spawnListElement.xPos(), 8));

            GuiBorderedRect lb = new GuiBorderedRect().setColours(0x80000000, 0xFF808080);
            lb.setEnabledCallback(() -> selectedSpawn != -1 && gui.selectedEntity != null);
            lb.addAndFireReloadCallback(element -> element.setPosAndSize(spawnListElement.maxXPos() + 2, biomeListLabel.maxYPos() + 2, 80, ySize() - 40));
            lb.setInsets(1, 1, 1, 1);
            addChild(lb);
//
            GuiSlideControl scrollBar = new GuiSlideControl(VERTICAL);
            scrollBar.setBackgroundElement(new GuiBorderedRect().setColours(0x80000000, 0xFF000000, 0xFF808080, 0xFFFFFFFF));
            scrollBar.setSliderElement(new MGuiElementBase() {
                @Override
                public void renderElement(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
                    drawShadedRect(this.xPos(), this.yPos(), this.xSize(), this.ySize(), 1, 0XFF9F9F9F, 0XFFC7C7C7, 0XFF555555, 0XFF6F6F6F);
                }
            });

            biomeListElement = new GuiScrollElement();
            biomeListElement.setVerticalScrollBar(scrollBar);
            biomeListElement.setListMode(GuiScrollElement.ListMode.VERT_LOCK_POS);
            biomeListElement.addAndFireReloadCallback(element -> element.setPosAndSize(lb.getInsetRect()));
            biomeListElement.setStandardScrollBehavior();
            lb.addChild(biomeListElement);
            scrollBar.addAndFireReloadCallback(sb -> {
                sb.setPosAndSize(lb.maxXPos() + 1, lb.yPos(), 6, lb.ySize());
                sb.getBackgroundElement().setPosAndSize(sb);
                sb.getSliderElement().setXSize(sb.xSize() - 2);
            });

            biomeSearch = new GuiTextField();
            biomeSearch.setColours(0xFF000000, 0xFFFFFFFF);
            biomeSearch.addAndFireReloadCallback(field -> field.setPosAndSize(lb.xPos(), lb.maxYPos() + 2, 87, 14));
            biomeSearch.setChangeListener(this::updateFields);
            lb.addChild(biomeSearch);

            GuiLabel searchLabel = new GuiLabel("Filter Biomes");
            searchLabel.setTextColour(0x909090).setTrim(false);
            searchLabel.addAndFireReloadCallback(guiLabel -> guiLabel.setPosAndSize(biomeSearch).translate(3, 0));
            searchLabel.setEnabledCallback(() -> !biomeSearch.isFocused() && biomeSearch.getText().isEmpty());
            searchLabel.setAlignment(GuiAlign.LEFT);
            lb.addChild(searchLabel);


            weightField = new GuiTextField();
            weightField.addAndFireReloadCallback(field -> field.setPosAndSize(scrollBar.maxXPos() + 1, scrollBar.yPos() + 10, 100, 16));
            weightField.addChild(new GuiLabel("Spawn Weight").setPosAndSize(weightField.xPos(), weightField.yPos() - 8, 100, 8));
            weightField.setColours(0xFF5f5f60, 0xFFCCCCCC);
            weightField.setValidator(s -> s.isEmpty() || Utils.validInteger(s));
            lb.addChild(weightField);

            minGroupField = new GuiTextField();
            minGroupField.addAndFireReloadCallback(field -> field.setPosAndSize(weightField.xPos(), weightField.maxYPos() + 11, 100, 16));
            minGroupField.addChild(new GuiLabel("Min Group Size").setPosAndSize(minGroupField.xPos(), minGroupField.yPos() - 8, 100, 8));
            minGroupField.setColours(0xFF5f5f60, 0xFFCCCCCC);
            minGroupField.setValidator(s -> s.isEmpty() || Utils.validInteger(s));
            lb.addChild(minGroupField);

            maxGroupField = new GuiTextField();
            maxGroupField.addAndFireReloadCallback(field -> field.setPosAndSize(minGroupField.xPos(), minGroupField.maxYPos() + 11, 100, 16));
            maxGroupField.addChild(new GuiLabel("Max Group Size").setPosAndSize(maxGroupField.xPos(), maxGroupField.yPos() - 8, 100, 8));
            maxGroupField.setColours(0xFF5f5f60, 0xFFCCCCCC);
            maxGroupField.setValidator(s -> s.isEmpty() || Utils.validInteger(s));
            lb.addChild(maxGroupField);

            creatureType = new GuiButton();
            creatureType.addAndFireReloadCallback(field -> field.setPosAndSize(maxGroupField.xPos(), maxGroupField.maxYPos() + 11, 100, 16));
            creatureType.addChild(new GuiLabel("Creature Type").setPosAndSize(creatureType.xPos(), creatureType.yPos() - 8, 100, 8));
            creatureType.setRectColours(0xFF5f5f60, 0xFF5f5f60, 0xFFCCCCCC, 0xFFDDDDDD);
            lb.addChild(creatureType);

            addAllBiomes = new GuiButton("All Biomes").setHoverText("Enable spawning in all biomes");
            addAllBiomes.addAndFireReloadCallback(field -> field.setPosAndSize(creatureType.xPos(), creatureType.maxYPos() + 2, 100, 14));
            addAllBiomes.setVanillaButtonRender(true);
            lb.addChild(addAllBiomes);

            removeAllBiomes = new GuiButton("Clear Biomes").setHoverText("Disable spawning in all biomes");
            removeAllBiomes.addAndFireReloadCallback(field -> field.setPosAndSize(addAllBiomes.xPos(), addAllBiomes.maxYPos() + 1, 100, 14));
            removeAllBiomes.setVanillaButtonRender(true);
            lb.addChild(removeAllBiomes);

            addBomesByType = new GuiButton("Add Biome Type").setHoverText("Enable spawning in all biomes of a specific type");
            addBomesByType.addAndFireReloadCallback(field -> field.setPosAndSize(removeAllBiomes.xPos(), removeAllBiomes.maxYPos() + 1, 100, 14));
            addBomesByType.setVanillaButtonRender(true);
            lb.addChild(addBomesByType);

            removeBomesByType = new GuiButton("Clear Biome Type").setHoverText("Disable spawning in all biomes of a specific type");
            removeBomesByType.addAndFireReloadCallback(field -> field.setPosAndSize(addBomesByType.xPos(), addBomesByType.maxYPos() + 1, 100, 14));
            removeBomesByType.setVanillaButtonRender(true);
            lb.addChild(removeBomesByType);
        }

        private void updateFields() {
            double prevPos = biomeListElement.getVerticalScrollBar().getRawPos();
            biomeListElement.clearElements();
            biomeListElement.resetScrollPositions();

            if (gui.selectedEntity == null || selectedSpawn == -1) {
                return;
            }
            EntityEntry entry = gui.entitySpawnMap.get(gui.selectedEntity);
            if (entry == null || selectedSpawn >= entry.spawns.size()) {
                return;
            }
            EntitySpawn spawn = entry.spawns.get(selectedSpawn);
            Map<ResourceLocation, Biome> biomeMap = spawn.biomeMap;

            for (Biome biome : ForgeRegistries.BIOMES.getValues()) {
                String biomeName = biome.getBiomeName();

                String filter = biomeSearch.getText();
                if (!filter.isEmpty() && !biomeName.toLowerCase().contains(filter.toLowerCase())) {
                    continue;
                }

                GuiButton button = new GuiButton(biomeName);
                button.setToggleMode(true);
                button.setToggleStateSupplier(() -> biomeMap.containsValue(biome));
                button.setTextColGetter((hovering, disabled) -> hovering ? biomeMap.containsValue(biome) ? 0x00AA00 : 0xAA0000 : biomeMap.containsValue(biome) ? 0x00FF00 : 0xFF0000);
                button.setAlignment(GuiAlign.LEFT);
                button.setSize(100, 10).setInsets(0, 1, 0, 1);
                button.setHoverText(TextFormatting.GOLD + "Biome: " + biomeName, "Temperature Category: " + biome.getTempCategory());
                button.setListener(() -> {
                    if (biomeMap.containsValue(biome)) {
                        biomeMap.remove(biome.getRegistryName());
                    }
                    else {
                        biomeMap.put(biome.getRegistryName(), biome);
                    }
                    sendChanges();
                });

                biomeListElement.addElement(button);
            }

            biomeListElement.getVerticalScrollBar().updateRawPos(prevPos);

            //Update Fields
            weightField.setText(String.valueOf(spawn.weight));
            minGroupField.setText(String.valueOf(spawn.minGroupSize));
            maxGroupField.setText(String.valueOf(spawn.maxGroupSize));
            creatureType.setText(spawn.type.name());
            creatureType.setListener(() -> {
                int i;
                for (i = 0; i < EnumCreatureType.values().length; i++) {
                    if (EnumCreatureType.values()[i] == spawn.type) break;
                }
                i++;
                if (i >= EnumCreatureType.values().length) {
                    i = 0;
                }

                spawn.type = EnumCreatureType.values()[i];
                sendChanges();
            });
            weightField.setChangeListener(s -> {
                if (!s.isEmpty()) {
                    spawn.weight = Utils.parseInt(s, true);
                    sendChanges();
                }
            });
            minGroupField.setChangeListener(s -> {
                if (!s.isEmpty()) {
                    spawn.minGroupSize = Utils.parseInt(s, true);
                    sendChanges();
                }
            });
            maxGroupField.setChangeListener(s -> {
                if (!s.isEmpty()) {
                    spawn.maxGroupSize = Utils.parseInt(s, true);
                    sendChanges();
                }
            });
            addAllBiomes.setListener(() -> {
                ForgeRegistries.BIOMES.getEntries().forEach(e -> biomeMap.put(e.getKey(), e.getValue()));
                sendChanges();
            });
            removeAllBiomes.setListener(() -> {
                biomeMap.clear();
                sendChanges();
            });
            addBomesByType.setListener(() -> showTypeSelector(biomes -> {
                biomes.forEach(biome -> {
                    if (biome.getRegistryName() != null) {
                        biomeMap.put(biome.getRegistryName(), biome);
                    }
                });
                sendChanges();
            }));
            removeBomesByType.setListener(() -> showTypeSelector(biomes -> {
                biomes.forEach(biome -> {
                    if (biome.getRegistryName() != null) biomeMap.remove(biome.getRegistryName());
                });
                sendChanges();
            }));
            resetEntitySpawns.setListener(() -> {
                if (gui.selectedEntity != null) {
                    new PacketCustom(SpawnManager.NET_CHANNEL, 3).writeResourceLocation(gui.selectedEntity).writeBoolean(true).sendToServer();
                }
            });
        }

        private void showTypeSelector(Consumer<Set<Biome>> callback) {
            GuiSelectDialog<Type> selector = new GuiSelectDialog<>(this);
            selector.setSize(134, 200).setInsets(1, 1, 12, 1).setCloseOnSelection(true);
            selector.addChild(new GuiBorderedRect().setPosAndSize(selector).setColours(0xFF808080, 0xFF000000));
            selector.setRendererBuilder(type -> {
                MGuiElementBase base = new GuiBorderedRect().setColours(0xFF000000, 0xFF000000, 0xFF707070, 0xFFA0A0A0).setSize(130, 12);
                base.addChild(new GuiLabel(TextFormatting.GOLD + type.getName()).setPosAndSize(base));
                return base;
            });

            GuiTextField filter = new GuiTextField();
            selector.addChild(filter);
            filter.setSize(selector.xSize(), 14).setPos(selector.xPos(), selector.maxYPos() - 12);

            GuiLabel searchLabel = new GuiLabel("Search Types");
            searchLabel.setTextColour(0xA0A0A0).setTrim(false);
            searchLabel.addAndFireReloadCallback(guiLabel -> guiLabel.setPosAndSize(filter).translate(3, 0));
            searchLabel.setEnabledCallback(() -> !filter.isFocused() && filter.getText().isEmpty());
            searchLabel.setAlignment(GuiAlign.LEFT);
            filter.addChild(searchLabel);

            Runnable reload = () -> {
                selector.clearItems();
                String filterText = filter.getText().toLowerCase();
                for (Type type : Type.getAll()) {
                    if (filterText.isEmpty() || type.getName().toLowerCase().contains(filterText)) {
                        selector.addItem(type);
                    }
                }
            };

            reload.run();
            filter.setListener((event1, eventSource1) -> reload.run());

            selector.showCenter();
            selector.getScrollElement().setListSpacing(0).reloadElement();
            selector.setSelectionListener(type -> callback.accept(BiomeDictionary.getBiomes(type)));
        }

        private void sendChanges() {
            sendChanges(false);
        }

        private void sendChanges(boolean requiresReload) {
            PacketCustom packet = new PacketCustom(SpawnManager.NET_CHANNEL, 2);
            packet.writeVarInt(gui.entitySpawnMap.size());
            gui.entitySpawnMap.values().forEach(entry -> entry.serialize(packet));
            packet.writeBoolean(requiresReload);
            packet.sendToServer();
        }

        @Override
        public boolean onUpdate() {
            return super.onUpdate();
        }
    }
}
