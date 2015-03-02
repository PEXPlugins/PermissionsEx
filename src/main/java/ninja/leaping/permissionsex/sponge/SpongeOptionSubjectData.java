/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.sponge;

import ninja.leaping.permissionsex.backends.DataStore;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.sponge.option.OptionSubjectData;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around ImmutableSubjectData that writes to backend each change
 */
public class SpongeOptionSubjectData implements OptionSubjectData, Caching {
	private final DataStore source;
	private final String type, identifier;
	private volatile ImmutableOptionSubjectData data;

	public SpongeOptionSubjectData(DataStore source, String type, String identifier) {
		this.source = source;
		this.type = type;
		this.identifier = identifier;
		this.data = source.getData(type, identifier, this);
	}

	@Override
	public void clearCache() {
		this.data = source.getData(type, identifier, this);
	}

	@Override
	public Map<Set<Context>, Map<String, String>> getAllOptions() {
		return this.data.getAllOptions();
	}

	@Override
	public Map<String, String> getOptions(Set<Context> contexts) {
		return this.data.getOptions(contexts);
	}

	@Override
	public boolean setOption(Set<Context> contexts, String key, String value) {
		this.source.setData(type, identifier, data.setOption(contexts, key, value));
		return true;
	}

	@Override
	public boolean clearOptions(Set<Context> contexts) {
		this.source.setData(type, identifier, data.clearOptions(contexts));
		return false;
	}

	@Override
	public boolean clearOptions() {
		this.source.setData(type, identifier, data.clearOptions());
		return false;
	}

	@Override
	public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
		return data.getAllPermissions();
	}

	@Override
	public Map<String, Boolean> getPermissions(Set<Context> set) {
		return data.getPermissions(set);
	}

	@Override
	public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
		this.source.setData(type, identifier, data.setPermission(set, s, tristate));
		return false;
	}

	@Override
	public boolean clearPermissions() {
		this.source.setData(type, identifier, data.clearPermissions());
		return false;
	}

	@Override
	public boolean clearPermissions(Set<Context> set) {
		this.source.setData(type, identifier, data.clearPermissions(set));
		return false;
	}

	@Override
	public Map<Set<Context>, List<Subject>> getAllParents() {
		return data.getAllParents();
	}

	@Override
	public List<Subject> getParents(Set<Context> set) {
		return data.getParents(set);
	}

	@Override
	public boolean addParent(Set<Context> set, Subject subject) {
		return false;
	}

	@Override
	public boolean removeParent(Set<Context> set, Subject subject) {
		return false;
	}

	@Override
	public boolean clearParents() {
		return false;
	}

	@Override
	public boolean clearParents(Set<Context> set) {
		return false;
	}
}
