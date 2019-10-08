/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.permissionsex.util;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An immutable tree structure for determining node data. Any changes will create new copies of the necessary tree objects.
 * Keys are case-insensitive.
 * Segments of nodes are split by the '.' character
 */
public class NodeTree {
    public static final int PERMISSION_UNDEFINED = 0;

    private static final Pattern SPLIT_REGEX = Pattern.compile("\\.");
    private final Node rootNode;


    private NodeTree(int value) {
        this.rootNode = new Node(new HashMap<>());
        this.rootNode.value = value;
    }

    private NodeTree(Node rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Create a new node tree with the given values, and a default value of UNDEFINED.
     *
     * @param values The values to set
     * @return The new node tree
     */
    public static NodeTree of(Map<String, Integer> values) {
        return of(values, PERMISSION_UNDEFINED);
    }

    /**
     * Create a new node tree with the given values, and the specified root fallback value.
     *
     * @param values The values to be contained in this node tree
     * @param defaultValue The fallback value for any completely undefined nodes
     * @return The newly created node tree
     */
    public static NodeTree of(Map<String, Integer> values, int defaultValue) {
        NodeTree newTree = new NodeTree(defaultValue);
        for (Map.Entry<String, Integer> value : values.entrySet()) {
            String[] parts = SPLIT_REGEX.split(value.getKey().toLowerCase());
            Node currentNode = newTree.rootNode;
            for (String part : parts) {
                if (currentNode.children.containsKey(part)) {
                    currentNode = currentNode.children.get(part);
                } else {
                    Node newNode = new Node(new HashMap<>());
                    currentNode.children.put(part, newNode);
                    currentNode = newNode;
                }
            }
            currentNode.value = value.getValue();
        }
        return newTree;
    }

    /**
     * Returns the value assigned to a specific node, or the nearest parent value in the tree if the node itself is undefined.
     *
     * @param node The path to get the node value at
     * @return The tristate value for the given node
     */
    public int get(String node) {
        String[] parts = SPLIT_REGEX.split(node.toLowerCase());
        Node currentNode = this.rootNode;
        int lastUndefinedVal = this.rootNode.value;
        for (String str : parts) {
            if (!currentNode.children.containsKey(str)) {
                break;
            }
            currentNode = currentNode.children.get(str);
            if (Math.abs(currentNode.value) >= Math.abs(lastUndefinedVal)) {
                lastUndefinedVal = currentNode.value;
            }
        }
        return lastUndefinedVal;

    }

    /**
     * Convert this node tree into a map of the defined nodes in this tree.
     *
     * @return An immutable map representation of the nodes defined in this tree
     */
    public Map<String, Integer> asMap() {
        ImmutableMap.Builder<String, Integer> ret = ImmutableMap.builder();
        for (Map.Entry<String, Node> ent : this.rootNode.children.entrySet()) {
            populateMap(ret, ent.getKey(), ent.getValue());
        }
        return ret.build();
    }

    private void populateMap(ImmutableMap.Builder<String, Integer> values, String prefix, Node currentNode) {
        if (currentNode.value != 0) {
            values.put(prefix, currentNode.value);
        }
        for (Map.Entry<String, Node> ent : currentNode.children.entrySet()) {
            populateMap(values, prefix + '.' + ent.getKey(), ent.getValue());
        }
    }

    /**
     * Return a new NodeTree instance with a single changed value.
     *
     * @param node The node path to change the value of
     * @param value The value to change, or UNDEFINED to remove
     * @return The new, modified node tree
     */
    public NodeTree withValue(String node, int value) {
        String[] parts = SPLIT_REGEX.split(node.toLowerCase());
        Node newRoot = new Node(new HashMap<>(this.rootNode.children));
        Node newPtr = newRoot;
        Node currentPtr = this.rootNode;

        newPtr.value = currentPtr == null ? 0 : currentPtr.value;
        for (String part : parts) {
            Node oldChild = currentPtr == null ? null : currentPtr.children.get(part);
            Node newChild = new Node(oldChild != null ? new HashMap<>(oldChild.children) : new HashMap<String, Node>());
            newPtr.children.put(part, newChild);
            currentPtr = oldChild;
            newPtr = newChild;
        }
        newPtr.value = value;
        return new NodeTree(newRoot);
    }

    /**
     * Return a modified new node tree with the specified values set.
     *
     * @param values The values to set
     * @return The new node tree
     */
    public NodeTree withAll(Map<String, Integer> values) {
        NodeTree ret = this;
        for (Map.Entry<String, Integer> ent : values.entrySet()) {
            ret = ret.withValue(ent.getKey(), ent.getValue());
        }
        return ret;
    }

    private static class Node {

        private final Map<String, Node> children;
        private int value = 0;

        private Node(Map<String, Node> children) {
            this.children = children;
        }
    }
}
