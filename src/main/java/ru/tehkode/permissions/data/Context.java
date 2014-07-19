package ru.tehkode.permissions.data;

import com.google.common.collect.ImmutableMultimap;

import java.util.Collection;

/**
 * The context used when determining whether a matcher is valid for a given query
 */
public interface Context {
	/**
	 * Returns the value specified for a qualifier if any
	 *
	 * @param qual The qualifier to get a value for, if any
	 * @return The values for the qualifier. Should return an empty collection if no values
	 */
	public Collection<String> getValues(Qualifier qual);

	/**
	 * Returns whether or not this context has a value for the specified qualifier
	 * @param qual The qualifier to check
	 * @return Whether a value is specified for this qualifier
	 */
	public boolean hasValue(Qualifier qual);

	/**
	 * Return all defined values for qualifiers
	 * @return The defined qualifier values
	 */
	public ImmutableMultimap<Qualifier, String> getValues();
}

