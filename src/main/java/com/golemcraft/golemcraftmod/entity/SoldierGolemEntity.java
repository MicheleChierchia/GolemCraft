package com.golemcraft.golemcraftmod.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.particles.ParticleTypes;

public class SoldierGolemEntity extends BaseGolemEntity implements RangedAttackMob, CrossbowAttackMob {

    // ── Synced attack animation ────────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(SoldierGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_GUARDING =
            SynchedEntityData.defineId(SoldierGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private BlockPos guardPos = null;

    // ── Combat goals (swapped based on equipped weapon) ────────────────────────
    private final RangedBowAttackGoal<SoldierGolemEntity>      bowGoal      = new RangedBowAttackGoal<>(this, 1.0D, 40, 20.0F) {
        @Override
        public void tick() {
            super.tick();
            if (SoldierGolemEntity.this.isGuarding()) {
                SoldierGolemEntity.this.getNavigation().stop();
                LivingEntity target = SoldierGolemEntity.this.getTarget();
                if (target != null && SoldierGolemEntity.this.getGuardPos() != null) {
                    double distFromCenterSq = SoldierGolemEntity.this.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(SoldierGolemEntity.this.getGuardPos()));
                    if (distFromCenterSq < 0.5D) {
                        SoldierGolemEntity.this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 0.5D);
                    } else if (distFromCenterSq > 0.64D) {
                        SoldierGolemEntity.this.getMoveControl().setWantedPosition(
                            SoldierGolemEntity.this.getGuardPos().getX() + 0.5D,
                            SoldierGolemEntity.this.getGuardPos().getY(),
                            SoldierGolemEntity.this.getGuardPos().getZ() + 0.5D,
                            0.5D
                        );
                    } else {
                        // Cancella lo strafing generato da super.tick() che causava la rotazione continua!
                        SoldierGolemEntity.this.getMoveControl().strafe(0.0F, 0.0F);
                    }
                    SoldierGolemEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
            } else {
                LivingEntity target = SoldierGolemEntity.this.getTarget();
                if (target != null && SoldierGolemEntity.this.distanceToSqr(target) < 100.0D) { // 10 blocks
                    SoldierGolemEntity.this.getNavigation().stop();
                }
            }
        }
    };
    private final RangedCrossbowAttackGoal<SoldierGolemEntity> crossbowGoal = new RangedCrossbowAttackGoal<>(this, 1.0D, 20.0F) {
        @Override
        public void tick() {
            super.tick();
            if (SoldierGolemEntity.this.isGuarding()) {
                SoldierGolemEntity.this.getNavigation().stop();
                LivingEntity target = SoldierGolemEntity.this.getTarget();
                if (target != null && SoldierGolemEntity.this.getGuardPos() != null) {
                    double distFromCenterSq = SoldierGolemEntity.this.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(SoldierGolemEntity.this.getGuardPos()));
                    if (distFromCenterSq < 0.5D) {
                        SoldierGolemEntity.this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 0.5D);
                    } else if (distFromCenterSq > 0.64D) {
                        SoldierGolemEntity.this.getMoveControl().setWantedPosition(
                            SoldierGolemEntity.this.getGuardPos().getX() + 0.5D,
                            SoldierGolemEntity.this.getGuardPos().getY(),
                            SoldierGolemEntity.this.getGuardPos().getZ() + 0.5D,
                            0.5D
                        );
                    } else {
                        SoldierGolemEntity.this.getMoveControl().strafe(0.0F, 0.0F);
                    }
                    SoldierGolemEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
            } else {
                LivingEntity target = SoldierGolemEntity.this.getTarget();
                if (target != null && SoldierGolemEntity.this.distanceToSqr(target) < 100.0D) {
                    SoldierGolemEntity.this.getNavigation().stop();
                }
            }
        }
    };
    private final MeleeAttackGoal                              meleeGoal    = new MeleeAttackGoal(this, 1.2D, true);

    private enum WeaponMode { NONE, MELEE, BOW, CROSSBOW }
    private WeaponMode currentWeaponMode = null;
    private int weaponSwapCooldown = 0;

    public SoldierGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    // ── SynchedData ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_ANIM_TICKS, 0);
        builder.define(IS_GUARDING, false);
    }

    public int getAttackAnimTicks() { return this.entityData.get(ATTACK_ANIM_TICKS); }
    public void setAttackAnimTicks(int t) { this.entityData.set(ATTACK_ANIM_TICKS, t); }

    public boolean isGuarding() { return this.entityData.get(IS_GUARDING); }
    public void setGuarding(boolean guarding) { this.entityData.set(IS_GUARDING, guarding); }

    public BlockPos getGuardPos() { return this.guardPos; }
    public void setGuardPos(BlockPos pos) { this.guardPos = pos; }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("IsGuarding", com.mojang.serialization.Codec.BOOL, this.isGuarding());
        if (this.guardPos != null) {
            output.store("GuardPos", BlockPos.CODEC, this.guardPos);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("IsGuarding", com.mojang.serialization.Codec.BOOL).ifPresent(this::setGuarding);
        input.read("GuardPos", BlockPos.CODEC).ifPresent(this::setGuardPos);
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return BaseGolemEntity.createAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.0D)   // base unarmed damage = nearly 0
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new ReturnToGuardPositionGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.4D, 10.0F, 3.0F));
        // Priority 3: combat goal — added dynamically by updateAttackGoals()
        this.goalSelector.addGoal(4, new com.golemcraft.golemcraftmod.entity.ai.EquipWeaponGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                if (isGuarding() && !hasMeleeWeapon()) return false;
                return super.canUse();
            }
            @Override
            public boolean canContinueToUse() {
                if (isGuarding() && !hasMeleeWeapon()) return false;
                return super.canContinueToUse();
            }
        });

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NearOwnerEnemyGoal(this, 20.0D)); // Range incrementato a 20.0D (era 10.0D)
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (weaponSwapCooldown > 0) weaponSwapCooldown--;

            // Countdown attack animation
            int anim = getAttackAnimTicks();
            if (anim > 0) setAttackAnimTicks(anim - 1);

            // Weapon-only hand sync: only equip from slot 0 if it's a weapon
            syncWeaponToHand();
            // Swap combat goal based on current weapon
            updateAttackGoals();
            
            // "Taunt" continuo: forza il nostro bersaglio a combattere contro di noi
            forceTargetAggro();
        }
    }

    /**
     * Tank mechanic: if we are targeting a Mob, we force it to target us back constantly.
     * This prevents vanilla AI (like NearestAttackableTargetGoal for players) from ignoring the golem.
     */
    private void forceTargetAggro() {
        LivingEntity currentTarget = this.getTarget();
        if (currentTarget instanceof Mob mobTarget && mobTarget.getTarget() != this) {
            mobTarget.setTarget(this);
            mobTarget.setLastHurtByMob(this);
            // Support for newer mobs that use Brains (like Piglins)
            if (mobTarget.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)) {
                mobTarget.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET, this);
            }
        }
    }

    /**
     * The weapon lives in the equipment slot (not inventory), just like the Farmer's hoe.
     * If the player puts a new weapon in inventory slot 0 (to re-arm the golem),
     * we move it to the hand and clear the slot. Otherwise, leave the hand untouched.
     */
    private void syncWeaponToHand() {
        ItemStack slot0 = this.getInventory().getItem(0);
        if (!slot0.isEmpty() && isWeapon(slot0)) {
            // Player dropped a new weapon into slot 0 → equip it
            this.setItemInHand(InteractionHand.MAIN_HAND, slot0.copy());
            this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
            this.getInventory().setItem(0, ItemStack.EMPTY);
        }
        // If slot 0 is empty → weapon stays in equipment slot, hand is untouched
    }

    /** Returns true if the given stack counts as a weapon the soldier can use. */
    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ItemTags.SWORDS)) return true;
        if (stack.is(Items.BOW))      return true;
        if (stack.is(Items.CROSSBOW)) return true;
        if (stack.is(Items.TRIDENT))  return true;
        if (stack.getItem() instanceof AxeItem) return true;
        // Modded swords/weapons by name convention
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("sword") || path.contains("blade") || path.contains("saber");
    }

    public static boolean isArrow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ItemTags.ARROWS) || stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW);
    }

    private boolean hasArrows() {
        if (isArrow(this.getItemInHand(InteractionHand.OFF_HAND))) return true;
        net.minecraft.world.SimpleContainer inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isArrow(inv.getItem(i))) return true;
        }
        return false;
    }

    private void swapToMeleeWeaponIfAvailable() {
        net.minecraft.world.SimpleContainer inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && isWeapon(stack) && !stack.is(Items.BOW) && !stack.is(Items.CROSSBOW)) {
                ItemStack current = this.getItemInHand(InteractionHand.MAIN_HAND);
                this.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                inv.setItem(i, current.copy());
                return;
            }
        }
    }

    private void swapToRangedWeaponIfAvailable() {
        net.minecraft.world.SimpleContainer inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.BOW) || stack.is(Items.CROSSBOW))) {
                ItemStack current = this.getItemInHand(InteractionHand.MAIN_HAND);
                this.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                inv.setItem(i, current.copy());
                return;
            }
        }
    }

    private void equipBestWeaponFromInventory() {
        net.minecraft.world.SimpleContainer inv = this.getInventory();
        int bestMeleeIndex = -1;
        int bestRangedIndex = -1;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && isWeapon(stack)) {
                if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
                    if (bestRangedIndex == -1) bestRangedIndex = i;
                } else {
                    if (bestMeleeIndex == -1) bestMeleeIndex = i;
                }
            }
        }

        int targetIndex = -1;
        if (hasArrows() && bestRangedIndex != -1) {
            targetIndex = bestRangedIndex;
        } else if (bestMeleeIndex != -1) {
            targetIndex = bestMeleeIndex;
        } else if (!hasArrows() && bestRangedIndex != -1) {
            targetIndex = bestRangedIndex;
        }

        if (targetIndex != -1) {
            ItemStack stack = inv.getItem(targetIndex);
            ItemStack current = this.getItemInHand(InteractionHand.MAIN_HAND);
            this.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            inv.setItem(targetIndex, current.copy());
        }
    }

    public boolean hasMeleeWeapon() {
        ItemStack hand = this.getItemInHand(InteractionHand.MAIN_HAND);
        if (!hand.isEmpty() && isWeapon(hand) && !hand.is(Items.BOW) && !hand.is(Items.CROSSBOW)) {
            return true;
        }
        net.minecraft.world.SimpleContainer inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && isWeapon(stack) && !stack.is(Items.BOW) && !stack.is(Items.CROSSBOW)) {
                return true;
            }
        }
        return false;
    }



    /**
     * Swaps the active combat goal only when the weapon type actually changes.
     * When no weapon is equipped, ALL combat goals are removed so the golem cannot attack.
     */
    private void updateAttackGoals() {
        ItemStack item = this.getItemInHand(InteractionHand.MAIN_HAND);

        // Se non abbiamo armi, cerchiamo di pescarne una dall'inventario
        if (item.isEmpty()) {
            equipBestWeaponFromInventory();
            item = this.getItemInHand(InteractionHand.MAIN_HAND);
        }

        // Calcolo della distanza dal target
        LivingEntity target = this.getTarget();
        boolean preferRanged = false;
        boolean preferMelee = false;

        if (target != null && target.isAlive()) {
            double distSq = this.distanceToSqr(target);
            if (distSq > 36.0D) {
                preferRanged = true;
            } else {
                preferMelee = true;
            }
        }

        if (weaponSwapCooldown <= 0) {
            if (preferRanged && hasArrows()) {
                if (!item.is(Items.BOW) && !item.is(Items.CROSSBOW)) {
                    swapToRangedWeaponIfAvailable();
                    ItemStack newItem = this.getItemInHand(InteractionHand.MAIN_HAND);
                    if (newItem != item) {
                        item = newItem;
                        weaponSwapCooldown = 20;
                    }
                }
            } else if (preferMelee || !hasArrows()) {
                if (item.is(Items.BOW) || item.is(Items.CROSSBOW)) {
                    swapToMeleeWeaponIfAvailable();
                    ItemStack newItem = this.getItemInHand(InteractionHand.MAIN_HAND);
                    if (newItem != item) {
                        item = newItem;
                        weaponSwapCooldown = 20;
                    }
                }
            }
        }

        WeaponMode desired;
        if (item.isEmpty() || !isWeapon(item)) {
            desired = WeaponMode.NONE;
        } else if (item.is(Items.BOW)) {
            desired = hasArrows() ? WeaponMode.BOW : WeaponMode.NONE;
        } else if (item.is(Items.CROSSBOW)) {
            desired = hasArrows() ? WeaponMode.CROSSBOW : WeaponMode.NONE;
        } else {
            desired = WeaponMode.MELEE;
        }

        if (desired == currentWeaponMode) return; // nothing changed

        this.goalSelector.removeGoal(this.meleeGoal);
        this.goalSelector.removeGoal(this.bowGoal);
        this.goalSelector.removeGoal(this.crossbowGoal);

        if (desired != WeaponMode.NONE) {
            switch (desired) {
                case BOW      -> this.goalSelector.addGoal(3, this.bowGoal);
                case CROSSBOW -> this.goalSelector.addGoal(3, this.crossbowGoal);
                default       -> this.goalSelector.addGoal(3, this.meleeGoal);
            }
        }
        // When NONE: no combat goal added → golem won't attack
        currentWeaponMode = desired;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level,
                                 net.minecraft.world.entity.Entity target) {
        // Refuse to attack if no weapon equipped
        if (!isWeapon(this.getItemInHand(InteractionHand.MAIN_HAND))) return false;

        setAttackAnimTicks(10); // trigger client-side animation

        boolean success = super.doHurtTarget(level, target);

        if (success) {
            // Force the attacked mob to target the golem back
            if (target instanceof Mob mobTarget) {
                mobTarget.setTarget(this);
            }

            // Consume 1 durability per hit, just like a player would
            ItemStack weapon = this.getItemInHand(InteractionHand.MAIN_HAND);
            if (!weapon.isEmpty() && weapon.isDamageableItem()) {
                weapon.hurtAndBreak(1, level, null, item -> {
                    // Weapon broke — empty the hand and play break sound
                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    this.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
                });
            }
        }

        return success;
    }

    // ── Ranged Attacks ────────────────────────────────────────────────────────

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        setAttackAnimTicks(10);
        
        GolemFakePlayerHelper.executeAsPlayer(this, player -> {
            net.minecraft.world.item.ItemStack weapon = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (weapon.getItem() instanceof net.minecraft.world.item.BowItem bowItem) {
                int timeLeft = 72000 - (int)(pullProgress * 20.0f);
                
                double dx = target.getX() - player.getX();
                double dy = target.getY(0.3333333333333333D) - player.getEyeY();
                double dz = target.getZ() - player.getZ();
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                dy += horizDist * 0.20000000298023224D;
                
                player.setXRot((float)(-(net.minecraft.util.Mth.atan2(dy, horizDist) * (180F / (float)Math.PI))));
                player.setYRot((float)(net.minecraft.util.Mth.atan2(dz, dx) * (180F / (float)Math.PI)) - 90.0F);
                
                bowItem.releaseUsing(weapon, player.level(), player, timeLeft);
            }
        });
    }

    @Override
    public void setChargingCrossbow(boolean isCharging) { this.setRummaging(isCharging); }

    private void consumeArrowForCrossbow() {
        ItemStack offhand = this.getItemInHand(InteractionHand.OFF_HAND);
        if (isArrow(offhand)) {
            offhand.shrink(1);
            if (offhand.isEmpty()) this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            return;
        }
        net.minecraft.world.SimpleContainer inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isArrow(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    public void shootCrossbowProjectile(LivingEntity target, ItemStack crossbow,
                                         Projectile projectile, float angle) {
        consumeArrowForCrossbow(); // Consuma la freccia vera!

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - projectile.getY();
        double dz = target.getZ() - this.getZ();
        double h  = Math.sqrt(dx * dx + dz * dz);
        projectile.shoot(dx, dy + h * 0.20000000298023224D, dz, 1.6F, 1.5F);
        this.level().addFreshEntity(projectile);
        
        if (!crossbow.isEmpty() && crossbow.isDamageableItem()) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                crossbow.hurtAndBreak(1, serverLevel, null, item -> {
                    this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    this.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
                });
            }
        }
    }

    public void onCrossbowAttackPerformed() { this.noActionTime = 0; }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Player getOwnerPlayer() {
        if (this.getOwnerUUID() == null) return null;
        return this.level().getPlayerByUUID(this.getOwnerUUID());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Prima lascia che BaseGolemEntity gestisca armi, asce (cera/ossidazione), miele ecc.
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) {
            return result;
        }

        // Se il click non è stato consumato, e la mano è vuota, alterniamo la modalità
        ItemStack stackInHand = player.getItemInHand(hand);
        if (stackInHand.isEmpty() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(player.getUUID())) {
            if (!this.level().isClientSide()) {
                boolean nextState = !this.isGuarding();
                this.setGuarding(nextState);
                if (nextState) {
                    this.setGuardPos(this.blockPosition());
                    this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.0F); // suono metallico rassicurante
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
                    }
                } else {
                    this.setGuardPos(null);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // =========================================================================
    //  Guard Goal
    // =========================================================================

    static class ReturnToGuardPositionGoal extends Goal {
        private final SoldierGolemEntity golem;
        private final double speed;

        ReturnToGuardPositionGoal(SoldierGolemEntity golem, double speed) {
            this.golem = golem;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!golem.isGuarding() || golem.getGuardPos() == null) return false;
            LivingEntity t = golem.getTarget();
            if (t != null && t.isAlive()) return false; // in combat
            return golem.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(golem.getGuardPos())) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            if (!golem.isGuarding() || golem.getGuardPos() == null) return false;
            LivingEntity t = golem.getTarget();
            if (t != null && t.isAlive()) return false;
            return !golem.getNavigation().isDone() && golem.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(golem.getGuardPos())) > 2.25D;
        }

        @Override
        public void start() {
            if (golem.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(golem.getGuardPos())) > 144.0D) {
                // Teletrasporto se caduto o spinto troppo lontano
                golem.teleportTo(golem.getGuardPos().getX() + 0.5D, golem.getGuardPos().getY(), golem.getGuardPos().getZ() + 0.5D);
                golem.getNavigation().stop();
            } else {
                golem.getNavigation().moveTo(golem.getGuardPos().getX() + 0.5D, golem.getGuardPos().getY(), golem.getGuardPos().getZ() + 0.5D, speed);
            }
        }
    }

    // =========================================================================
    //  Follow Owner Goal — wolf-like behaviour
    // =========================================================================

    static class FollowOwnerGoal extends Goal {
        private final SoldierGolemEntity golem;
        private final double             speed;
        private final float              startDistSq;
        private final float              stopDistSq;
        private Player owner;
        private int    recalcTimer;

        FollowOwnerGoal(SoldierGolemEntity golem, double speed, float startDist, float stopDist) {
            this.golem       = golem;
            this.speed       = speed;
            this.startDistSq = startDist * startDist;
            this.stopDistSq  = stopDist  * stopDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.isGuarding()) return false;
            owner = golem.getOwnerPlayer();
            if (owner == null) return false;
            LivingEntity t = golem.getTarget();
            if (t != null && t.isAlive()) return false; // in combat
            return golem.distanceToSqr(owner) >= startDistSq;
        }

        @Override
        public boolean canContinueToUse() {
            if (golem.isGuarding()) return false;
            owner = golem.getOwnerPlayer();
            if (owner == null || !owner.isAlive()) return false;
            LivingEntity t = golem.getTarget();
            if (t != null && t.isAlive()) return false;
            return golem.distanceToSqr(owner) > stopDistSq;
        }

        @Override
        public void start() { recalcTimer = 0; }

        @Override
        public void stop() {
            golem.getNavigation().stop();
            owner = null;
        }

        @Override
        public void tick() {
            owner = golem.getOwnerPlayer();
            if (owner == null) return;

            golem.getLookControl().setLookAt(owner, 10.0F, golem.getMaxHeadXRot());

            if (--recalcTimer > 0) return;
            recalcTimer = 10;

            double distSq = golem.distanceToSqr(owner);
            if (distSq <= stopDistSq) { 
                golem.getNavigation().stop(); 
                return; 
            }

            // Come il lupo: se sei a più di 12 blocchi (12*12 = 144), si teletrasporta.
            if (distSq > 144.0D) {
                teleportNear(owner);
            } else {
                golem.getNavigation().moveTo(owner, speed);
            }
        }

        private void teleportNear(Player target) {
            net.minecraft.util.RandomSource rng = golem.getRandom();
            for (int i = 0; i < 10; i++) {
                double ox = target.getX() + (rng.nextFloat() * 6.0F - 3.0F);
                double oz = target.getZ() + (rng.nextFloat() * 6.0F - 3.0F);
                // Cerca un blocco solido partendo da un po' più in alto rispetto al player
                BlockPos g = BlockPos.containing(ox, target.getY() + 2, oz);
                int tries = 6;
                while (tries-- > 0 && !golem.level().getBlockState(g).isSolid()) {
                    g = g.below();
                }
                
                if (golem.level().getBlockState(g).isSolid()) {
                    BlockPos tpPos = g.above();
                    if (golem.level().noCollision(golem, golem.getBoundingBox().move(
                            ox - golem.getX(), tpPos.getY() - golem.getY(), oz - golem.getZ()))) {
                        golem.teleportTo(ox, tpPos.getY(), oz);
                        golem.getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }

    // =========================================================================
    //  Target Goals
    // =========================================================================

    static class OwnerHurtByTargetGoal extends TargetGoal {
        private LivingEntity attackerOfOwner;
        private int lastHurtTs;

        OwnerHurtByTargetGoal(SoldierGolemEntity g) { super(g, false); }

        @Override public boolean canUse() {
            if (((SoldierGolemEntity) mob).isGuarding()) return false; // don't care if owner is hurt while guarding
            Player owner = ((SoldierGolemEntity) mob).getOwnerPlayer();
            if (owner == null) return false;
            attackerOfOwner = owner.getLastHurtByMob();
            int ts = owner.getLastHurtByMobTimestamp();
            return ts != lastHurtTs && attackerOfOwner != null && attackerOfOwner != mob
                    && canAttack(attackerOfOwner, TargetingConditions.DEFAULT);
        }
        @Override public void start() {
            mob.setTarget(attackerOfOwner);
            Player owner = ((SoldierGolemEntity) mob).getOwnerPlayer();
            if (owner != null) lastHurtTs = owner.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    static class OwnerHurtTargetGoal extends TargetGoal {
        private LivingEntity ownerTarget;
        private int lastHurtTs;

        OwnerHurtTargetGoal(SoldierGolemEntity g) { super(g, false); }

        @Override public boolean canUse() {
            if (((SoldierGolemEntity) mob).isGuarding()) return false; // don't care about owner targets while guarding
            Player owner = ((SoldierGolemEntity) mob).getOwnerPlayer();
            if (owner == null) return false;
            ownerTarget = owner.getLastHurtMob();
            int ts = owner.getLastHurtMobTimestamp();
            return ts != lastHurtTs && ownerTarget != null && ownerTarget != mob
                    && canAttack(ownerTarget, TargetingConditions.DEFAULT);
        }
        @Override public void start() {
            mob.setTarget(ownerTarget);
            Player owner = ((SoldierGolemEntity) mob).getOwnerPlayer();
            if (owner != null) lastHurtTs = owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    /** Proactive defense: attacks Enemy mobs that enter within {@code range} blocks of the owner. */
    static class NearOwnerEnemyGoal extends TargetGoal {
        private final double range;
        private LivingEntity foundEnemy;

        NearOwnerEnemyGoal(SoldierGolemEntity g, double range) {
            super(g, false);
            this.range = range;
        }

        @Override public boolean canUse() {
            LivingEntity ex = mob.getTarget();
            if (ex != null && ex.isAlive()) return false;
            
            SoldierGolemEntity sg = (SoldierGolemEntity) mob;
            
            net.minecraft.world.phys.AABB searchBox;
            if (sg.isGuarding() && sg.getGuardPos() != null) {
                // If guarding, protect the guard area
                searchBox = new net.minecraft.world.phys.AABB(sg.getGuardPos()).inflate(range);
            } else {
                // If companion, protect owner
                Player owner = sg.getOwnerPlayer();
                if (owner == null) return false;
                searchBox = owner.getBoundingBox().inflate(range);
            }

            List<Mob> enemies = mob.level().getEntitiesOfClass(
                    Mob.class,
                    searchBox,
                    e -> e instanceof Enemy && e.isAlive() && !e.is(mob)
            );
            if (enemies.isEmpty()) return false;
            
            net.minecraft.world.phys.Vec3 center = sg.isGuarding() ? net.minecraft.world.phys.Vec3.atBottomCenterOf(sg.getGuardPos()) : sg.getOwnerPlayer().position();
            
            foundEnemy = enemies.stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(center)))
                    .orElse(null);
            return foundEnemy != null && canAttack(foundEnemy, TargetingConditions.DEFAULT);
        }

        @Override public boolean canContinueToUse() { return false; }

        @Override public void start() {
            mob.setTarget(foundEnemy);
            super.start();
        }
    }
}
