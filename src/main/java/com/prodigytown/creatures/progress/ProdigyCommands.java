package com.prodigytown.creatures.progress;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** /prodigy xp N  y  /prodigy nivel N  (solo operadores): sirven para probar la evolucion y el arbol sin farmear. */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID)
public final class ProdigyCommands {
    private ProdigyCommands() {}

    private static TamableAnimal nearestPet(ServerPlayer player) {
        List<TamableAnimal> pets = player.level().getEntitiesOfClass(TamableAnimal.class,
            player.getBoundingBox().inflate(32.0), e -> e instanceof ProdigyPet && e.isTame() && e.isOwnedBy(player));
        TamableAnimal best = null;
        for (TamableAnimal e : pets) if (best == null || e.distanceToSqr(player) < best.distanceToSqr(player)) best = e;
        return best;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("prodigy").requires(src -> src.hasPermission(2))
            .then(Commands.literal("xp").then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 10_000_000)).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                TamableAnimal pet = nearestPet(player);
                if (pet == null) { ctx.getSource().sendFailure(Component.literal("No hay un compañero tuyo a menos de 32 bloques")); return 0; }
                ((ProdigyPet) pet).gainXp(IntegerArgumentType.getInteger(ctx, "cantidad"));
                ctx.getSource().sendSuccess(() -> Component.literal("Nivel actual: " + ((ProdigyPet) pet).getProgress().level()), false);
                return 1;
            })))
            .then(Commands.literal("nivel").then(Commands.argument("nivel", IntegerArgumentType.integer(1, 100)).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                TamableAnimal pet = nearestPet(player);
                if (pet == null) { ctx.getSource().sendFailure(Component.literal("No hay un compañero tuyo a menos de 32 bloques")); return 0; }
                ProdigyPet pp = (ProdigyPet) pet;
                int target = IntegerArgumentType.getInteger(ctx, "nivel");
                int guard = 0;
                while (pp.getProgress().level() < target && guard++ < 200000) pp.gainXp(Math.max(1, pp.getProgress().xpToNext() - pp.getProgress().xp()));
                ctx.getSource().sendSuccess(() -> Component.literal("Nivel actual: " + pp.getProgress().level()), false);
                return 1;
            }))));
    }
}
