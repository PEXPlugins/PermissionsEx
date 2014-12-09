package com.permissionsex.permissions.data;

import org.spongepowered.api.util.Tristate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Data structure representing a permissions node
 *
 * This class is thread-safe because its contents are never modified after initialization.
 * This behaviour should be preserved in future modifications.
 */
public class NodeTree {
	private static final Pattern DOT_SPLIT_PATTERN = Pattern.compile("\\.");
	private static final Tristate DEFAULT_VALUE = Tristate.UNDEFINED;
	private final Node rootNode;

	private static class Node {
		private Map<String, Node> children;
		private Tristate value;

		private Node(Tristate value) {
			this.value = value;
		}

		public boolean hasChild(String child) {
			return children != null && children.containsKey(child);
		}

		public Node getChild(String child) {
			return children == null ? null : children.get(child);
		}

		public Node getAddChild(String child, Tristate value) {
			if (children == null) {
				children = new HashMap<>();
			}
			if (!children.containsKey(child)) {
				Node res = new Node(value == null ? Tristate.UNDEFINED : value);
				children.put(child, res);
				return res;
			} else {
				Node res = children.get(child);
				if (value != null) {
					res.value = value;
				}
				return res;
			}
		}
	}

	private NodeTree(Tristate defaultValue) {
		this.rootNode = new Node(defaultValue);
	}

	public Tristate getValue(String node) {
		String[] nodeSegments = DOT_SPLIT_PATTERN.split(node);
		Node ptr = rootNode;
		Tristate lastDefinedValue = ptr.value;
		for (int i = 0; i < nodeSegments.length && ptr.hasChild(nodeSegments[i]); ++i) {
			ptr = ptr.getChild(nodeSegments[i]);
			if (ptr.value != Tristate.UNDEFINED) {
				lastDefinedValue = ptr.value;
			}
		}
		return lastDefinedValue;
	}

	public static NodeTree fromMap(Map<String, Tristate> map) {
		return fromMap(map, DEFAULT_VALUE);
	}

	public static NodeTree fromMap(Map<String, Tristate> map, Tristate defaultValue) {
		NodeTree tree = new NodeTree(defaultValue);
		for (Map.Entry<String, Tristate> ent : map.entrySet()) {
			String[] segments = DOT_SPLIT_PATTERN.split(ent.getKey());
			Node ptr = tree.rootNode;
			for (int i = 0; i < segments.length; ++i) {
				ptr = ptr.getAddChild(segments[i], i == segments.length - 1 ? ent.getValue() : Tristate.UNDEFINED);
			}
		}
		return tree;
	}
}
