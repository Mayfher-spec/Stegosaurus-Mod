package com.prodigytown.creatures.quests;

import com.prodigytown.creatures.net.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Paquetes de misiones: el servidor manda la lista completa (QuestSync) y el cliente pide aceptar / reclamar / abandonar (QuestAction). */
public final class QuestPackets {
    private QuestPackets() {}

    public record QuestView(String id, String title, String description, String category, int type, int target, int progress,
                            int state, String requiresTitle, List<ItemStack> items, int xp, int tab, ItemStack icon, String objective) {
        void encode(FriendlyByteBuf b) {
            b.writeUtf(id); b.writeUtf(title); b.writeUtf(description, 2048); b.writeUtf(category);
            b.writeVarInt(type); b.writeVarInt(target); b.writeVarInt(progress); b.writeVarInt(state); b.writeUtf(requiresTitle);
            b.writeVarInt(items.size());
            for (ItemStack s : items) b.writeItem(s);
            b.writeVarInt(xp); b.writeVarInt(tab); b.writeItem(icon); b.writeUtf(objective, 512);
        }
        static QuestView decode(FriendlyByteBuf b) {
            String id = b.readUtf(), title = b.readUtf(), desc = b.readUtf(2048), cat = b.readUtf();
            int type = b.readVarInt(), target = b.readVarInt(), progress = b.readVarInt(), state = b.readVarInt();
            String req = b.readUtf();
            int n = b.readVarInt();
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < n; i++) items.add(b.readItem());
            int xp = b.readVarInt(), tab = b.readVarInt();
            ItemStack icon = b.readItem();
            return new QuestView(id, title, desc, cat, type, target, progress, state, req, items, xp, tab, icon, b.readUtf(512));
        }
    }

    /** npcId >= 0: abrir la pantalla del Cronista con ese id; -1: solo actualizar datos. */
    public record QuestSync(int npcId, List<QuestView> views) {
        public static void encode(QuestSync m, FriendlyByteBuf b) {
            b.writeInt(m.npcId);
            b.writeVarInt(m.views.size());
            for (QuestView v : m.views) v.encode(b);
        }
        public static QuestSync decode(FriendlyByteBuf b) {
            int npc = b.readInt(), n = b.readVarInt();
            List<QuestView> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(QuestView.decode(b));
            return new QuestSync(npc, list);
        }
        public static void handle(QuestSync m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.prodigytown.creatures.client.QuestClient.onSync(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** action 0 = aceptar, 1 = reclamar recompensa, 2 = abandonar. Solo vale con un Cronista a menos de 10 bloques. */
    public record QuestAction(int action, String id) {
        public static void encode(QuestAction m, FriendlyByteBuf b) { b.writeByte(m.action); b.writeUtf(m.id); }
        public static QuestAction decode(FriendlyByteBuf b) { return new QuestAction(b.readByte(), b.readUtf()); }
        public static void handle(QuestAction m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                QuestDef q = QuestManager.get(m.id);
                if (q == null || sp.level().getEntitiesOfClass(CronistaEntity.class, sp.getBoundingBox().inflate(10.0)).isEmpty()) return;
                if (m.action == 0) QuestData.accept(sp, q);
                else if (m.action == 1) QuestData.claim(sp, q);
                else if (m.action == 2) QuestData.abandon(sp, q);
                QuestData.sync(sp, -1);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static void register() {
        ModNetwork.CHANNEL.registerMessage(5, QuestSync.class, QuestSync::encode, QuestSync::decode, QuestSync::handle,
            java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        ModNetwork.CHANNEL.registerMessage(6, QuestAction.class, QuestAction::encode, QuestAction::decode, QuestAction::handle,
            java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
    }
}
