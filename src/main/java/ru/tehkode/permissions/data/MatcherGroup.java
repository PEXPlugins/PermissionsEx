package ru.tehkode.permissions.data;

import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a matcher group. This object is immutable.
 *
 * If a matcher group is changed, it becomes invalid (meaning that write operations will return null).
 * The latest values must be fetched again from the backend in that case.
 */
public abstract class MatcherGroup implements Comparable<MatcherGroup> {
	public static final String PERMISSIONS_KEY = "permissions",
								OPTIONS_KEY = "options",
								INHERITANCE_KEY = "inheritance",
								WORLD_INHERITANCE_KEY = "world-inheritance",
								GENERAL_KEY = "general",
								UUID_ALIASES_KEY = "uuid-aliases";
	/**
	 * Return the name of this matcher group
	 *
	 * @return
	 */
	public abstract String getName();

	/**
	 * Return a list of qualifiers associated with this match group.
	 *
	 * @return The qualifiers associated with this group (unmodifiable)
	 */
	public abstract Multimap<Qualifier, String> getQualifiers();

	/**
	 * Set the qualifiers in this group, returning an updated group object.
	 *
	 * @param qualifiers The qualifiers to set for this group.
	 * @return an updated group object
	 */
	public abstract MatcherGroup setQualifiers(Multimap<Qualifier, String> qualifiers);

	/**
	 * Return the entries associated with this group.
	 * If this group's values are a list, the keys represent the entries in the list and the values are null.
	 * @return
	 */
	public abstract Map<String, String> getEntries();

	public List<String> getEntriesList() {
		return new ArrayList<>(getEntries().keySet());
	}

	public abstract MatcherGroup setEntries(Map<String, String> value);

	public MatcherGroup putEntry(String key, String value) {
		Map<String, String> newEntries = new LinkedHashMap<>(getEntries());
		newEntries.put(key, value);
		return setEntries(newEntries);
	}


	public boolean matches(Context context) {
		int matches = 0;
		for (Map.Entry<Qualifier, String> ent : getQualifiers().entries()) {
			if ((matches & ~ent.getKey().getFlag()) != 0) { // We have already matched a qualifier of this type, don't need to check again
				continue;
			}
			if (ent.getKey().matches(context, ent.getValue())) {
				matches |= ent.getKey().getFlag();
			}
		}
		return getQualifierTypeMask() == matches; // same flags are masked as were expected to be matched
	}

	/**
	 * Returns if this matcher group is valid.
	 * A matcher group object becomes invalid if its entries or qualifiers have changed or it has been removed.
	 *
	 * @return Whether this object is valid
	 */
	public abstract boolean isValid();

	/**
	 * Removes this matcher group from the listing
	 *
	 * @return true if matcher was removed, false if {@link #isValid()} returns false
	 */
	public abstract boolean remove();

	public int getQualifierTypeMask() {
		int ret = 0;
		for (Qualifier qualifier : getQualifiers().keySet()) {
			ret |= qualifier.getFlag();
		}
		return ret;
	}

	/**
	 * Sorts by weighting of qualifier types
	 * This is inconsistent with equals
	 */
	@Override
	public int compareTo(MatcherGroup o) {
		final int me = getQualifierTypeMask();
		final int other = o.getQualifierTypeMask();
		return me == other ? 0 :
				me > other ? 1 : -1;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MatcherGroup)) {
			return false;
		}
		MatcherGroup group = (MatcherGroup) other;
		return getName().equals(group.getName())
				&& getQualifiers().equals(group.getQualifiers())
				&& getEntries().equals(group.getEntries());
	}


}
