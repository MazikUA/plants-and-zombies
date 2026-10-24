package joshxviii.plantz.gui

import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazNetwork.ZombieRaidClientCache
import joshxviii.plantz.networking.ZombieRaidClientData
import joshxviii.plantz.pazResource
import joshxviii.plantz.raid.ZombieRaid
import joshxviii.plantz.renderer.outlineText
import joshxviii.plantz.tickTimeFormat
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth

object ZombieRaidOverlay {

    @JvmStatic
    fun isZombieRaid(event: LerpingBossEvent): Boolean {
        return event.name.toString().contains("zombie")
    }

    val BACKGROUND: Identifier = pazResource("textures/gui/raid/background.png")
    val BACKGROUND_CREDITS: Identifier = pazResource("textures/gui/raid/background_after_credits.png")
    val PLANT_FLAG_SPRITE: Identifier = pazResource("textures/gui/raid/plant_flag.png")
    val ZOMBIE_HEAD_SPRITE: Identifier = pazResource("textures/gui/raid/zombie_head.png")
    val ZOMBIE_HEAD_CREDITS_SPRITE: Identifier = pazResource("textures/gui/raid/zombie_head_after_credits.png")
    val HEALTH_BAR_FLAG: Identifier = pazResource("textures/gui/raid/flag_health.png")
    val HEALTH_BAR_ZOMBIES: Identifier = pazResource("textures/gui/raid/zombie_health.png")
    val BOSS_BAR_BACKGROUND: Identifier = pazResource("textures/gui/raid/boss_bar_background.png")
    val BOSS_BAR: Identifier = pazResource("textures/gui/raid/boss_bar.png")

    const val BG_WIDTH = 256
    const val BG_HEIGHT = 32

    const val BAR_WIDTH = 98
    const val BAR_HEIGHT = 5

    fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        deltaTracker: DeltaTracker
    ) {
        if (PazConfig.HIDE_RAID_UI) return
        renderRaidUI(graphics, deltaTracker)
        ZombieHudMarkers.renderIcons(graphics, deltaTracker)
    }

    private fun renderRaidUI(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val font = Minecraft.getInstance().font
        val raidEvent = ZombieRaidClientCache.get() ?: return

        val screenWidth = graphics.guiWidth()
        val x = screenWidth / 2 - (BG_WIDTH / 2)
        val y = 0

        // tombstone bg
        graphics.blit(RenderPipelines.GUI_TEXTURED, if (raidEvent.seenCredits) BACKGROUND_CREDITS else BACKGROUND, x, y, 0f, 0f, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT)

        // flag health
        renderPlantBar(x, y, graphics, raidEvent)

        // zombie health
        zombieBar(x, y, graphics, raidEvent)

        // boss health
        //bossBar(x, y, graphics, raidEvent)

        // timer
        val time = raidEvent.waveTimer
        val timer = Component.literal(time.tickTimeFormat())

        val textX = screenWidth / 2 - font.width(timer) / 2
        val textY = y + 18
        val textColor = when (time) {
            in -1..600 -> 0xFF5555
            in 601..1200 -> 0xFFFF55
            else -> 0xFFFFFF
        }
        if (raidEvent.status != ZombieRaid.ZombieRaidStatus.NEXT_WAVE) graphics.outlineText(font, timer, textX, textY, color = textColor)

        val waveText = "${raidEvent.currentWaveNumber()} / ${raidEvent.numWaves}"
        val waveX = screenWidth / 2 - font.width(waveText) / 2
        graphics.text(font, waveText, waveX, textY+14, -1)
        if (PazConfig.SHOW_DEBUG_INFO) {
            val textX = screenWidth / 2 - font.width(raidEvent.status.name) / 2
            graphics.text(font, raidEvent.status.name, textX, textY+30, -1)
        }
    }

    private fun renderPlantBar(x: Int, y: Int, graphics: GuiGraphicsExtractor, raidEvent: ZombieRaidClientData) {
        val flagHealthPercent = raidEvent.plantProgressBar
        val flagBarWidth = Mth.floor(BAR_WIDTH * flagHealthPercent)
        val flagHealthX = x + 17 + (BAR_WIDTH - flagBarWidth)
        val flagHealthY = y + 15
        val flagFrame = if (raidEvent.activeTime % 30 > 15) 1 else 0
        graphics.blit(RenderPipelines.GUI_TEXTURED, HEALTH_BAR_FLAG, flagHealthX, flagHealthY, 0f, 0f, flagBarWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT)
        graphics.blit(RenderPipelines.GUI_TEXTURED, PLANT_FLAG_SPRITE, flagHealthX-7, flagHealthY-5, 7f*flagFrame, 0f, 7, 10, 14, 10)
    }

    private fun zombieBar(x: Int, y: Int, graphics: GuiGraphicsExtractor, raidEvent: ZombieRaidClientData) {
        val zombieHealthPercent = raidEvent.zombieProgressBar
        if (zombieHealthPercent > 0f) {
            val zombieBarWidth = Mth.floor(BAR_WIDTH * zombieHealthPercent)
            val zombieHealthX = x + (BG_WIDTH-BAR_WIDTH) - 17
            val zombieHealthY = y + 15
            graphics.blit(RenderPipelines.GUI_TEXTURED, HEALTH_BAR_ZOMBIES, zombieHealthX, zombieHealthY, 0f, 0f, zombieBarWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT)
            val headWidth = 10
            val headCount = zombieBarWidth / 8
            if (raidEvent.status != ZombieRaid.ZombieRaidStatus.NEXT_WAVE) for (i in 0..headCount) {
                val headX = zombieHealthX + (i * 8)
                val headY = zombieHealthY - if (i % 2 == 0) 3 else 0
                val headFrame = i % 4f
                graphics.blit(RenderPipelines.GUI_TEXTURED, if (raidEvent.seenCredits) ZOMBIE_HEAD_CREDITS_SPRITE else  ZOMBIE_HEAD_SPRITE, headX, headY, headWidth*headFrame, 0f, headWidth, 11, headWidth*4, 11)
            }
        }
    }

    private fun bossBar(x: Int, y: Int, graphics: GuiGraphicsExtractor, raidEvent: ZombieRaidClientData) {
        val bossHealthPercent = raidEvent.bossProgressBar
        if (bossHealthPercent <= 0f) return //TODO maybe use -1 to hide bar?

        val font = Minecraft.getInstance().font

        val text = "$bossHealthPercent"
        val bossBarWidth = Mth.floor(BAR_WIDTH * bossHealthPercent)
        val bossHealthX = (graphics.guiWidth()/2) - font.width(text) / 2
        val bossHealthY = y + 45
        graphics.text(font, text, bossHealthX, bossHealthY, -1)
        return
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOSS_BAR_BACKGROUND, bossHealthX, bossHealthY, 0f, 0f, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT)
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOSS_BAR, bossHealthX, bossHealthY, 0f, 0f, bossBarWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT)
    }
}