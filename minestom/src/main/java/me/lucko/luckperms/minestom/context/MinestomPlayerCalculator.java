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

package me.lucko.luckperms.minestom.context;

import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.stream.Collectors;

public class MinestomPlayerCalculator implements ContextCalculator<Player> {
    private final Set<ContextProvider> contextProviders;

    public MinestomPlayerCalculator(
            LPMinestomPlugin plugin,
            Set<ContextProvider> contextProviders,
            Set<String> disabled
    ) {
        final EventNode<Event> eventNode = EventNode.all("LuckPerms-ContextProviders");
        plugin.getEventNode().addChild(eventNode);

        this.contextProviders = contextProviders.stream()
                .filter(provider -> !disabled.contains(provider.getKey()))
                .peek(provider -> provider.register(eventNode, plugin.getContextManager()::signalContextUpdate))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void calculate(@NonNull Player subject, @NonNull ContextConsumer c) {
        contextProviders.forEach(p -> p.getContext(subject).ifPresent(v -> c.accept(p.getKey(), v)));
    }

    @Override
    public @NotNull @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        contextProviders.forEach(p -> p.potentialValues().forEach(v -> builder.add(p.getKey(), v)));
        return builder.build();
    }
}