package com.prodigytown.creatures.net;

import com.prodigytown.creatures.CarnotaurusEntity;

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
        CHANNEL.registerMessage(2, RenamePet.class, RenamePet::encode, RenamePet::decode, RenamePet::handle);
        CHANNEL.registerMessage(3, SetOrder.class, SetOrder::encode, SetOrder::decode, SetOrder::handle);
        CHANNEL.registerMessage(4, ShrinkDragon.class, ShrinkDragon::encode, ShrinkDragon::decode, ShrinkDragon::handle);
    }

    /** action 0 = subir rango de un talento (index), 1 = restablecer, 2 = sentar/levantar, 3 = quitar silla, 4 = quitar armadura, 5 = volver a chibi. */
    public record TalentAction(int entityId, int action, int index) {
        public static void encode(TalentAction m, FriendlyByteBuf buf) { buf.writeInt(m.entityId); buf.writeByte(m.action); buf.writeByte(m.index); }
        public static TalentAction decode(FriendlyByteBuf buf) { return new TalentAction(buf.readInt(), buf.readByte(), buf.readByte()); }
        public static void handle(TalentAction m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                Entity e = sp.level().getEntity(m.entityId);
                if (e instanceof CarnotaurusEntity carno && carno.isOwnedBy(sp) && carno.distanceTo(sp) <= 40.0F) {
                    if (m.action == 0) carno.unlockTalent(m.index);
                    else if (m.action == 1) carno.resetTalents();
                    else if (m.action == 2) carno.toggleSit();
                    else if (m.action == 3) carno.removeSaddle();
                    return;
                }
                if (!(e instanceof StegosaurusEntity stego) || !stego.isOwnedBy(sp) || stego.distanceTo(sp) > 40.0F) return;
                if (m.action == 0) stego.unlockTalent(m.index);
                else if (m.action == 1) stego.resetTalents();
                else if (m.action == 2) stego.toggleSit();
                else if (m.action == 3) stego.removeSaddle();
                else if (m.action == 4) stego.removeArmor();
                else if (m.action == 5) stego.revertToChibi();
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Compat Saint's Dragons: encoge o agranda a tu dragon (all = todos los tuyos cercanos). */
    public record ShrinkDragon(int entityId, boolean all) {
        public static void encode(ShrinkDragon m, FriendlyByteBuf buf) { buf.writeInt(m.entityId); buf.writeBoolean(m.all); }
        public static ShrinkDragon decode(FriendlyByteBuf buf) { return new ShrinkDragon(buf.readInt(), buf.readBoolean()); }
        public static void handle(ShrinkDragon m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp != null) com.prodigytown.creatures.compat.SaintsDragonsCompat.toggle(sp, m.entityId, m.all);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Orden para el adulto o el chibi propio (ver PetOrders). */
    public record SetOrder(int entityId, int order) {
        public static void encode(SetOrder m, FriendlyByteBuf buf) { buf.writeInt(m.entityId); buf.writeByte(m.order); }
        public static SetOrder decode(FriendlyByteBuf buf) { return new SetOrder(buf.readInt(), buf.readByte()); }
        public static void handle(SetOrder m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                Entity e = sp.level().getEntity(m.entityId);
                if (m.order == 9) { // reactivar la evolucion del chibi
                    if (e instanceof com.prodigytown.chibi.ChibiStego c && c.isOwnedBy(sp) && c.distanceTo(sp) <= 40.0F) c.unlockEvolve();
                    return;
                }
                if (e instanceof net.minecraft.world.entity.TamableAnimal t && e instanceof com.prodigytown.creatures.PetOrders.Holder
                        && t.isTame() && t.isOwnedBy(sp) && t.distanceTo(sp) <= 40.0F) {
                    com.prodigytown.creatures.PetOrders.setOrder(t, m.order);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Cambia el nombre del estegosaurio propio (cadena vacia = sin nombre). */
    public record RenamePet(int entityId, String name) {
        public static void encode(RenamePet m, FriendlyByteBuf buf) { buf.writeInt(m.entityId); buf.writeUtf(m.name, 32); }
        public static RenamePet decode(FriendlyByteBuf buf) { return new RenamePet(buf.readInt(), buf.readUtf(32)); }
        public static void handle(RenamePet m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                Entity e = sp.level().getEntity(m.entityId);
                if (e instanceof StegosaurusEntity stego && stego.isOwnedBy(sp) && stego.distanceTo(sp) <= 40.0F) stego.rename(m.name);
                else if (e instanceof CarnotaurusEntity carno && carno.isOwnedBy(sp) && carno.distanceTo(sp) <= 40.0F) carno.rename(m.name);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** id 0 = embestida, 1 = coletazo (basico o devastador), 2 = mordida. Actua el compañero propio mas cercano (o la montura). */
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
                if (sp.getVehicle() instanceof StegosaurusEntity mount && mount.isOwnedBy(sp)) best = mount;
                else for (StegosaurusEntity s : pets) if (best == null || s.distanceToSqr(sp) < best.distanceToSqr(sp)) best = s;
                if (best != null) { best.useAbility(sp, m.ability); return; }
                CarnotaurusEntity carno = null;
                if (sp.getVehicle() instanceof CarnotaurusEntity mount && mount.isOwnedBy(sp)) carno = mount;
                else for (CarnotaurusEntity c : sp.level().getEntitiesOfClass(CarnotaurusEntity.class, sp.getBoundingBox().inflate(24.0),
                        c -> c.isTame() && c.isOwnedBy(sp) && c.isAdultStage()))
                    if (carno == null || c.distanceToSqr(sp) < carno.distanceToSqr(sp)) carno = c;
                if (carno != null) carno.useAbility(sp, m.ability);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
