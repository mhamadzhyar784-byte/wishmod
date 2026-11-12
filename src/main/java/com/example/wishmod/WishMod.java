
package com.example.wishmod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WishMod implements ModInitializer {
    private static final double BASE_RISK = 0.20;
    private static final double RISK_PER_EXTRA_ITEM = 0.005;
    private static final double MAX_RISK = 0.75;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
            dispatcher.register(CommandManager.literal("wish")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                    .executes(ctx -> wish(ctx, 1))
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> wish(ctx, IntegerArgumentType.getInteger(ctx, "count")))))));
    }

    private static int wish(CommandContext<ServerCommandSource> ctx, int count) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        ItemStack stack = ItemStackArgumentType.getItemStackArgument(ctx, "item").createStack(1, false);
        stack.setCount(Math.min(count, stack.getMaxCount()));

        double risk = Math.min(MAX_RISK, BASE_RISK + (stack.getCount() - 1) * RISK_PER_EXTRA_ITEM);

        if (world.random.nextDouble() < risk) {
            player.getInventory().clear();
            player.setExperienceLevels(0);
            player.setExperiencePoints(0);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.6f, 1.5f);
            world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.05);
            player.sendMessage(Text.literal("The genie laughs... and takes EVERYTHING.").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
        } else {
            player.getInventory().offerOrDrop(stack);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1f);
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.05);
            player.sendMessage(Text.literal("Your wish is granted! (" + Math.round(risk * 100) + "% risk)").formatted(Formatting.GOLD), false);
        }
        return 1;
    }
}
