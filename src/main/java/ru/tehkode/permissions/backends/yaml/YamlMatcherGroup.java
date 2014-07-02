package ru.tehkode.permissions.backends.yaml;

import com.google.common.collect.Multimap;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Super-simple matcher group for the yaml backend
 */
public final class YamlMatcherGroup extends MatcherGroup {
	private final String name;
	private final Multimap<Qualifier, String> qualifiers;
	private final Map<String, String> entries;
	private final List<String> entriesList;

	YamlMatcherGroup(String name, Multimap<Qualifier, String> qualifiers, Map<String, String> entries) {
		this.name = name;
		this.qualifiers = qualifiers;
		this.entries = Collections.unmodifiableMap(entries);
		this.entriesList = null;
	}

	YamlMatcherGroup(String name, Multimap<Qualifier, String> qualifiers, List<String> entriesList) {
		this.name = name;
		this.qualifiers = qualifiers;
		this.entriesList = Collections.unmodifiableList(entriesList);
		this.entries = null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Multimap<Qualifier, String> getQualifiers() {
		return qualifiers;
	}

	@Override
	public MatcherGroup setQualifiers(Multimap<Qualifier, String> qualifiers) {
		throw new UnsupportedOperationException("YAML backend is read-only");
	}

	@Override
	public Map<String, String> getEntries() {
		return entries;
	}

	@Override
	public List<String> getEntriesList() {
		return entriesList;
	}

	@Override
	public MatcherGroup setEntries(Map<String, String> value) {
		throw new UnsupportedOperationException("YAML backend is read-only");
	}

	@Override
	public MatcherGroup setEntries(List<String> value) {
		throw new UnsupportedOperationException("YAML backend is read-only");
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public boolean remove() {
		throw new UnsupportedOperationException("YAML backend is read-only");
	}
}
