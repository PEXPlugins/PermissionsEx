package ru.tehkode.permissions.backends.file.config;

import org.parboiled.trees.ImmutableTreeNode;
import ru.tehkode.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a node in the pex config AST
 */
public class Node extends ImmutableTreeNode<Node> {
	public static enum Type {
		MAPPING,
		SCALAR,
		QUALIFIER,
		COMMENT,
		ROOT,
		SECTION
	}
	private final String key;
	private final Type type;

	public Node(String key, Type type, List<Node> children) {
		super(children);
		this.key = key;
		this.type = type;
	}

	public Node(String key, Type type, Node... children) {
		this(key, type, Arrays.asList(children));
	}

	public String getKey() {
		return key;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return printRecurs(0);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Node)) {
			return false;
		}
		Node other = (Node) o;
		return other.key.equals(this.key)
				&& other.type.equals(this.type)
				&& other.getChildren().equals(this.getChildren());
	}

	public String printRecurs(int indent) {
		StringBuilder build = new StringBuilder(StringUtils.repeat("    ", indent)).append("Node{")
				.append("key='").append(key).append('\'')
				.append(", type=").append(type).append("}\n");
		for (Node child : getChildren()) {
			build.append(child.printRecurs(indent + 1));
		}
		return build.toString();
	}
}
