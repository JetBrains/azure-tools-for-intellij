/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Accessors(chain = true, fluent = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node<D> {
    @Nonnull
    @EqualsAndHashCode.Include
    private final D data;
    @Nonnull
    @Getter(AccessLevel.NONE)
    private final List<ChildrenNodeBuilder<D, ?>> childrenBuilders = new ArrayList<>();
    @Nonnull
    @Setter
    private NodeView view;
    @Setter
    private boolean lazy = true;
    @Nullable
    private IActionGroup actions;
    private int order;
    @Getter
    private Order newItemOrder = Order.LIST_ORDER;
    private Action<? super D> clickAction;
    private Action<? super D> doubleClickAction;
    private final List<Action<? super D>> inlineActionList = new ArrayList<>();
    private Consumer<? super D> loadMoreChildren;
    // by default, we will load all children, so return false for has more child
    @Getter(AccessLevel.NONE)
    private Predicate<? super D> hasMoreChildren = ignore -> false;

    public Node(@Nonnull D data) {
        this(data, null);
    }

    public Node(@Nonnull D data, @Nonnull NodeView view) {
        this.data = data;
        this.view = view;
    }

    public <C> Node<D> addChildren(
        @Nonnull Function<? super D, ? extends List<C>> getChildrenData,
        @Nonnull BiFunction<C, Node<D>, Node<?>> buildChildNode) {
        this.childrenBuilders.add(new ChildrenNodeBuilder<>(getChildrenData, buildChildNode));
        return this;
    }

    public <C> Node<D> addChild(
        @Nonnull Function<? super D, ? extends C> getChildData,
        @Nonnull BiFunction<C, Node<D>, Node<?>> buildChildNode) {
        this.childrenBuilders.add(new ChildrenNodeBuilder<>(d -> Collections.singletonList(getChildData.apply(d)), buildChildNode));
        return this;
    }

    public Node<D> addChild(@Nonnull Function<? super Node<D>, ? extends Node<?>> buildChildNode) {
        return this.addChildren((d) -> Collections.singletonList(null), (cd, n) -> buildChildNode.apply(n));
    }

    public Node<D> addChildren(@Nonnull List<Node<?>> children) {
        return this.addChildren((d) -> children, (cd, n) -> cd);
    }

    public Node<D> addChildren(@Nonnull Function<? super D, ? extends List<Node<?>>> getChildrenNodes) {
        return this.addChildren(getChildrenNodes, (cd, n) -> cd);
    }

    public Node<D> addChild(@Nonnull Node<?> childNode) {
        return this.addChildren(Collections.singletonList(childNode));
    }

    public List<Node<?>> getChildren() {
        return this.childrenBuilders.stream().flatMap((builder) -> {
            try {
                return builder.build(this).stream();
            } catch (final Exception e) {
                AzureMessager.getMessager().error(e);
                return Stream.empty();
            }
        }).collect(Collectors.toList());
    }

    public boolean hasChildren() {
        return !this.childrenBuilders.isEmpty();
    }

    public boolean hasMoreChildren() {
        return this.hasMoreChildren.test(this.data);
    }

    public Node<D> hasMoreChildren(Predicate<? super D> hasMoreChildren) {
        this.hasMoreChildren = hasMoreChildren;
        return this;
    }

    public void loadMoreChildren() {
        this.loadMoreChildren.accept(this.data);
    }

    public Node<D> loadMoreChildren(Consumer<? super D> loadMoreChildren) {
        this.loadMoreChildren = loadMoreChildren;
        return this;
    }

    public boolean hasClickAction() {
        return this.clickAction != null;
    }

    public void triggerClickAction(final Object event) {
        if (this.clickAction != null) {
            this.clickAction.handle(this.data, event);
        }
    }

    public Node<D> clickAction(Action.Id<? super D> actionId) {
        this.clickAction = AzureActionManager.getInstance().getAction(actionId);
        return this;
    }

    public void triggerDoubleClickAction(final Object event) {
        if (this.doubleClickAction != null) {
            this.doubleClickAction.handle(this.data, event);
        }
    }

    public Node<D> doubleClickAction(Action.Id<? super D> actionId) {
        this.doubleClickAction = AzureActionManager.getInstance().getAction(actionId);
        return this;
    }

    public void triggerInlineAction(final Object event, int index) {
        final List<Action<? super D>> enabledActions =  this.inlineActionList.stream()
                .filter(action -> action.getView(this.data).isEnabled()).collect(Collectors.toList());
        if (index >=0 && index < enabledActions.size()) {
            Optional.ofNullable(enabledActions.get(index)).ifPresent(a -> a.handle(this.data, event));
        }
    }

    public Node<D> addInlineAction(Action.Id<? super D> actionId) {
        this.inlineActionList.add(AzureActionManager.getInstance().getAction(actionId));
        return this;
    }

    public Node<D> actions(String groupId) {
        return this.actions(AzureActionManager.getInstance().getGroup(groupId));
    }

    public Node<D> actions(IActionGroup group) {
        this.actions = group;
        return this;
    }

    public Node<D> newItemsOrder(@Nonnull final Order order) {
        this.newItemOrder = order;
        return this;
    }

    public void dispose() {
        this.view.dispose();
    }

    @RequiredArgsConstructor
    private static class ChildrenNodeBuilder<D, C> {
        private final Function<? super D, ? extends List<C>> getChildrenData;
        private final BiFunction<C, Node<D>, Node<?>> buildChildNode;

        private List<Node<?>> build(Node<D> n) {
            final val childrenData = this.getChildrenData.apply(n.data);
            return childrenData.stream().map(d -> buildChildNode.apply(d, n)).collect(Collectors.toList());
        }
    }

    public enum Order {
        LIST_ORDER,
        INSERT_ORDER
    }
}