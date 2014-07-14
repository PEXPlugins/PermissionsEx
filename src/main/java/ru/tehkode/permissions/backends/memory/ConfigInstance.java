package ru.tehkode.permissions.backends.memory;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents an instance of configuration data to be loaded and saved.
 */
public interface ConfigInstance {
	Collection<MemoryMatcherGroup> getGroups();
	void setGroups(Collection<MemoryMatcherGroup> groups);

	public static class Memory implements ConfigInstance {
		private Collection<MemoryMatcherGroup> groups = Collections.emptyList();

		@Override
		public Collection<MemoryMatcherGroup> getGroups() {
			return groups;
		}

		@Override
		public void setGroups(Collection<MemoryMatcherGroup> groups) {
			this.groups = groups;
		}
	}
}
