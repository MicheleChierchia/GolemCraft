package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class FarmerGolemRenderer extends BaseGolemRenderer {
    private static final Identifier GOLEM_LOCATION = Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/farmer_golem.png");

    public FarmerGolemRenderer(EntityRendererProvider.Context context) {
        super(context, com.golemcraft.golemcraftmod.events.ClientEvents.FARMER_GOLEM_LAYER);
        this.addLayer(new FlowerGolemEyesLayer(this));
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATION;
    }
}
