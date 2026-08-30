package com.golemcraft.golemcraftmod.events;

import java.util.Comparator;
import java.util.List;

import com.golemcraft.golemcraftmod.GolemCraft;
import com.golemcraft.golemcraftmod.entity.ExplorerGolemEntity;
import com.golemcraft.golemcraftmod.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = GolemCraft.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN)) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);

            // Se la struttura è un Iron Golem vanilla (T-shape di ferro), lascia fare a Minecraft vanilla!
            if (isIronGolemStructure(level, pos)) {
                return;
            }

            // Il Base Golem si crea SOLO con 1 Blocco di Ferro e una Zucca Intagliata
            if (belowState.is(Blocks.IRON_BLOCK)) {
                // Destroy blocks
                level.destroyBlock(pos, false);
                level.destroyBlock(belowPos, false);

                // Spawn Base Golem
                com.golemcraft.golemcraftmod.entity.BaseGolemEntity golem = ModEntities.BASE_GOLEM.get().create(level, EntitySpawnReason.EVENT);
                if (golem != null) {
                    golem.setOxidationLevel(0);
                    golem.setWaxed(false);
                    if (event.getEntity() instanceof Player player) {
                        golem.setOwnerUUID(player.getUUID());
                        golem.setYRot(player.getYRot() + 180.0F);
                        golem.yBodyRot = golem.getYRot();
                        golem.yHeadRot = golem.getYRot();
                    }
                    golem.setPos(pos.getX() + 0.5D, belowPos.getY(), pos.getZ() + 0.5D);
                    level.addFreshEntity(golem);
                }
            }
        }
    }

    private static boolean isIronGolemStructure(Level level, BlockPos pumpkinPos) {
        BlockPos center = pumpkinPos.below();
        if (!level.getBlockState(center).is(Blocks.IRON_BLOCK)) {
            return false;
        }
        BlockPos bottom = center.below();
        if (!level.getBlockState(bottom).is(Blocks.IRON_BLOCK)) {
            return false;
        }
        boolean xArms = level.getBlockState(center.east()).is(Blocks.IRON_BLOCK) 
                     && level.getBlockState(center.west()).is(Blocks.IRON_BLOCK);
        boolean zArms = level.getBlockState(center.north()).is(Blocks.IRON_BLOCK) 
                     && level.getBlockState(center.south()).is(Blocks.IRON_BLOCK);
        return xArms || zArms;
    }

    /**
     * When a player dies or drops items on death, trigger nearby ExplorerGolem to collect the drops.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        triggerExplorerGolemCollection(player);
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        triggerExplorerGolemCollection(player);
    }

    private static void triggerExplorerGolemCollection(Player player) {
        Level level = player.level();
        List<ExplorerGolemEntity> golems = level.getEntitiesOfClass(
                ExplorerGolemEntity.class,
                player.getBoundingBox().inflate(48),
                g -> (g.getOwnerUUID() == null || player.getUUID().equals(g.getOwnerUUID())) && !g.isWaiting()
        );
        if (golems.isEmpty()) return;

        ExplorerGolemEntity golem = golems.stream()
                .min(Comparator.comparingDouble(g -> g.distanceToSqr(player)))
                .orElse(null);
        if (golem == null) return;

        if (golem.getOwnerUUID() == null) {
            golem.setOwnerUUID(player.getUUID());
        }

        if (!golem.isCollectingDrops()) {
            golem.startCollectingDeathDrops(player.blockPosition());
        }
    }

    /**
     * All golems have a lightning rod on their head and act as living lightning rods:
     * When lightning strikes within 48 blocks of a sky-exposed golem, it gets attracted directly to the golem!
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LightningBolt lightning && event.getLevel() instanceof ServerLevel serverLevel) {
            net.minecraft.world.phys.Vec3 strikePos = lightning.position();
            net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
                    strikePos.x - 48, strikePos.y - 32, strikePos.z - 48,
                    strikePos.x + 48, strikePos.y + 32, strikePos.z + 48
            );
            List<com.golemcraft.golemcraftmod.entity.BaseGolemEntity> golems = serverLevel.getEntitiesOfClass(
                    com.golemcraft.golemcraftmod.entity.BaseGolemEntity.class,
                    searchBox,
                    g -> g.isAlive() && serverLevel.canSeeSky(g.blockPosition())
            );
            if (!golems.isEmpty()) {
                com.golemcraft.golemcraftmod.entity.BaseGolemEntity nearest = golems.stream()
                        .min(Comparator.comparingDouble(g -> g.distanceToSqr(strikePos)))
                        .orElse(null);
                if (nearest != null) {
                    lightning.setPos(nearest.getX(), nearest.getY(), nearest.getZ());
                }
            }
        }
    }

    /**
     * Remove i-frames (damage invulnerability cooldown) from mobs attacked by Soldier Golem arrows,
     * so rapid fire / charged arrows never get blocked by invulnerability frames!
     */
    @SubscribeEvent
    public static void onProjectileImpact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
                net.minecraft.world.entity.projectile.Projectile proj = event.getProjectile();
                if (proj.getPersistentData().getBooleanOr("GolemArrow", false) || proj.getOwner() instanceof com.golemcraft.golemcraftmod.entity.SoldierGolemEntity) {
                    livingTarget.invulnerableTime = 0;
                    livingTarget.hurtTime = 0;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        net.minecraft.world.entity.Entity direct = source.getDirectEntity();
        net.minecraft.world.entity.Entity attacker = source.getEntity();

        boolean isGolemAttack = (attacker instanceof com.golemcraft.golemcraftmod.entity.SoldierGolemEntity) ||
                                (direct != null && direct.getPersistentData().getBooleanOr("GolemArrow", false)) ||
                                (direct instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() instanceof com.golemcraft.golemcraftmod.entity.SoldierGolemEntity);

        if (isGolemAttack) {
            net.minecraft.world.entity.LivingEntity target = event.getEntity();
            target.invulnerableTime = 0;
            target.hurtTime = 0;
            event.setInvulnerabilityTicks(0);

            if (direct != null && direct.getPersistentData().getBooleanOr("ChargedGolemArrow", false) && target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.5D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.08D);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        net.minecraft.world.entity.Entity direct = source.getDirectEntity();
        net.minecraft.world.entity.Entity attacker = source.getEntity();

        boolean isGolemAttack = (attacker instanceof com.golemcraft.golemcraftmod.entity.SoldierGolemEntity) ||
                                (direct != null && direct.getPersistentData().getBooleanOr("GolemArrow", false)) ||
                                (direct instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() instanceof com.golemcraft.golemcraftmod.entity.SoldierGolemEntity);

        if (isGolemAttack) {
            event.getEntity().invulnerableTime = 0;
            event.getEntity().hurtTime = 0;
        }
    }
}
