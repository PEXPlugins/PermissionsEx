package ru.tehkode.permissions.data;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
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
	 * Constants for common values are the _KEY strings of {@link MatcherGroup}
	 * @return this group's name
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
	public abstract ListenableFuture<MatcherGroup> setQualifiers(Multimap<Qualifier, String> qualifiers);

	/**
	 * Return the entries associated with this group.
	 * If this group's values are a list, this method will return null.
	 *
	 * @return the immutable result.
	 */
	public abstract Map<String, String> getEntries();

	/**
	 * @return whether or not the data contained in this group is a map.
	 */
	public boolean isMap() {
		return getEntries() != null;
	}

	/**
	 * Returns the list of entries associated with this group.
	 * If this group's values are mappings, this method will return null.
	 *
	 * @return the immutable result.
	 */
	public abstract List<String> getEntriesList();

	/**
	 *
	 * @return whether not the data contained in this group is a list.
	 */
	public boolean isList() {
		return getEntriesList() != null;
	}

	/**
	 * Sets this group's entries to a map.
	 * An immutable copy of the provided map will be made for storage.
	 * Values must not be null.
	 *
	 * @param value the value to set as entries.
	 * @return the new matcher group object, or null if this group's already invalid
	 */
	public abstract ListenableFuture<MatcherGroup> setEntries(Map<String, String> value);

	/**
	 * Sets this group's entries as a list.
	 * An immutable copy of the provided list will be made for storage.
	 *
	 * @param value The values to set as entries.
	 * @return the new matcher group object, or null if this group's already invalid.
	 */
	public abstract ListenableFuture<MatcherGroup> setEntries(List<String> value);

	/**
	 * Adds an entry to the current mapping of entries.
	 *
	 * @see #setEntries(Map) for error conditions.
	 * @param key The key to add
	 * @param value The value to add
	 * @return null if the current value is not a map, otherwise the result of {@link #setEntries(Map)}.
	 */
	public ListenableFuture<MatcherGroup> putEntry(String key, String value) {
		Map<String, String> entries = getEntries();
		if (getEntries() == null) {
			return Futures.immediateFailedFuture(new IllegalStateException("Group is not a map!").fillInStackTrace());
		}
		Map<String, String> newEntries = new HashMap<>(entries);
		newEntries.put(key, value);
		return setEntries(newEntries);
	}

	/**
	 * Adds an entry to the current list of entries
	 *
	 * @see #setEntries(List) for error conditions
	 * @param value The value to add
	 * @return future with IllegalStateException if the current value is not a list, otherwise the result of {@link #setEntries(List)}
	 */
	public ListenableFuture<MatcherGroup> addEntry(String value) {
		List<String> entries = getEntriesList();
		if (entries == null) {
			return Futures.immediateFailedFuture(new IllegalStateException("Group is not a list!").fillInStackTrace());
		}
		List<String> newEntries = new ArrayList<>(entries.size() + 1);
		newEntries.addAll(entries);
		newEntries.add(value);
		return setEntries(newEntries);
	}

	/**
	 * Remove an entry from either the list or map used to store entries for this group.
	 * (yeah, it's a single method. Isn't that cool?)
	 *
	 * @param key The key or list entry to remove.
	 * @return The new matcher group, this if this group didn't contain the provided entry, or {@link InvalidGroupException} if this matcher group is invalid.
	 */
	public ListenableFuture<MatcherGroup> removeEntry(String key) {
		if (!isValid()) {
			return Futures.immediateFailedFuture(new InvalidGroupException());
		}

		if (isMap()) {
			Map<String, String> newEntries = new HashMap<>(getEntries());
			if (newEntries.remove(key) == null) {
				return Futures.immediateFuture(this);
			} else {
				return setEntries(newEntries);
			}
		} else if (isList()) {
			List<String> newEntries = new ArrayList<>(getEntriesList());
			if (!newEntries.remove(key)) {
				return Futures.immediateFuture(this);
			} else {
				return setEntries(newEntries);
			}
		} else { // What are we?
			return Futures.immediateFailedFuture(new IllegalStateException("Group is not a list or map!").fillInStackTrace());
		}
	}

	/**
	 * Thrown when a group's data is no longer valid
	 */
	public static class InvalidGroupException extends Exception {

	}


	/**
	 * Returns whether this group matches the provided context, based on this group's qualifiers.
	 * Only needs to match one of each qualifier on this group.
	 *
	 * @param context The context to verify
	 * @return whether or not a match is performed.
	 */
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
	 * @return true if matcher was removed, false if {@link #isValid()} returns false or
	 * 	if for some other reason the backend doesn't feel like removing this matcher.
	 */
	public abstract ListenableFuture<Boolean> remove();

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
	public int compareTo(@Nonnull MatcherGroup o) {
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
