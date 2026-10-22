package joshxviii.plantz.renderer.entity.zombie

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.entity.zombie.RoboZombie
import joshxviii.plantz.model.zombies.PazZombieModel
import joshxviii.plantz.model.zombies.RoboZombieModel
import joshxviii.plantz.toAngle
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.util.Mth
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class RoboZombieRenderer(
    context: EntityRendererProvider.Context,
    private val model: PazZombieModel<PazZombieRenderState> = RoboZombieModel(context.bakeLayer(RoboZombieModel.LAYER_LOCATION)),
): PazZombieRenderer(context, model, model) {

    override fun submit(
        state: PazZombieRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        super.submit(state, poseStack, collector, camera)
    }

    override fun createRenderState(): PazZombieRenderState {
        return RoboZombieRenderState()
    }

    override fun extractRenderState(entity: PazZombie, state: PazZombieRenderState, partialTicks: Float) {
        (state as RoboZombieRenderState)
        (entity as RoboZombie)
        super.extractRenderState(entity, state, partialTicks)
        updateTreadRotation(entity, state, partialTicks)
        state.isTankTransformation = entity.isTransformed
        state.idleAnimationState.copyFrom(entity.idleAnimation)
        state.shootAnimationState.copyFrom(entity.shootAnimation)
        state.bashAnimationState.copyFrom(entity.bashAnimation)

        state.treadRotation = entity.treadRotation
    }

    private fun updateTreadRotation(entity: RoboZombie, state: RoboZombieRenderState, partialTicks: Float) {
        if (!entity.isTransformed) {
            entity.treadRotation = 0.0f
            return
        }

        val yBodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot)

        val dx = entity.x - entity.xo
        val dz = entity.z - entity.zo
        val distance = sqrt(dx * dx + dz * dz)

        val moveDir =
            if (distance < 0.05)
                entity.treadRotation
            else
                Mth.wrapDegrees(-yBodyRot + Vec3(dx, 0.0, dz).toAngle() + 90)

        entity.treadRotation = Mth.rotLerpRad(partialTicks * 0.025f, entity.treadRotation, moveDir * Mth.DEG_TO_RAD)
    }

}

class RoboZombieRenderState: PazZombieRenderState() {
    var isTankTransformation = false
    var treadRotation: Float = 0.0f
    val idleAnimationState: AnimationState = AnimationState()
    val shootAnimationState: AnimationState = AnimationState()
    val bashAnimationState: AnimationState = AnimationState()
}
