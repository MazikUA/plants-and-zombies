package joshxviii.plantz.mixin.client;

import joshxviii.plantz.RaidMusicManager;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Josh
 */
@Mixin(NowPlayingToast.class)
public class NowPlayingToastMixin {
    @Inject(method = "getCurrentSongName", at = @At(value = "HEAD"), cancellable = true)
    private static void getCurrentSongName(CallbackInfoReturnable<String> cir) {
        var raidMusic = RaidMusicManager.INSTANCE.getTargetMusic();
        if (raidMusic != null) cir.setReturnValue("music.plantz.raid." + raidMusic.getMusicName());
    }
}
