package com.prodigytown.creatures.quests;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /prodigyquests (operadores): invocar al Cronista, reiniciar el progreso, sumar progreso o completar una mision (util para integrar asedios o eventos con command blocks). */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID)
public final class QuestCommands {
    private QuestCommands() {}

    private static final SuggestionProvider<CommandSourceStack> IDS = (ctx, b) -> SharedSuggestionProvider.suggest(QuestManager.all().stream().map(q -> q.id), b);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("prodigyquests").requires(src -> src.hasPermission(2))
            .then(Commands.literal("summon").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                CronistaEntity npc = ProdigyCreatures.CRONISTA.get().spawn(p.serverLevel(), p.blockPosition(), MobSpawnType.COMMAND);
                if (npc != null) npc.setYRot(p.getYRot() + 180.0F);
                return npc == null ? 0 : 1;
            }))
            .then(Commands.literal("reset").then(Commands.argument("jugador", EntityArgument.player()).executes(ctx -> {
                ServerPlayer t = EntityArgument.getPlayer(ctx, "jugador");
                QuestData.reset(t); QuestData.sync(t, -1);
                ctx.getSource().sendSuccess(() -> Component.literal("Misiones reiniciadas para " + t.getName().getString()), true);
                return 1;
            })))
            .then(Commands.literal("progress").then(Commands.argument("jugador", EntityArgument.player())
                .then(Commands.argument("mision", StringArgumentType.string()).suggests(IDS)
                    .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100000)).executes(ctx -> {
                        ServerPlayer t = EntityArgument.getPlayer(ctx, "jugador");
                        QuestDef q = QuestManager.get(StringArgumentType.getString(ctx, "mision"));
                        if (q == null) { ctx.getSource().sendFailure(Component.literal("Mision desconocida")); return 0; }
                        boolean ok = QuestData.add(t, q, IntegerArgumentType.getInteger(ctx, "cantidad"));
                        QuestData.sync(t, -1);
                        if (!ok) ctx.getSource().sendFailure(Component.literal("Esa mision no esta activa para el jugador"));
                        return ok ? 1 : 0;
                    })))))
            .then(Commands.literal("complete").then(Commands.argument("jugador", EntityArgument.player())
                .then(Commands.argument("mision", StringArgumentType.string()).suggests(IDS).executes(ctx -> {
                    ServerPlayer t = EntityArgument.getPlayer(ctx, "jugador");
                    QuestDef q = QuestManager.get(StringArgumentType.getString(ctx, "mision"));
                    if (q == null) { ctx.getSource().sendFailure(Component.literal("Mision desconocida")); return 0; }
                    QuestData.complete(t, q); QuestData.sync(t, -1);
                    return 1;
                })))));
    }
}
