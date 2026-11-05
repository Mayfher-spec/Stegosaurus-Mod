package com.prodigytown.creatures.net;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

/** Canal de red cliente->servidor. El estado del arbol viaja al cliente por datos sincronizados de la entidad. */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ProdigyCreatures.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered = false;
    private ModNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, TalentAction.class, TalentAction::encode, TalentAction::decode, TalentAction::handle);
        CHANNEL.registerMessage(1, UseAbility.class, UseAbility::encode, UseAbility::decode, UseAbility::handle);
    }

    /** action 0 = subir rango de un talento (index), 1 = restablecer. */
    public record TalentAction(int entityId, int action, int index) {
        public static void encode(TalentAction m, FriendlyByteBuf buf) { buf.writeInt(m.entityId); buf.writeByte(m.action); buf.writeByte(m.index); }
        public static TalentAction decode(FriendlyByteBuf buf) { return new TalentAction(buf.readInt(), buf.readByte(), buf.readByte()); }
        public static void handle(TalentAction m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                Entity e = sp.level().getEntity(m.entityId);
                if (!(e instanceof StegosaurusEntity stego) || !stego.isOwnedBy(sp) || stego.distanceTo(sp) > 40.0F) return;
                if (m.action == 0) stego.unlockTalent(m.index);
                else if (m.action == 1) stego.resetTalents();
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** id 0 = embestida, 1 = coletazo devastador. Actua el compañero propio mas cercano. */
    public record UseAbility(int ability) {
        public static void encode(UseAbility m, FriendlyByteBuf buf) { buf.writeByte(m.ability); }
        public static UseAbility decode(FriendlyByteBuf buf) { return new UseAbility(buf.readByte()); }
        public static void handle(UseAbility m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                List<StegosaurusEntity> pets = sp.level().getEntitiesOfClass(StegosaurusEntity.class,
                    sp.getBoundingBox().inflate(24.0), s -> s.isTame() && s.isOwnedBy(sp));
                StegosaurusEntity best = null;
                for (StegosaurusEntity s : pets) if (best == null || s.distanceToSqr(sp) < best.distanceToSqr(sp)) best = s;
                if (best != null) best.useAbility(sp, m.ability);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
