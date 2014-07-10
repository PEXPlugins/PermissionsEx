package ru.tehkode.permissions.backends.yaml;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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

	YamlMatcherGroup(YamlBackend backend, String name, Multimap<Qualifier, String> qualifiers, Map<String, String> entries) {
		super(backend);
		this.name = name;
		this.qualifiers = qualifiers;
		this.entries = Collections.unmodifiableMap(entries);
		this.entriesList = null;
	}

	YamlMatcherGroup(YamlBackend backend, String name, Multimap<Qualifier, String> qualifiers, List<String> entriesList) {
		super(backend);
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

	private <T> ListenableFuture<T> readOnly() {
		return Futures.immediateFailedFuture(new UnsupportedOperationException("YAAML backend is read-only").fillInStackTrace());
	}

	@Override
	protected ListenableFuture<MatcherGroup> setQualifiersImpl(Multimap<Qualifier, String> qualifiers) {
		return readOnly();
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
	protected ListenableFuture<MatcherGroup> setEntriesImpl(Map<String, String> value) {
		return readOnly();
	}

	@Override
	protected ListenableFuture<MatcherGroup> setEntriesImpl(List<String> value) {
		return readOnly();
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	protected ListenableFuture<Boolean> removeImpl() {
		return readOnly();
	}

	@Override
	public String toString() {
		return "YamlMatcherGroup{" +
				"name='" + name + '\'' +
				", qualifiers=" + qualifiers +
				", entries=" + entries +
				", entriesList=" + entriesList +
				'}';
	}
}
