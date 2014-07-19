package ru.tehkode.permissions.data;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

/**
 * Simple implementation of context, primarily designed for usage as a hash key.
 */
public class StaticContext implements Context {
	private final ImmutableMultimap<Qualifier, String> values;

	private StaticContext(ImmutableMultimap<Qualifier, String> values) {
		this.values = values;
	}

	@Override
	public Collection<String> getValues(Qualifier qual) {
		return values.get(qual);
	}

	@Override
	public boolean hasValue(Qualifier qual) {
		return values.containsKey(qual);
	}

	@Override
	public ImmutableMultimap<Qualifier, String> getValues() {
		return values;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StaticContext that = (StaticContext) o;

		return values.equals(that.values);
	}

	@Override
	public int hashCode() {
		return 31 + values.hashCode();
	}

	@Override
	public String toString() {
		return "StaticContext{" +
				"values=" + values +
				'}';
	}

	public static StaticContext of(Context context) {
		return new StaticContext(context.getValues());
	}

	public static StaticContext of(Multimap<Qualifier, String> values) {
		return new StaticContext(ImmutableMultimap.copyOf(values));
	}

	public static StaticContext of(Qualifier key, String value) {
		return new StaticContext(ImmutableMultimap.of(key, value));
	}
}
