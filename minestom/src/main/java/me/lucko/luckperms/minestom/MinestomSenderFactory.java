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

import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.minestom.util.AdventureBridge;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class MinestomSenderFactory extends SenderFactory<LPMinestomPlugin, CommandSender> {
    public MinestomSenderFactory(LPMinestomPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(CommandSender source) {
        return source instanceof Player player ? player.getUsername() : Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSender source) {
        return source instanceof Player player ? player.getUuid() : Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender source, Component message) {
        Locale locale = source instanceof Player player ? player.getLocale() : null;
        Component rendered = TranslationManager.render(message, locale);
        AdventureBridge.INSTANCE.sendMessage(source, rendered);
    }

    @Override
    protected Tristate getPermissionValue(CommandSender source, String node) {
        return source instanceof Player player ? getPlugin().getApiProvider()
                .getPlayerAdapter(Player.class)
                .getPermissionData(player)
                .checkPermission(node) : Tristate.TRUE;
    }

    @Override
    protected boolean hasPermission(CommandSender source, String node) {
        return !(source instanceof Player player) || getPlugin().getApiProvider()
                .getPlayerAdapter(Player.class)
                .getPermissionData(player)
                .checkPermission(node).asBoolean();
    }

    @Override
    protected void performCommand(CommandSender source, String command) {
        MinecraftServer.getCommandManager().execute(source, command);
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleSender;
    }
}