package com.golemcraft.golemcraftmod.client.renderer;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.events.ClientEvents;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class LevelerGolemRenderer extends BaseGolemRenderer {

    private static final Identifier[] GOLEM_LOCATIONS = new Identifier[]{
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/leveler_golem.png"),
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/exposed_leveler_golem.png"),
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/weathered_leveler_golem.png"),
            Identifier.fromNamespaceAndPath(GolemCraft.MODID, "textures/entity/oxidized_leveler_golem.png")
    };

    public LevelerGolemRenderer(EntityRendererProvider.Context context) {
        super(context, ClientEvents.LEVELER_GOLEM_LAYER);
        this.addLayer(new FlowerGolemEyesLayer(this));
    }

    @Override
    public Identifier getTextureLocation(BaseGolemRenderState state) {
        return GOLEM_LOCATIONS[state.oxidationLevel];
    }
}
