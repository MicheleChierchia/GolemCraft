package com.golemcraft.golemcraftmod.client.screen;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.block.GolemBeaconMenu;
import com.golemcraft.golemcraftmod.network.GolemBeaconUpdatePacket;
import com.golemcraft.golemcraftmod.registry.ModEffects;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

public class GolemBeaconScreen extends AbstractContainerScreen<GolemBeaconMenu> {
    private static final Identifier BEACON_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/beacon.png");
    private static final Identifier BUTTON_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_disabled");
    private static final Identifier BUTTON_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_selected");
    private static final Identifier BUTTON_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_highlighted");
    private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("container/beacon/button");
    private static final Identifier CONFIRM_SPRITE = Identifier.withDefaultNamespace("container/beacon/confirm");
    private static final Identifier CANCEL_SPRITE = Identifier.withDefaultNamespace("container/beacon/cancel");
    
    private static final Component PRIMARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.primary");
    private static final Component SECONDARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.secondary");
    
    private final List<BeaconButton> beaconButtons = Lists.newArrayList();
    private @Nullable Holder<MobEffect> tier1;
    private @Nullable Holder<MobEffect> tier2;
    private @Nullable Holder<MobEffect> tier3;
    private @Nullable Holder<MobEffect> secondary;

    public GolemBeaconScreen(GolemBeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 219);
    }

    private <T extends AbstractButton & BeaconButton> void addBeaconButton(T button) {
        this.addRenderableWidget(button);
        this.beaconButtons.add(button);
    }

    @Override
    protected void init() {
        super.init();
        this.beaconButtons.clear();

        // Legge gli stati correnti
        this.tier1 = getHolder(menu.getTier1Effect());
        this.tier2 = getHolder(menu.getTier2Effect());
        this.tier3 = getHolder(menu.getTier3Effect());
        this.secondary = getHolder(menu.getSecondaryEffect());

        // Level 1
        this.addBeaconButton(new BeaconPowerButton(this.leftPos + 76 - 12 - 1, this.topPos + 22, MobEffects.REGENERATION, 1));
        this.addBeaconButton(new BeaconPowerButton(this.leftPos + 76 + 12 + 1, this.topPos + 22, MobEffects.HASTE, 1));

        // Level 2
        this.addBeaconButton(new BeaconPowerButton(this.leftPos + 76 - 12 - 1, this.topPos + 47, MobEffects.RESISTANCE, 2));
        this.addBeaconButton(new BeaconPowerButton(this.leftPos + 76 + 12 + 1, this.topPos + 47, MobEffects.STRENGTH, 2));

        // Level 3
        this.addBeaconButton(new BeaconPowerButton(this.leftPos + 76, this.topPos + 72, ModEffects.CHARGE, 3));

        // Secondary Power (Level 4)
        this.addBeaconButton(new BeaconUpgradePowerButton(this.leftPos + 167 - 12 - 1, this.topPos + 47));
        this.addBeaconButton(new BeaconPowerButton(this.leftPos + 167 + 12 + 1, this.topPos + 47, ModEffects.OXIDATION_IMMUNITY, 4));

        this.addBeaconButton(new BeaconConfirmButton(this.leftPos + 164, this.topPos + 107));
        this.addBeaconButton(new BeaconCancelButton(this.leftPos + 190, this.topPos + 107));

        this.updateButtons();
    }

    @Nullable
    private Holder<MobEffect> getHolder(int id) {
        if (id == 0) return null;
        MobEffect ef = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.byId(id);
        return ef != null ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ef) : null;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.updateButtons();
    }

    private void updateButtons() {
        int tier = this.menu.getLevels();
        for (BeaconButton button : this.beaconButtons) {
            button.updateStatus(tier);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.centeredText(this.font, PRIMARY_EFFECT_LABEL, 62, 10, -2039584);
        graphics.centeredText(this.font, SECONDARY_EFFECT_LABEL, 169, 10, -2039584);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BEACON_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        graphics.item(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT), xo + 42 + 22, yo + 109);
    }

    private interface BeaconButton {
        void updateStatus(final int levels);
    }

    private abstract static class BeaconScreenButton extends AbstractButton implements BeaconButton {
        private boolean selected;

        protected BeaconScreenButton(int x, int y) {
            super(x, y, 22, 22, CommonComponents.EMPTY);
        }

        protected void extractBackground(GuiGraphicsExtractor graphics) {
            Identifier sprite = !this.active ? BUTTON_DISABLED_SPRITE : (this.selected ? BUTTON_SELECTED_SPRITE : (this.isHovered() ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mx, int my, float pt) {
            this.extractBackground(graphics);
            this.extractIcon(graphics);
        }

        protected abstract void extractIcon(GuiGraphicsExtractor graphics);

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class BeaconPowerButton extends BeaconScreenButton {
        protected final int tier;
        protected Holder<MobEffect> effect;
        protected Identifier sprite;

        public BeaconPowerButton(int x, int y, Holder<MobEffect> effect, int tier) {
            super(x, y);
            this.tier = tier;
            this.setEffect(effect);
        }

        protected void setEffect(Holder<MobEffect> effect) {
            this.effect = effect;
            if (effect.unwrapKey().isPresent()) {
                Identifier id = effect.unwrapKey().get().identifier();
                this.sprite = Identifier.fromNamespaceAndPath(id.getNamespace(), "mob_effect/" + id.getPath());
            } else {
                this.sprite = Identifier.fromNamespaceAndPath("minecraft", "mob_effect/regeneration");
            }
            this.setTooltip(Tooltip.create(Component.translatable(effect.value().getDescriptionId()), null));
        }

        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            if (!this.isSelected()) {
                if (this.tier == 1) GolemBeaconScreen.this.tier1 = this.effect;
                else if (this.tier == 2) GolemBeaconScreen.this.tier2 = this.effect;
                else if (this.tier == 3) GolemBeaconScreen.this.tier3 = this.effect;
                else if (this.tier == 4) GolemBeaconScreen.this.secondary = this.effect;
                GolemBeaconScreen.this.updateButtons();
            }
        }

        @Override
        protected void extractIcon(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX() + 2, this.getY() + 2, 18, 18);
        }

        @Override
        public void updateStatus(int levels) {
            this.active = this.tier <= levels;
            Holder<MobEffect> current = switch(this.tier) {
                case 1 -> GolemBeaconScreen.this.tier1;
                case 2 -> GolemBeaconScreen.this.tier2;
                case 3 -> GolemBeaconScreen.this.tier3;
                default -> GolemBeaconScreen.this.secondary;
            };
            this.setSelected(this.effect != null && this.effect.equals(current));
        }
    }

    private class BeaconUpgradePowerButton extends BeaconPowerButton {
        public BeaconUpgradePowerButton(int x, int y) {
            super(x, y, MobEffects.REGENERATION, 4);
        }

        @Override
        public void updateStatus(int levels) {
            // Find highest primary tier selected
            Holder<MobEffect> highestPrimary = GolemBeaconScreen.this.tier3 != null ? GolemBeaconScreen.this.tier3 : 
                    (GolemBeaconScreen.this.tier2 != null ? GolemBeaconScreen.this.tier2 : GolemBeaconScreen.this.tier1);
            
            if (highestPrimary != null) {
                this.visible = true;
                this.setEffect(highestPrimary);
                super.updateStatus(levels);
            } else {
                this.visible = false;
            }
        }
    }

    private class BeaconConfirmButton extends BeaconScreenButton {
        public BeaconConfirmButton(int x, int y) {
            super(x, y);
            this.setTooltip(Tooltip.create(CommonComponents.GUI_DONE, null));
        }

        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            int t1Id = GolemBeaconScreen.this.tier1 != null ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(GolemBeaconScreen.this.tier1.value()) : 0;
            int t2Id = GolemBeaconScreen.this.tier2 != null ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(GolemBeaconScreen.this.tier2.value()) : 0;
            int t3Id = GolemBeaconScreen.this.tier3 != null ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(GolemBeaconScreen.this.tier3.value()) : 0;
            int secId = GolemBeaconScreen.this.secondary != null ? net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(GolemBeaconScreen.this.secondary.value()) : 0;
            
            ClientPacketDistributor.sendToServer(new GolemBeaconUpdatePacket(
                    GolemBeaconScreen.this.menu.getBeaconPos(), t1Id, t2Id, t3Id, secId));
            GolemBeaconScreen.this.onClose();
        }

        @Override
        protected void extractIcon(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CONFIRM_SPRITE, this.getX() + 2, this.getY() + 2, 18, 18);
        }

        @Override
        public void updateStatus(int levels) {
            boolean hasAnyBuff = GolemBeaconScreen.this.tier1 != null || GolemBeaconScreen.this.tier2 != null || GolemBeaconScreen.this.tier3 != null || GolemBeaconScreen.this.secondary != null;
            this.active = hasAnyBuff && GolemBeaconScreen.this.menu.hasPayment();
        }
    }

    private class BeaconCancelButton extends BeaconScreenButton {
        public BeaconCancelButton(int x, int y) {
            super(x, y);
            this.setTooltip(Tooltip.create(CommonComponents.GUI_CANCEL, null));
        }

        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            GolemBeaconScreen.this.onClose();
        }

        @Override
        protected void extractIcon(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CANCEL_SPRITE, this.getX() + 2, this.getY() + 2, 18, 18);
        }

        @Override
        public void updateStatus(int levels) {
        }
    }
}
