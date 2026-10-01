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

package me.lucko.luckperms.minestom.listeners;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import me.lucko.luckperms.minestom.util.AdventureBridge;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MinestomConnectionListener extends AbstractConnectionListener {
    private final LPMinestomPlugin plugin;

    public MinestomConnectionListener(LPMinestomPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void onPlayerConnect(AsyncPlayerPreLoginEvent event) {
        /* wait for the plugin to enable. because these events are fired async, they can be called before
           the plugin has enabled.  */
        try {
            this.plugin.getBootstrap().getEnableLatch().await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            this.plugin.getLogger().severe("An error occurring while processing a pre-login event", ex);
        }

        final UUID uuid = event.getGameProfile().uuid();
        final String name = event.getGameProfile().name();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + uuid + " - " + name);
        }

        if (!event.getConnection().isOnline()) {
            // Already kicked
            this.plugin.getLogger()
                    .info("An external source has cancelled the connection for " + uuid + " - " + name +
                            ". No permissions data will be loaded.");
            return;
        }

        try {
            User user = loadUser(uuid, name);
            recordConnection(uuid);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uuid, name, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uuid + " - " + name, ex);

            Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            AdventureBridge.INSTANCE.kick(event.getConnection(), reason);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uuid, name, null);
        }
    }

    public void onPlayerPostLogin(AsyncPlayerConfigurationEvent event) {
        final Player player = event.getPlayer();
        final User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger()
                    .info("Processing post-login for " + player.getUuid() + " - " + player.getUsername());
        }

        handleLoggedIn(player.getUuid());

        if (user == null) {
            if (!getUniqueConnections().contains(player.getUuid())) {
                this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getUsername() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this " +
                        "session.");
            } else {
                this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getUsername() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session" +
                        ".");
            }

            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // disconnect the user
                AdventureBridge.INSTANCE.kick(player.getPlayerConnection(),
                        TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getLocale()));
            } else {
                // just send a message
                this.plugin.getBootstrap().getScheduler().asyncLater(() -> {
                    if (!player.isActive()) {
                        return;
                    }

                    Message.LOADING_STATE_ERROR.send(this.plugin.getSenderFactory().wrap(player));
                }, 1, TimeUnit.SECONDS);
            }
        }
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        handleDisconnect(event.getPlayer().getUuid());
    }
}