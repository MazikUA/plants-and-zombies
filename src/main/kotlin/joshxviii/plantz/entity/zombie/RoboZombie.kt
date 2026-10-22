package joshxviii.plantz.entity.zombie

import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.MeleeAttackActionGoal
import joshxviii.plantz.ai.goal.NavigateToTargetGoal
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.projectile.Missile
import joshxviii.plantz.entity.zombie.SuperBrainz.Companion.BEAM_COOLDOWN_ID
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class RoboZombie(type: EntityType<out RoboZombie>, level: Level) : PazZombie(type, level) {

    companion object {
        const val MISSILE_COOLDOWN_TIME = 210

        val TANK_TRANSFORMATION: EntityDataAccessor<Boolean> = SynchedEntityData.defineId<Boolean>(RoboZombie::class.java, EntityDataSerializers.BOOLEAN)
        val MISSILE_TIME_ID: EntityDataAccessor<Int> = SynchedEntityData.defineId<Int>(RoboZombie::class.java, EntityDataSerializers.INT)
        val MISSILE_COOLDOWN_ID: EntityDataAccessor<Int> = SynchedEntityData.defineId<Int>(RoboZombie::class.java, EntityDataSerializers.INT)
        val BASH_TIME_ID: EntityDataAccessor<Int> = SynchedEntityData.defineId<Int>(RoboZombie::class.java, EntityDataSerializers.INT)
    }

    init {
        xpReward = 30
    }

    override fun getAmbientSound(): SoundEvent {
        return PazSounds.ROBO_ZOMBIE_AMBIENT
    }
    override fun getHurtSound(source: DamageSource): SoundEvent {
        return PazSounds.ROBO_ZOMBIE_HURT
    }
    override fun getDeathSound(): SoundEvent {
        return PazSounds.ROBO_ZOMBIE_DEATH
    }
    override fun getStepSound(): SoundEvent {
        return SoundEvents.COPPER_GOLEM_STEP // TODO: custom sounds
    }

    val walkNavigation = NavigateToTargetGoal(this)
    val tankNavigation = NavigateToTargetGoal(
        this,
        speedModifier = 1.25,
        keepAwayDistance = 5.0,
        alwaysFaceTarget = true
    )

    var missileCooldown: Int
        get() = this.entityData.get(BEAM_COOLDOWN_ID)
        set(value) = this.entityData.set(BEAM_COOLDOWN_ID, value)

    var isTransformed: Boolean
        get() = this.entityData.get(TANK_TRANSFORMATION)
        set(value) = this.entityData.set(TANK_TRANSFORMATION, value)

    var missileTime: Int
        get() = this.entityData.get(MISSILE_TIME_ID)
        set(value) = this.entityData.set(MISSILE_TIME_ID, value)

    var bashTime: Int
        get() = this.entityData.get(BASH_TIME_ID)
        set(value) = this.entityData.set(BASH_TIME_ID, value)

    val idleAnimation : AnimationState = AnimationState()
    val shootAnimation : AnimationState = AnimationState()
    val bashAnimation : AnimationState = AnimationState()

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(TANK_TRANSFORMATION, false)
        entityData.define(MISSILE_COOLDOWN_ID, MISSILE_COOLDOWN_TIME)
        entityData.define(MISSILE_TIME_ID, 0)
        entityData.define(BASH_TIME_ID, 0)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putBoolean("is_transformed", isTransformed)
        output.putInt("missile_cooldown", missileCooldown)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        isTransformed = input.getBooleanOr("is_transformed", false)
        missileCooldown = input.getIntOr("missile_cooldown", MISSILE_COOLDOWN_TIME)
        reassessNavigation()
    }

    fun transform() {
        isTransformed = !isTransformed
        reassessNavigation()
    }

    fun reassessNavigation() {
        goalSelector.removeGoal(walkNavigation)
        goalSelector.removeGoal(tankNavigation)
        if (isTransformed) goalSelector.addGoal(3, tankNavigation)
        else goalSelector.addGoal(3, walkNavigation)
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(4, MeleeAttackActionGoal(
            usingEntity = this,
            damageType = DamageTypes.MOB_ATTACK,
            actionDelay = 10,
            usePredicate = {
                bashTime<=0 && missileTime<=0
            },
            actionStartEffect = {
                bashTime=1
            }
        ))
        goalSelector.addGoal(4, ProjectileAttackGoal(
            usingEntity = this,
            projectileFactory =  { Missile(level(), this) },
            velocity = 1.0,
            actionDelay = 19,
            soundEvent = null,
            usePredicate = {
                missileTime<=0 && missileCooldown<=0
            },
            actionStartEffect = {
                playSound(PazSounds.ROBO_ZOMBIE_SHOOT)
                missileTime=1
            },
            actionSuccessEffect = {
                missileCooldown = MISSILE_COOLDOWN_TIME + random.nextIntBetweenInclusive(10, 50)
            }
        ))
    }

    override fun addBehaviourGoals() {
        addBehaviourGoalsNoMelee()
    }

    override fun canEquipBalloons(): Boolean = false
    override fun canEquipDuckyInWater(): Boolean = false
    override fun canPickUpLoot(): Boolean = false

    override fun getMoveControl(): MoveControl = if (missileTime>0) noMoveControl else super.getMoveControl()

    override fun tick() {
        super.tick()
        if (!this.isNoAi) { updateAnimationState() }
        if (missileCooldown>0) --missileCooldown
    }

    fun updateAnimationState() {
        idleAnimation.startIfStopped(0)

        if (missileTime>0) {
            shootAnimation.startIfStopped(tickCount)
            if (missileTime++>60) {
                shootAnimation.stop()
                missileTime=0
            }
        }

        if (bashTime>0 || swinging) {
            bashAnimation.startIfStopped(tickCount)
            if (bashTime++>20) {
                bashAnimation.stop()
                bashTime=0
            }
        }
    }

    override fun doHurtTarget(level: ServerLevel, target: Entity): Boolean {
        val result = super.doHurtTarget(level, target)
        return result
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnReason: EntitySpawnReason,
        groupData: SpawnGroupData?
    ): SpawnGroupData? {
        reassessNavigation()
        val data = super.finalizeSpawn(level, difficulty, spawnReason, groupData)

        missileCooldown = MISSILE_COOLDOWN_TIME

        if (spawnReason != EntitySpawnReason.CONVERSION) {
            setCanBreakDoors(true)
        }

        return data
    }
}