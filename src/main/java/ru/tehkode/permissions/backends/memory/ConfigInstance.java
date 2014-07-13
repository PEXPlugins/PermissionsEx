package ru.tehkode.permissions.backends.memory;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents an instance of configuration data to be loaded and saved.
 */
public interface ConfigInstance<T extends MemoryMatcherGroup<T>> {
	Collection<T> getGroups();
	void setGroups(Collection<T> groups);

	public static class Memory<T extends MemoryMatcherGroup<T>> implements ConfigInstance<T> {
		private Collection<T> groups = Collections.emptyList();

		@Override
		public Collection<T> getGroups() {
			return groups;
		}

		@Override
		public void setGroups(Collection<T> groups) {
			this.groups = groups;
		}
	}
}
