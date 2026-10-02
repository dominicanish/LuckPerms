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

import com.google.inject.Inject;
import gg.soju.api.event.EventManager;
import gg.soju.api.event.plugin.PluginSetupEvent;
import gg.soju.api.event.plugin.PluginTeardownEvent;
import gg.soju.api.permission.PermissionProvider;
import gg.soju.api.plugin.Plugin;
import gg.soju.api.plugin.annotation.DataDirectory;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.logging.Slf4jPluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.BuildInfo;
import net.luckperms.api.platform.Platform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Minestom.
 */
@Plugin(
        id = "luckperms",
        name = "LuckPerms",
        version = BuildInfo.VERSION,
        authors = "Luck",
        description = "A permissions plugin",
        url = "https://luckperms.net"
)
public class LPMinestomBootstrap implements LuckPermsBootstrap {

    private final PluginLogger logger;
    private final Path dataDirectory;
    private final SchedulerAdapter schedulerAdapter;
    private final ClassPathAppender classPathAppender;
    private final LPMinestomPlugin plugin;

    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    @Inject
    public LPMinestomBootstrap(final Logger logger, final @DataDirectory Path dataDirectory, final EventManager eventManager) {
        this.logger = new Slf4jPluginLogger(logger);
        this.dataDirectory = dataDirectory.toAbsolutePath();
        this.schedulerAdapter = new MinestomSchedulerAdapter(this);
        this.classPathAppender = new MinestomClassPathAppender();
        this.plugin = new LPMinestomPlugin(this);

        eventManager.eventNode().addListener(PluginSetupEvent.class, _ -> {
            this.enable();

            PermissionProvider.set(((commandSender, permission) -> {
                Sender sender = this.plugin.getSenderFactory().wrap(commandSender);
                return sender.isConsole() || sender.hasPermission(permission);
            }));
        });

        eventManager.eventNode().addListener(PluginTeardownEvent.class, _ -> {
            PermissionProvider.set(PermissionProvider.DEFAULT);
            this.disable();
        });
    }

    // provide adapters

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    // lifecycle

    public void enable() {
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    public void disable() {
        this.plugin.disable();
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return BuildInfo.VERSION;
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.MINESTOM;
    }

    @Override
    public String getServerBrand() {
        return MinecraftServer.getBrandName();
    }

    @Override
    public String getServerVersion() {
        return MinecraftServer.VERSION_NAME;
    }

    @Override
    public Path getDataDirectory() {
        return this.dataDirectory.toAbsolutePath();
    }

    @Override
    public Optional<Player> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        return Optional.ofNullable(MinecraftServer.getConnectionManager().findOnlinePlayer(username))
                .map(Player::getUuid);
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return getPlayer(uniqueId).map(Player::getUsername);
    }

    @Override
    public int getPlayerCount() {
        return MinecraftServer.getConnectionManager().getOnlinePlayerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        return MinecraftServer.getConnectionManager().getOnlinePlayers()
                .stream()
                .map(Player::getUsername)
                .toList();
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        return MinecraftServer.getConnectionManager().getOnlinePlayers()
                .stream()
                .map(Player::getUuid)
                .toList();
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        final Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uniqueId);
        return player != null && player.isOnline();
    }
}