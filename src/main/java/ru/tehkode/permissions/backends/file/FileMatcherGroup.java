package ru.tehkode.permissions.backends.file;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import ru.tehkode.permissions.backends.memory.MemoryMatcherGroup;
import ru.tehkode.permissions.backends.memory.MemoryMatcherList;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Memory matcher group supporting additional matcher data from files
 */
public class FileMatcherGroup extends MemoryMatcherGroup<FileMatcherGroup> {
	private static final Logger LOGGER = Logger.getLogger(FileMatcherGroup.class.getCanonicalName());
	private final List<String> comments;
	private final Multimap<String, String> entryComments;

	protected FileMatcherGroup(String name, AtomicReference<FileMatcherGroup> selfRef, MemoryMatcherList<FileMatcherGroup, ?> listRef, Multimap<Qualifier, String> qualifiers, Map<String, String> entries, List<String> comments, Multimap<String, String> entryComments) {
		super(name, selfRef, listRef, qualifiers, entries);
		this.comments = comments == null ? Collections.<String>emptyList() : Collections.unmodifiableList(comments);
		this.entryComments = entryComments == null ? ImmutableMultimap.<String, String>of() : ImmutableMultimap.copyOf(entryComments);
	}

	protected FileMatcherGroup(String name, AtomicReference<FileMatcherGroup> selfRef, MemoryMatcherList<FileMatcherGroup, ?> listRef, Multimap<Qualifier, String> qualifiers, List<String> entriesList, List<String> comments, Multimap<String, String> entryComments) {
		super(name, selfRef, listRef, qualifiers, entriesList);
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
	public MatcherGroup setQualifiers(Multimap<Qualifier, String> qualifiers) {
		MatcherGroup ret = super.setQualifiers(qualifiers);
		save();
		return ret;
	}

	@Override
	public MatcherGroup setEntries(Map<String, String> entries) {
		MatcherGroup ret = super.setEntries(entries);
		save();
		return ret;
	}

	private void save() {
		try {
			listRef.save();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error while saving for group " + this, e);
		}
	}

	@Override
	protected FileMatcherGroup newSelf(Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new FileMatcherGroup(getName(), selfRef, listRef, qualifiers, entries, getComments(), getEntryComments());
	}

	@Override
	public String toString() {
		return "FileMatcherGroup{name=" + getName()
				+ ",selfRef=" + selfRef
				+ ",listRef=" + listRef
				+ ",entries=" + getEntries()
				+ ",qualifiers=" + getQualifiers()
				+ ",comments=" + getComments()
				+ ",entryComments=" + getEntryComments()
				+ "}";
	}
}
