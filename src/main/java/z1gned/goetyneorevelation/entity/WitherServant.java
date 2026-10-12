package z1gned.goetyneorevelation.entity;

import com.Polarice3.Goety.api.entities.IMobTyped;
import com.Polarice3.Goety.common.entities.ally.Summoned;
import com.Polarice3.Goety.init.ModMobType;
import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import com.Polarice3.Goety.utils.MobTypeHelper;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 从原版 Goety Revelation (Forge 1.20.1) 的 WitherServant 移植到 NeoForge 1.21.1。
 *
 * <p>关键移植点（不同于未测试的 Forge 包，全部以本地 NeoForm 1.21.1 源码核对）：
 * <ul>
 *   <li>{@link #defineSynchedData(SynchedEntityData.Builder)} 改用 NeoForge 的 Builder 形参。</li>
 *   <li>实体破坏钩子使用 {@link EventHooks#canEntityGrief}/{@link EventHooks#onEntityDestroyBlock}。</li>
 *   <li>实体发光粒子改用 {@link ColorParticleOption#create}（原代码把颜色塞进速度参数，在 1.21.1 不生效）。</li>
 *   <li>目标搜索按1.21.1 Level实体筛选接口适配，原range/亡灵过滤不变。</li>
 *   <li>{@code dropCustomDeathLoot} 签名变为 {@code (ServerLevel, DamageSource, boolean)}。</li>
 *   <li>{@code canChangeDimensions} 签名保持 {@code (Level, Level)}。</li>
 *   <li>亡灵归类使用 {@link IMobTyped#getGoetyMobType()} 与原版 undead 标签；不再引用已删除的旧 MobType。</li>
 *   <li>WitherSkull 构造改为三参 {@code (Level, LivingEntity, Vec3 方向)}。</li>
 * </ul>
 */
public class WitherServant extends Summoned implements PowerableMob, RangedAttackMob, IMobTyped {
    private static final EntityDataAccessor<Integer> DATA_TARGET_A = SynchedEntityData.defineId(WitherServant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_B = SynchedEntityData.defineId(WitherServant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_C = SynchedEntityData.defineId(WitherServant.class, EntityDataSerializers.INT);
    private static final List<EntityDataAccessor<Integer>> DATA_TARGETS = ImmutableList.of(DATA_TARGET_A, DATA_TARGET_B, DATA_TARGET_C);
    private static final EntityDataAccessor<Integer> DATA_ID_INV = SynchedEntityData.defineId(WitherServant.class, EntityDataSerializers.INT);
    private final float[] xRotHeads = new float[2];
    private final float[] yRotHeads = new float[2];
    private final float[] xRotOHeads = new float[2];
    private final float[] yRotOHeads = new float[2];
    private final int[] nextHeadUpdate = new int[2];
    private final int[] idleHeadUpdates = new int[2];
    private int destroyBlocksTick;
    private static final Predicate<LivingEntity> LIVING_ENTITY_SELECTOR = arg0 -> MobTypeHelper.getMobType(arg0) != ModMobType.UNDEAD && arg0.attackable();
    private static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forCombat().range(20.0).selector(LIVING_ENTITY_SELECTOR);

    public WitherServant(EntityType<? extends WitherServant> arg0, Level arg1) {
        super(arg0, arg1);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setHealth(this.getMaxHealth());
        this.xpReward = 50;
    }

    protected PathNavigation createNavigation(Level arg0) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, arg0);
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(true);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WitherServant.WitherDoNothingGoal());
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 20.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, false, false, LIVING_ENTITY_SELECTOR));
    }

    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_TARGET_A, 0);
        pBuilder.define(DATA_TARGET_B, 0);
        pBuilder.define(DATA_TARGET_C, 0);
        pBuilder.define(DATA_ID_INV, 0);
    }

    public void addAdditionalSaveData(CompoundTag arg0) {
        super.addAdditionalSaveData(arg0);
        arg0.putInt("Invul", this.getInvulnerableTicks());
    }

    public void readAdditionalSaveData(CompoundTag arg0) {
        super.readAdditionalSaveData(arg0);
        this.setInvulnerableTicks(arg0.getInt("Invul"));
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource arg0) {
        return SoundEvents.WITHER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    public void aiStep() {
        Vec3 vec3 = this.getDeltaMovement().multiply(1.0, 0.6, 1.0);
        if (!this.level().isClientSide && this.getAlternativeTarget(0) > 0) {
            Entity entity = this.level().getEntity(this.getAlternativeTarget(0));
            if (entity != null) {
                double d0 = vec3.y;
                if (this.getY() < entity.getY() || !this.isPowered() && this.getY() < entity.getY() + 5.0) {
                    d0 = Math.max(0.0, d0);
                    d0 += 0.3 - d0 * 0.6000000238418579;
                }

                vec3 = new Vec3(vec3.x, d0, vec3.z);
                Vec3 vec31 = new Vec3(entity.getX() - this.getX(), 0.0, entity.getZ() - this.getZ());
                if (vec31.horizontalDistanceSqr() > 9.0) {
                    Vec3 vec32 = vec31.normalize();
                    vec3 = vec3.add(vec32.x * 0.3 - vec3.x * 0.6, 0.0, vec32.z * 0.3 - vec3.z * 0.6);
                }
            }
        }

        this.setDeltaMovement(vec3);
        if (vec3.horizontalDistanceSqr() > 0.05) {
            this.setYRot((float)Mth.atan2(vec3.z, vec3.x) * 57.295776F - 90.0F);
        }

        super.aiStep();

        for (int i = 0; i < 2; i++) {
            this.yRotOHeads[i] = this.yRotHeads[i];
            this.xRotOHeads[i] = this.xRotHeads[i];
        }

        for (int j = 0; j < 2; j++) {
            int k = this.getAlternativeTarget(j + 1);
            Entity entity1 = null;
            if (k > 0) {
                entity1 = this.level().getEntity(k);
            }

            if (entity1 != null) {
                double d9 = this.getHeadX(j + 1);
                double d1 = this.getHeadY(j + 1);
                double d3 = this.getHeadZ(j + 1);
                double d4 = entity1.getX() - d9;
                double d5 = entity1.getEyeY() - d1;
                double d6 = entity1.getZ() - d3;
                double d7 = Math.sqrt(d4 * d4 + d6 * d6);
                float f = (float)(Mth.atan2(d6, d4) * 57.2957763671875) - 90.0F;
                float f1 = (float)(-(Mth.atan2(d5, d7) * 57.2957763671875));
                this.xRotHeads[j] = this.rotlerp(this.xRotHeads[j], f1, 40.0F);
                this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], f, 10.0F);
            } else {
                this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], this.yBodyRot, 10.0F);
            }
        }

        boolean flag = this.isPowered();

        for (int l = 0; l < 3; l++) {
            double d8 = this.getHeadX(l);
            double d10 = this.getHeadY(l);
            double d2 = this.getHeadZ(l);
            this.level()
                .addParticle(
                    ParticleTypes.SMOKE,
                    d8 + this.random.nextGaussian() * 0.30000001192092896,
                    d10 + this.random.nextGaussian() * 0.30000001192092896,
                    d2 + this.random.nextGaussian() * 0.30000001192092896,
                    0.0,
                    0.0,
                    0.0
                );
            if (flag && this.level().random.nextInt(4) == 0) {
                this.level()
                    .addParticle(
                        ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.699999988079071F, 0.699999988079071F, 0.5F),
                        d8 + this.random.nextGaussian() * 0.30000001192092896,
                        d10 + this.random.nextGaussian() * 0.30000001192092896,
                        d2 + this.random.nextGaussian() * 0.30000001192092896,
                        0.0,
                        0.0,
                        0.0
                    );
            }
        }

        if (this.getInvulnerableTicks() > 0) {
            for (int i1 = 0; i1 < 3; i1++) {
                this.level()
                    .addParticle(
                        ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.699999988079071F, 0.699999988079071F, 0.8999999761581421F),
                        this.getX() + this.random.nextGaussian(),
                        this.getY() + (double)(this.random.nextFloat() * 3.3F),
                        this.getZ() + this.random.nextGaussian(),
                        0.0,
                        0.0,
                        0.0
                    );
            }
        }
    }

    protected void customServerAiStep() {
        if (this.getInvulnerableTicks() > 0) {
            int k1 = this.getInvulnerableTicks() - 1;
            if (k1 <= 0) {
                this.level().explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, ExplosionInteraction.MOB);
                if (!this.isSilent()) {
                    this.level().globalLevelEvent(1023, this.blockPosition(), 0);
                }
            }

            this.setInvulnerableTicks(k1);
            if (this.tickCount % 10 == 0) {
                this.heal(10.0F);
            }
        } else {
            super.customServerAiStep();

            for (int i = 1; i < 3; i++) {
                if (this.tickCount >= this.nextHeadUpdate[i - 1]) {
                    this.nextHeadUpdate[i - 1] = this.tickCount + 10 + this.random.nextInt(10);
                    if (this.level().getDifficulty() == Difficulty.NORMAL || this.level().getDifficulty() == Difficulty.HARD) {
                        int i3 = i - 1;
                        int j3 = this.idleHeadUpdates[i - 1];
                        this.idleHeadUpdates[i3] = this.idleHeadUpdates[i - 1] + 1;
                        if (j3 > 15) {
                            float f = 10.0F;
                            float f1 = 5.0F;
                            double d0 = Mth.nextDouble(this.random, this.getX() - 10.0, this.getX() + 10.0);
                            double d1 = Mth.nextDouble(this.random, this.getY() - 5.0, this.getY() + 5.0);
                            double d2 = Mth.nextDouble(this.random, this.getZ() - 10.0, this.getZ() + 10.0);
                            this.performRangedAttack(i + 1, d0, d1, d2, true);
                            this.idleHeadUpdates[i - 1] = 0;
                        }
                    }

                    int l1 = this.getAlternativeTarget(i);
                    if (l1 > 0) {
                        LivingEntity livingentity = (LivingEntity)this.level().getEntity(l1);
                        if (livingentity != null && this.canAttack(livingentity) && !(this.distanceToSqr(livingentity) > 900.0) && this.hasLineOfSight(livingentity)) {
                            this.performRangedAttack(i + 1, livingentity);
                            this.nextHeadUpdate[i - 1] = this.tickCount + 40 + this.random.nextInt(20);
                            this.idleHeadUpdates[i - 1] = 0;
                        } else {
                            this.setAlternativeTarget(i, 0);
                        }
                    } else {
                        List<LivingEntity> list = this.level().getEntities(
                            EntityTypeTest.forClass(LivingEntity.class),
                            this.getBoundingBox().inflate(20.0, 8.0, 20.0),
                            (LivingEntity le) -> TARGETING_CONDITIONS.test(this, le)
                        );
                        if (!list.isEmpty()) {
                            LivingEntity livingentity1 = list.get(this.random.nextInt(list.size()));
                            this.setAlternativeTarget(i, livingentity1.getId());
                        }
                    }
                }
            }

            if (this.getTarget() != null) {
                this.setAlternativeTarget(0, this.getTarget().getId());
            } else {
                this.setAlternativeTarget(0, 0);
            }

            if (this.destroyBlocksTick > 0) {
                this.destroyBlocksTick--;
                if (this.destroyBlocksTick == 0 && EventHooks.canEntityGrief(this.level(), this)) {
                    int j1 = Mth.floor(this.getY());
                    int i2 = Mth.floor(this.getX());
                    int j2 = Mth.floor(this.getZ());
                    boolean flag = false;

                    for (int j = -1; j <= 1; j++) {
                        for (int k2 = -1; k2 <= 1; k2++) {
                            for (int k = 0; k <= 3; k++) {
                                int l2 = i2 + j;
                                int l = j1 + k;
                                int i1 = j2 + k2;
                                BlockPos blockpos = new BlockPos(l2, l, i1);
                                BlockState blockstate = this.level().getBlockState(blockpos);
                                if (blockstate.canEntityDestroy(this.level(), blockpos, this) && EventHooks.onEntityDestroyBlock(this, blockpos, blockstate)) {
                                    flag = this.level().destroyBlock(blockpos, true, this) || flag;
                                }
                            }
                        }
                    }

                    if (flag) {
                        this.level().levelEvent((Player)null, 1022, this.blockPosition(), 0);
                    }
                }
            }

            if (this.tickCount % 20 == 0) {
                this.heal(1.0F);
            }
        }
    }

    @Deprecated
    public static boolean canDestroy(BlockState arg0) {
        return !arg0.isAir() && !arg0.is(BlockTags.WITHER_IMMUNE);
    }

    public void makeStuckInBlock(BlockState arg0, Vec3 arg1) {
    }

    public void startSeenByPlayer(ServerPlayer arg0) {
        super.startSeenByPlayer(arg0);
    }

    public void stopSeenByPlayer(ServerPlayer arg0) {
        super.stopSeenByPlayer(arg0);
    }

    private double getHeadX(int arg0) {
        if (arg0 <= 0) {
            return this.getX();
        } else {
            float f = (this.yBodyRot + (float)(180 * (arg0 - 1))) * 0.017453292F;
            float f1 = Mth.cos(f);
            return this.getX() + (double)f1 * 1.3;
        }
    }

    public boolean isOnFire() {
        return false;
    }

    private double getHeadY(int arg0) {
        return arg0 <= 0 ? this.getY() + 3.0 : this.getY() + 2.2;
    }

    private double getHeadZ(int arg0) {
        if (arg0 <= 0) {
            return this.getZ();
        } else {
            float f = (this.yBodyRot + (float)(180 * (arg0 - 1))) * 0.017453292F;
            float f1 = Mth.sin(f);
            return this.getZ() + (double)f1 * 1.3;
        }
    }

    private float rotlerp(float arg0, float arg1, float arg2) {
        float f = Mth.wrapDegrees(arg1 - arg0);
        if (f > arg2) {
            f = arg2;
        }

        if (f < -arg2) {
            f = -arg2;
        }

        return arg0 + f;
    }

    private void performRangedAttack(int arg0, LivingEntity arg1) {
        this.performRangedAttack(
            arg0,
            arg1.getX(),
            arg1.getY() + (double)arg1.getEyeHeight() * 0.5,
            arg1.getZ(),
            arg0 == 0 && this.random.nextFloat() < 0.001F
        );
    }

    private void performRangedAttack(int arg0, double arg1, double arg2, double arg3, boolean arg4) {
        if (!this.isSilent()) {
            this.level().levelEvent((Player)null, 1024, this.blockPosition(), 0);
        }

        double d0 = this.getHeadX(arg0);
        double d1 = this.getHeadY(arg0);
        double d2 = this.getHeadZ(arg0);
        double d3 = arg1 - d0;
        double d4 = arg2 - d1;
        double d5 = arg3 - d2;
        WitherSkull witherskull = new WitherSkull(this.level(), this, new Vec3(d3, d4, d5).normalize());
        witherskull.setOwner(this);
        if (arg4) {
            witherskull.setDangerous(true);
        }

        witherskull.setPosRaw(d0, d1, d2);
        this.level().addFreshEntity(witherskull);
    }

    public void performRangedAttack(LivingEntity arg0, float arg1) {
        this.performRangedAttack(0, arg0);
    }

    public boolean hurt(DamageSource arg0, float arg1) {
        if (this.isInvulnerableTo(arg0)) {
            return false;
        } else if (arg0.is(DamageTypeTags.WITHER_IMMUNE_TO) || arg0.getEntity() instanceof WitherServant) {
            return false;
        } else if (this.getInvulnerableTicks() > 0 && !arg0.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            if (this.isPowered()) {
                Entity entity = arg0.getDirectEntity();
                if (entity instanceof AbstractArrow) {
                    return false;
                }
            }

            Entity entity1 = arg0.getEntity();
            if (entity1 != null && !(entity1 instanceof Player) && entity1 instanceof LivingEntity && MobTypeHelper.getMobType((LivingEntity)entity1) == this.getGoetyMobType()) {
                return false;
            } else {
                if (this.destroyBlocksTick <= 0) {
                    this.destroyBlocksTick = 20;
                }

                for (int i = 0; i < this.idleHeadUpdates.length; i++) {
                    this.idleHeadUpdates[i] = this.idleHeadUpdates[i] + 3;
                }

                return super.hurt(arg0, arg1);
            }
        }
    }

    protected void dropCustomDeathLoot(ServerLevel pLevel, DamageSource pCause, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pLevel, pCause, pRecentlyHit);
        ItemEntity itementity = this.spawnAtLocation(Items.NETHER_STAR);
        if (itementity != null) {
            itementity.setExtendedLifetime();
        }
    }

    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.discard();
        } else {
            this.noActionTime = 0;
        }
    }

    public boolean addEffect(MobEffectInstance arg0, @Nullable Entity arg1) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.6000000238418579)
            .add(Attributes.FLYING_SPEED, 0.6000000238418579)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 4.0);
    }

    public boolean isInvulnerableTo(DamageSource arg0) {
        return arg0.is(DamageTypeTags.IS_FALL) && super.isInvulnerableTo(arg0);
    }

    public float getHeadYRot(int arg0) {
        return this.yRotHeads[arg0];
    }

    public float getHeadXRot(int arg0) {
        return this.xRotHeads[arg0];
    }

    public int getInvulnerableTicks() {
        return this.entityData.get(DATA_ID_INV);
    }

    public void setInvulnerableTicks(int arg0) {
        this.entityData.set(DATA_ID_INV, arg0);
    }

    public int getAlternativeTarget(int arg0) {
        return this.entityData.get(DATA_TARGETS.get(arg0));
    }

    public void setAlternativeTarget(int arg0, int arg1) {
        this.entityData.set(DATA_TARGETS.get(arg0), arg1);
    }

    public boolean isPowered() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    @Override
    public ModMobType getGoetyMobType() {
        return ModMobType.UNDEAD;
    }

    protected boolean canRide(Entity arg0) {
        return false;
    }

    public boolean canChangeDimensions(Level arg0, Level arg1) {
        return false;
    }

    public boolean canBeAffected(MobEffectInstance arg0) {
        return arg0.getEffect().is(MobEffects.WITHER) ? false : super.canBeAffected(arg0);
    }

    class WitherDoNothingGoal extends Goal {
        public WitherDoNothingGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        public boolean canUse() {
            return WitherServant.this.getInvulnerableTicks() > 0;
        }
    }
}
