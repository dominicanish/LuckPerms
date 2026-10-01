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

package me.lucko.luckperms.minestom.messaging;

import com.google.common.collect.Iterables;
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.timer.TaskSchedule;

import java.util.Collection;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger extends AbstractPluginMessageMessenger {

    private final LPMinestomPlugin plugin;
    private EventListener<PlayerPluginMessageEvent> eventListener;

    public PluginMessageMessenger(LPMinestomPlugin plugin, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    public void init() {
        this.eventListener = EventListener.of(PlayerPluginMessageEvent.class, this::onPluginMessage);
        plugin.getEventNode().addListener(this.eventListener);
    }

    @Override
    public void close() {
        if (eventListener == null) {
            throw new IllegalStateException("PluginMessageMessenger is not initialized");
        }

        plugin.getEventNode().removeListener(eventListener);
    }

    @Override
    protected void sendOutgoingMessage(byte[] buf) {
        MinecraftServer.getSchedulerManager().submitTask(() -> {
            Collection<Player> players = MinecraftServer.getConnectionManager().getOnlinePlayers();
            Player player = Iterables.getFirst(players, null);
            if (player == null) {
                return TaskSchedule.seconds(5L);
            }

            player.sendPluginMessage(CHANNEL, buf);
            return TaskSchedule.stop();
        });
    }

    public void onPluginMessage(PlayerPluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }

        handleIncomingMessage(event.getMessage());
    }
}