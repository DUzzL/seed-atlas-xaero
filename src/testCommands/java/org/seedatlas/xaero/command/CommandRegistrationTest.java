package org.seedatlas.xaero.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Exercises shared command registration with a source type supplied by a loader. */
public final class CommandRegistrationTest {
    public static void main(String[] args) throws Exception {
        CommandDispatcher<Boolean> dispatcher = new CommandDispatcher<>();
        SeedAtlasCommands.register(dispatcher, Source::new);
        for (String command : new String[]{"seedatlas", "seedatlas status", "seedatlas clear", "seedatlas settings"}) {
            var parsed = dispatcher.parse(command, true);
            check(!parsed.getReader().canRead() && parsed.getContext().getCommand() != null,
                "Expected executable client command: " + command);
        }
        for (String seed : new String[]{"-9223372036854775808", "some seed with spaces", "123"}) {
            var parsed = dispatcher.parse("seedatlas seed " + seed, true);
            check(!parsed.getReader().canRead(), "Seed input must be fully consumed");
            check(StringArgumentType.getString(parsed.getContext().build("seedatlas seed " + seed), "seed").equals(seed),
                "Seed text must survive the loader adapter");
        }
        check(!dispatcher.parse("seedatlas status", false).getReader().canRead(), "Status is always available");
        for (String command : new String[]{"seedatlas clear", "seedatlas settings", "seedatlas seed 42"}) {
            check(dispatcher.parse(command, false).getReader().canRead(),
                "World-changing commands need an attended client: " + command);
        }
        check(dispatcher.parse("seedatlas seed", true).getContext().getCommand() == null,
            "Setting a seed requires an argument");
        System.out.println("Shared client command registration tests passed");
    }

    private record Source(boolean attended) implements SeedAtlasCommandSource {
        @Override public Minecraft getClient() { throw new AssertionError("Parsing must not access Minecraft"); }
        @Override public void sendFeedback(Component message) { throw new AssertionError("Parsing must not send chat"); }
        @Override public void sendError(Component message) { throw new AssertionError("Parsing must not send chat"); }
    }
    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
