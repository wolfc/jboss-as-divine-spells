/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.divine.spells;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class Node {
    private String name;
    private final List<Node> children = new ArrayList<Node>();
    private final ThreadLocal<Boolean> done = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    Node(final String name) {
        this.name = name;
    }

    boolean addChild(final Node child) {
        assert child != null : "child is null";
        return children.add(child);
    }

    boolean done() {
        boolean d = done.get();
        done.set(true);
        return d;
    }

    Iterable<Node> getChildren() {
        return children;
    }

    String getName() {
        return name;
    }

    boolean hasChildren() {
        return !children.isEmpty();
    }

    void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " " + children;
    }
}
