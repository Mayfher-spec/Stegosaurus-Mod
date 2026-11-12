package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;

/**
 * Tercera persona montado: aleja y sube la camara (el estegosaurio mide mas de 4 bloques de alto y 10 de largo).
 * Camera.move es protegido; se llama por reflexion con el nombre SRG (m_90568_ = move(DDD)V). Si no se encuentra,
 * se desactiva sin romper nada y la camara queda como la de Minecraft.
 */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
public final class MountCamera {
    private static final Logger LOG = LogUtils.getLogger();
    private static final double EXTRA_BACK = 8.0, EXTRA_UP = 1.5;
    private static Method move;
    private static boolean failed;

    private MountCamera() {}

    @SubscribeEvent public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (failed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getVehicle() instanceof StegosaurusEntity || mc.player.getVehicle() instanceof com.prodigytown.creatures.CarnotaurusEntity)) return;
        Camera cam = event.getCamera();
        if (!cam.isDetached() || mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) return;
        try {
            if (move == null) move = ObfuscationReflectionHelper.findMethod(Camera.class, "m_90568_", double.class, double.class, double.class);
            Vec3 pos = cam.getPosition();
            org.joml.Vector3f look = cam.getLookVector();
            Vec3 back = new Vec3(-look.x(), -look.y(), -look.z());
            double dist = EXTRA_BACK;
            HitResult hit = mc.level.clip(new ClipContext(pos, pos.add(back.scale(EXTRA_BACK)),
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS) dist = Math.max(0.0, hit.getLocation().distanceTo(pos) - 0.3);
            move.invoke(cam, -dist, EXTRA_UP, 0.0);
        } catch (Throwable t) {
            failed = true;
            LOG.warn("The Stegosaurus Mod: no se pudo alejar la camara al montar ({})", t.toString());
        }
    }
}
