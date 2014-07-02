package ru.tehkode.permissions.backends.file;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import ru.tehkode.permissions.backends.file.config.Node;
import ru.tehkode.permissions.backends.file.config.PEXMLWriter;
import ru.tehkode.permissions.backends.memory.MemoryMatcherList;
import ru.tehkode.permissions.data.Qualifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread-safe data structure that keeps track of all the
 */
public final class FileMatcherList extends MemoryMatcherList<FileMatcherGroup, List<Node>> {
	private List<String> comments;
	private final FileConfig config;

	public FileMatcherList(FileConfig config) {
		comments = Collections.emptyList();
		this.config = config;
		// Empty list
	}

	public FileMatcherList(List<Node> nodes, FileConfig config) throws IOException {
		super(nodes);
		this.config = config;
	}

	// -- Load/save
	protected void load(List<Node> nodes) throws IOException {
		comments = new ArrayList<>();
		for (Node node : nodes) {
			switch (node.getType()) {
				case SECTION:
					Multimap<Qualifier, String> qualifiers = HashMultimap.create();
					Map<String, String> entries = new HashMap<>();
					List<String> comments = new LinkedList<>();
					Multimap<String, String> entryComments = ArrayListMultimap.create();

					for (Node child : node.getChildren()) {
						switch (child.getType()) {
							case QUALIFIER:
								for (Node value : child.getChildren()) {
									switch (value.getType()) {
										case SCALAR:
											qualifiers.put(Qualifier.fromString(child.getKey()), value.getKey());
											break;
										default:
											throw new IOException("Node " + value + " is not supported as a qualifier child");
									}
								}
								break;
							case MAPPING:
								for (Node value : child.getChildren()) {
									switch (value.getType()) {
										case SCALAR:
											entries.put(child.getKey(), value.getKey());
											break;
										case COMMENT:
											entryComments.put(child.getKey(), value.getKey());
											break;
										default:
											throw new IOException("Node " + value + " is not supported as a mapping child");
									}
								}
								break;
							case SCALAR:
								entries.put(child.getKey(), null);
							case COMMENT:
								comments.add(child.getKey());
								break;
							default:
								throw new IOException("Node " + child + " is not supported as a child of a section");
						}
					}

					final AtomicReference<FileMatcherGroup> ref = new AtomicReference<>();
					ref.set(new FileMatcherGroup(node.getKey(), ref, this, qualifiers, entries, comments, entryComments));
					groups.add(ref);
					insertIntoLookup(ref, ref.get());
					break;
				case COMMENT:
					this.comments.add(node.getKey());
					break;
				default:
					throw new IOException("Node " + node + " is not supported as a child of the root");
			}
		}
	}

	public void save() throws IOException {
		this.config.save(this);
	}

	void save(PEXMLWriter writer) throws IOException {
		for (AtomicReference<FileMatcherGroup> ref : groups) {
			FileMatcherGroup group = valFromPtr(ref); // Prevent in-progress changes from being lost

			if (group == null) {
				throw new IllegalArgumentException("Failed to save due to null value in groups list (represents in-progress data change)!");
			}

			List<String> comments = group.getComments();
			if (comments != null) {
				for (String comment : comments) {
					writer.writeComment(comment);
				}
			}
			writer.beginHeader(group.getName());
			for (Map.Entry<Qualifier, String> qual : group.getQualifiers().entries()) {
				writer.writeQualifier(qual.getKey().getName(), qual.getValue());
			}
			writer.endHeader();

			for (Map.Entry<String, String> ent : group.getEntries().entrySet()) {
				if (group.getEntryComments() != null) {
					Collection<String> entryComments = group.getEntryComments().get(ent.getKey());
					if (entryComments != null && !entryComments.isEmpty()) {
						for (String comment : entryComments) {
							writer.writeComment(comment);
						}
					}
				}
				if (ent.getValue() == null) {
					writer.writeListEntry(ent.getKey());
				} else {
					writer.writeMapping(ent.getKey(), ent.getValue());
				}
			}
		}

		if (comments != null) {
			for (String comment : comments) {
				writer.writeComment(comment);
			}
		}
	}

	protected FileMatcherGroup newGroup(AtomicReference<FileMatcherGroup> ptr, String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new FileMatcherGroup(type, ptr, this, qualifiers, entries, null, null);
	}

	@Override
	protected FileMatcherGroup newGroup(AtomicReference<FileMatcherGroup> ptr, String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return new FileMatcherGroup(type, ptr, this, qualifiers, entries, null, null);
	}
}
