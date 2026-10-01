/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.List;

public class MinestomCommandExecutor extends CommandManager {
    /* The command aliases */
    private static final String PRIMARY_ALIAS = "luckperms";
    private static final String[] ALIASES = {"lp"};

    /* The command aliases, prefixed with '/' */
    private static final String SLASH_PRIMARY_ALIAS = "/luckperms";
    private static final String[] SLASH_ALIASES = Arrays.stream(ALIASES).map(s -> '/' + s).toArray(String[]::new);

    private final LPMinestomPlugin plugin;

    public MinestomCommandExecutor(LPMinestomPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        MinecraftServer.getCommandManager().register(new LuckPermsCommand());
    }

    private class LuckPermsCommand extends Command {

        private LuckPermsCommand() {
            super(PRIMARY_ALIAS, ALIASES);

            LuckPermsArgument argument = new LuckPermsArgument("args");

            addSyntax(this::execute, argument);
            setDefaultExecutor(this::execute);
        }

        private void execute(CommandSender sender, CommandContext context) {
            Sender wrapped = plugin.getSenderFactory().wrap(sender);
            String input = context.getInput().contains(" ") ? context.get("args") : "";

            List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(input);
            executeCommand(wrapped, context.getCommandName(), arguments);
        }
    }

    @NullMarked
    private final class LuckPermsArgument extends Argument<String> {
        private LuckPermsArgument(String id) {
            super(id, true, true);

            setSuggestionCallback((sender, context, suggestion) -> {
                Sender wrapped = plugin.getSenderFactory().wrap(sender);
                String input = context.getInput().contains(" ") ? context.get("args") : "";

                List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(input);
                tabCompleteCommand(wrapped, arguments).forEach(s -> suggestion.addEntry(new SuggestionEntry(s)));
            });
        }

        @Override
        public String parse(CommandSender sender, String input) throws ArgumentSyntaxException {
            return input;
        }

        @Override
        public byte @Nullable [] nodeProperties() {
            return NetworkBuffer.makeArray(NetworkBuffer.VAR_INT, 2); // Greedy phrase
        }

        @Override
        public ArgumentParserType parser() {
            return ArgumentParserType.STRING;
        }
    }
}