package ru.tehkode.permissions.backends.file;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import ru.tehkode.permissions.backends.memory.AbstractMemoryBackend;
import ru.tehkode.permissions.backends.memory.MemoryMatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Memory matcher group supporting additional matcher data from files
 */
public final class FileMatcherGroup extends MemoryMatcherGroup<FileMatcherGroup> {
	private static final Logger LOGGER = Logger.getLogger(FileMatcherGroup.class.getCanonicalName());
	private final List<String> comments;
	private final Multimap<String, String> entryComments;

	FileMatcherGroup(String name, AbstractMemoryBackend<FileMatcherGroup> backend, Multimap<Qualifier, String> qualifiers, Map<String, String> entries, List<String> comments, Multimap<String, String> entryComments) {
		super(name, backend, qualifiers, entries);
		this.comments = comments == null ? null : Collections.unmodifiableList(comments);
		this.entryComments = entryComments == null ? null : ImmutableMultimap.copyOf(entryComments);
	}

	FileMatcherGroup(String name, AbstractMemoryBackend<FileMatcherGroup> backend, Multimap<Qualifier, String> qualifiers, List<String> entriesList, List<String> comments, Multimap<String, String> entryComments) {
		super(name, backend, qualifiers, entriesList);
		this.comments = comments == null ? null : Collections.unmodifiableList(comments);
		this.entryComments = entryComments == null ? null : ImmutableMultimap.copyOf(entryComments);
	}

	public List<String> getComments() {
		return comments;
	}

	public Multimap<String, String> getEntryComments() {
		return entryComments;
	}

	@Override
	protected FileMatcherGroup newSelf(Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new FileMatcherGroup(getName(), backend, qualifiers, entries, getComments(), getEntryComments());
	}

	@Override
	protected FileMatcherGroup newSelf(List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return new FileMatcherGroup(getName(), backend, qualifiers, entries, getComments(), getEntryComments());
	}

	@Override
	public String toString() {
		return "FileMatcherGroup{name=" + getName()
				+ ",valid=" + isValid()
				+ ",backend=" + backend
				+ ",entries=" + getEntries()
				+ ",entriesList=" + getEntriesList()
				+ ",qualifiers=" + getQualifiers()
				+ ",comments=" + getComments()
				+ ",entryComments=" + getEntryComments()
				+ "}";
	}
}
