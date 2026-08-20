package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.FlowerGolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class FlowerGolemRenderer extends BaseGolemRenderer {
    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/flower_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_flower_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_flower_golem.png"),
        Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_flower_golem.png")
    };

    public FlowerGolemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new FlowerGolemEyesLayer(this));
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
