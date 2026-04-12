package com.bebub.genderbub.client;

import com.bebub.genderbub.config.GenderConfig;
import com.bebub.genderbub.network.EnabledMobsSyncPacket;
import com.bebub.genderbub.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class GenderConfigScreen extends Screen {
    private static final ResourceLocation BLACKSTONE = new ResourceLocation("textures/block/blackstone.png");
    
    private EditBox maleChanceBox;
    private EditBox femaleChanceBox;
    private EditBox displayRadiusBox;
    
    private Button allowMaleMaleBreedButton;
    private Button allowFemaleFemaleBreedButton;
    private Button allowSterileBreedButton;
    private Button enableVillagersButton;
    private Button keepVillagerGenderButton;
    
    private boolean currentAllowMaleMaleBreed;
    private boolean currentAllowFemaleFemaleBreed;
    private boolean currentAllowSterileBreed;
    private boolean currentEnableVillagers;
    private boolean currentKeepVillagerGender;
    
    private Button saveButton;
    private Button cancelButton;
    private Button resetButton;

    private int originalMaleChance;
    private int originalFemaleChance;
    private int originalDisplayRadius;
    private boolean originalAllowMaleMaleBreed;
    private boolean originalAllowFemaleFemaleBreed;
    private boolean originalAllowSterileBreed;
    private boolean originalEnableVillagers;
    private boolean originalKeepVillagerGender;
    
    private int defaultMaleChance = 45;
    private int defaultFemaleChance = 45;
    private int defaultDisplayRadius = 24;
    private boolean defaultAllowMaleMaleBreed = false;
    private boolean defaultAllowFemaleFemaleBreed = false;
    private boolean defaultAllowSterileBreed = false;
    private boolean defaultEnableVillagers = true;
    private boolean defaultKeepVillagerGender = true;

    public GenderConfigScreen() {
        super(Component.translatable("genderbub.config.title"));
        
        originalMaleChance = GenderConfig.getMaleChance();
        originalFemaleChance = GenderConfig.getFemaleChance();
        originalDisplayRadius = GenderConfig.getDisplayRadius();
        originalAllowMaleMaleBreed = GenderConfig.isAllowMaleMaleBreed();
        originalAllowFemaleFemaleBreed = GenderConfig.isAllowFemaleFemaleBreed();
        originalAllowSterileBreed = GenderConfig.isAllowSterileBreed();
        originalEnableVillagers = GenderConfig.isEnableVillagers();
        originalKeepVillagerGender = GenderConfig.isKeepVillagerGender();
        
        currentAllowMaleMaleBreed = originalAllowMaleMaleBreed;
        currentAllowFemaleFemaleBreed = originalAllowFemaleFemaleBreed;
        currentAllowSterileBreed = originalAllowSterileBreed;
        currentEnableVillagers = originalEnableVillagers;
        currentKeepVillagerGender = originalKeepVillagerGender;
    }

    @Override
    protected void init() {
        super.init();
        
        int leftX = 20;
        int startY = 30;
        int fieldWidth = 80;
        int buttonWidth = 80;
        int rowHeight = 30;
        int actionButtonWidth = 90;
        
        int actionButtonY = startY;
        this.saveButton = Button.builder(Component.translatable("genderbub.config.save"), this::saveChanges)
                .bounds(leftX, actionButtonY, actionButtonWidth, 20)
                .build();
        this.saveButton.active = true;
        this.addRenderableWidget(this.saveButton);
        
        this.resetButton = Button.builder(Component.translatable("genderbub.config.reset"), this::resetToDefault)
                .bounds(leftX, actionButtonY + 30, actionButtonWidth, 20)
                .build();
        this.addRenderableWidget(this.resetButton);
        
        this.cancelButton = Button.builder(Component.translatable("genderbub.config.cancel"), this::cancelChanges)
                .bounds(leftX, actionButtonY + 60, actionButtonWidth, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);
        
        int rightX = leftX + actionButtonWidth + 20;
        int fieldX = rightX;
        int textX = fieldX + fieldWidth + 10;
        
        this.maleChanceBox = new EditBox(this.font, fieldX, startY, fieldWidth, 20, Component.literal(""));
        this.maleChanceBox.setValue(String.valueOf(originalMaleChance));
        this.maleChanceBox.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(this.maleChanceBox);
        
        this.femaleChanceBox = new EditBox(this.font, fieldX, startY + rowHeight, fieldWidth, 20, Component.literal(""));
        this.femaleChanceBox.setValue(String.valueOf(originalFemaleChance));
        this.femaleChanceBox.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(this.femaleChanceBox);
        
        this.displayRadiusBox = new EditBox(this.font, fieldX, startY + rowHeight * 2, fieldWidth, 20, Component.literal(""));
        this.displayRadiusBox.setValue(String.valueOf(originalDisplayRadius));
        this.displayRadiusBox.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(this.displayRadiusBox);
        
        this.allowMaleMaleBreedButton = Button.builder(getBoolText(currentAllowMaleMaleBreed), this::toggleAllowMaleMaleBreed)
                .bounds(fieldX, startY + rowHeight * 3, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.allowMaleMaleBreedButton);
        
        this.allowFemaleFemaleBreedButton = Button.builder(getBoolText(currentAllowFemaleFemaleBreed), this::toggleAllowFemaleFemaleBreed)
                .bounds(fieldX, startY + rowHeight * 4, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.allowFemaleFemaleBreedButton);
        
        this.allowSterileBreedButton = Button.builder(getBoolText(currentAllowSterileBreed), this::toggleAllowSterileBreed)
                .bounds(fieldX, startY + rowHeight * 5, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.allowSterileBreedButton);
        
        this.enableVillagersButton = Button.builder(getBoolText(currentEnableVillagers), this::toggleEnableVillagers)
                .bounds(fieldX, startY + rowHeight * 6, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.enableVillagersButton);
        
        this.keepVillagerGenderButton = Button.builder(getBoolText(currentKeepVillagerGender), this::toggleKeepVillagerGender)
                .bounds(fieldX, startY + rowHeight * 7, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.keepVillagerGenderButton);
    }
    
    private MutableComponent getBoolText(boolean value) {
        if (value) {
            return Component.literal("TRUE").withStyle(ChatFormatting.GREEN);
        } else {
            return Component.literal("FALSE").withStyle(ChatFormatting.RED);
        }
    }
    
    private void toggleAllowMaleMaleBreed(Button button) {
        currentAllowMaleMaleBreed = !currentAllowMaleMaleBreed;
        button.setMessage(getBoolText(currentAllowMaleMaleBreed));
    }
    
    private void toggleAllowFemaleFemaleBreed(Button button) {
        currentAllowFemaleFemaleBreed = !currentAllowFemaleFemaleBreed;
        button.setMessage(getBoolText(currentAllowFemaleFemaleBreed));
    }
    
    private void toggleAllowSterileBreed(Button button) {
        currentAllowSterileBreed = !currentAllowSterileBreed;
        button.setMessage(getBoolText(currentAllowSterileBreed));
    }
    
    private void toggleEnableVillagers(Button button) {
        currentEnableVillagers = !currentEnableVillagers;
        button.setMessage(getBoolText(currentEnableVillagers));
    }
    
    private void toggleKeepVillagerGender(Button button) {
        currentKeepVillagerGender = !currentKeepVillagerGender;
        button.setMessage(getBoolText(currentKeepVillagerGender));
    }
    
    private void resetToDefault(Button button) {
        this.maleChanceBox.setValue(String.valueOf(defaultMaleChance));
        this.femaleChanceBox.setValue(String.valueOf(defaultFemaleChance));
        this.displayRadiusBox.setValue(String.valueOf(defaultDisplayRadius));
        
        currentAllowMaleMaleBreed = defaultAllowMaleMaleBreed;
        currentAllowFemaleFemaleBreed = defaultAllowFemaleFemaleBreed;
        currentAllowSterileBreed = defaultAllowSterileBreed;
        currentEnableVillagers = defaultEnableVillagers;
        currentKeepVillagerGender = defaultKeepVillagerGender;
        
        this.allowMaleMaleBreedButton.setMessage(getBoolText(currentAllowMaleMaleBreed));
        this.allowFemaleFemaleBreedButton.setMessage(getBoolText(currentAllowFemaleFemaleBreed));
        this.allowSterileBreedButton.setMessage(getBoolText(currentAllowSterileBreed));
        this.enableVillagersButton.setMessage(getBoolText(currentEnableVillagers));
        this.keepVillagerGenderButton.setMessage(getBoolText(currentKeepVillagerGender));
        
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.translatable("genderbub.config.reset_default_msg"), true);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (int x = 0; x < this.width; x += 16) {
            for (int y = 0; y < this.height; y += 16) {
                guiGraphics.setColor(0.4f, 0.4f, 0.4f, 1.0f);
                guiGraphics.blit(BLACKSTONE, x, y, 0, 0, 16, 16, 16, 16);
            }
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int leftX = 20;
        int startY = 30;
        int fieldWidth = 80;
        int rowHeight = 30;
        int actionButtonWidth = 90;
        
        int rightX = leftX + actionButtonWidth + 20;
        int textX = rightX + fieldWidth + 10;
        
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.maleChance"), textX, startY + 6, 0x55AAFF, false);
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.femaleChance"), textX, startY + 6 + rowHeight, 0xFF55FF, false);
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.displayRadius"), textX, startY + 6 + rowHeight * 2, 0xFFAA00, false);
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.allowMaleMaleBreed"), textX, startY + 6 + rowHeight * 3, 0x55AAFF, false);
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.allowFemaleFemaleBreed"), textX, startY + 6 + rowHeight * 4, 0xFF55FF, false);
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.allowSterileBreed"), textX, startY + 6 + rowHeight * 5, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.enableVillagers"), textX, startY + 6 + rowHeight * 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("genderbub.config.keepVillagerGender"), textX, startY + 6 + rowHeight * 7, 0xFFFFFF, false);
    }

    private void saveChanges(Button button) {
        try {
            int maleChance = Integer.parseInt(this.maleChanceBox.getValue());
            int femaleChance = Integer.parseInt(this.femaleChanceBox.getValue());
            int displayRadius = Integer.parseInt(this.displayRadiusBox.getValue());
            
            if (maleChance < 0) maleChance = 0;
            if (maleChance > 50) maleChance = 50;
            if (femaleChance < 0) femaleChance = 0;
            if (femaleChance > 50) femaleChance = 50;
            if (displayRadius < 0) displayRadius = 0;
            if (displayRadius > 256) displayRadius = 256;
            
            GenderConfig.setMaleChance(maleChance);
            GenderConfig.setFemaleChance(femaleChance);
            GenderConfig.setDisplayRadius(displayRadius);
            GenderConfig.setAllowMaleMaleBreed(currentAllowMaleMaleBreed);
            GenderConfig.setAllowFemaleFemaleBreed(currentAllowFemaleFemaleBreed);
            GenderConfig.setAllowSterileBreed(currentAllowSterileBreed);
            GenderConfig.setEnableVillagers(currentEnableVillagers);
            GenderConfig.setKeepVillagerGender(currentKeepVillagerGender);
            
            GenderConfig.save();
            GenderConfig.reload();
            
            Set<String> enabledMobs = new HashSet<>(GenderConfig.getEnabledMobs());
            if (this.minecraft != null && this.minecraft.getConnection() != null) {
                NetworkHandler.CHANNEL.send(
                    PacketDistributor.SERVER.noArg(),
                    new EnabledMobsSyncPacket(enabledMobs)
                );
            }
            
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.translatable("genderbub.config.saved"), true);
            }
            
            returnToModListScreen();
        } catch (NumberFormatException e) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.translatable("genderbub.config.invalid_number"), true);
            }
        }
    }

    private void cancelChanges(Button button) {
        returnToModListScreen();
    }
    
    private void returnToModListScreen() {
        if (this.minecraft != null) {
            Screen modListScreen = new ModListScreen(this);
            this.minecraft.setScreen(modListScreen);
        } else {
            this.onClose();
        }
    }
}