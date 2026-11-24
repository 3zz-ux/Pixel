package com.backpackmod.client;

import com.backpackmod.BackpackMod;
import com.backpackmod.ModRegistry;
import com.backpackmod.OpenBackpackPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BackpackClient implements ClientModInitializer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(BackpackMod.id("backpack"), "main");
    private static KeyMapping openKey;
    private static KeyMapping nightVisionKey;
    private static boolean nightVisionActive = false;

    public static boolean isNightVisionActive() {
        return nightVisionActive;
    }

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModRegistry.BACKPACK_MENU, BackpackScreen::new);
        ModelLayerRegistry.registerModelLayer(LAYER, BackpackModel::createLayer);

        KeyMapping.Category pixelCategory = KeyMapping.Category.register(BackpackMod.id("main"));

        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.backpackmod.open", InputConstants.KEY_B, pixelCategory));
        nightVisionKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.backpackmod.night_vision", InputConstants.KEY_V, pixelCategory));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.player != null) ClientPlayNetworking.send(new OpenBackpackPayload());
            }

            while (nightVisionKey.consumeClick()) {
                if (mc.player != null) {
                    nightVisionActive = !nightVisionActive;
                    if (nightVisionActive) {
                        mc.player.sendSystemMessage(Component.literal("§6§l[Pixel] §aNight Vision: ON"));
                    } else {
                        mc.player.sendSystemMessage(Component.literal("§6§l[Pixel] §cNight Vision: OFF"));
                    }
                }
            }
        });

        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof AvatarRenderer)
                helper.register(new BackpackLayer((RenderLayerParent) renderer, new BackpackModel(ctx.bakeLayer(LAYER))));
        });

        HammerOutlineRenderer.register();
    }
}
