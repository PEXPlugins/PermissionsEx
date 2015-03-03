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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.sponge.option.OptionSubjectData;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Wrapper around ImmutableSubjectData that writes to backend each change
 */
public class PEXOptionSubjectData implements OptionSubjectData, Caching {
	private final SubjectCache cache;
	private final String identifier;
	private volatile ImmutableOptionSubjectData data;

	public PEXOptionSubjectData(SubjectCache cache, String identifier) throws ExecutionException {
		this.cache = cache;
		this.identifier = identifier;
		clearCache(cache.getData(identifier, this));
	}

	@Override
	public void clearCache(ImmutableOptionSubjectData newData) {
		this.data = newData;
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
		this.cache.update(identifier, data.setOption(contexts, key, value));
		return true;
	}

	@Override
	public boolean clearOptions(Set<Context> contexts) {
		this.cache.update(identifier, data.clearOptions(contexts));
		return false;
	}

	@Override
	public boolean clearOptions() {
		this.cache.update(identifier, data.clearOptions());
		return false;
	}

	@Override
	public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
		return Maps.transformValues(data.getAllPermissions(), new Function<Map<String, Integer>, Map<String, Boolean>>() {
			@Nullable
			@Override
			public Map<String, Boolean> apply(Map<String, Integer> stringIntegerMap) {
				return Maps.transformValues(stringIntegerMap, new Function<Integer, Boolean>() {
					@Nullable
					@Override
					public Boolean apply(@Nullable Integer integer) {
						return integer != null && integer > 0;
					}
				});
			}
		});
	}

	@Override
	public Map<String, Boolean> getPermissions(Set<Context> set) {
		return Maps.transformValues(data.getPermissions(set), new Function<Integer, Boolean>() {
			@Nullable
			@Override
			public Boolean apply(@Nullable Integer integer) {
				return integer != null && integer > 0;
			}
		});
	}

	@Override
	public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
		int val;
		switch (tristate) {
			case TRUE:
				val = 1;
				break;
			case FALSE:
				val = -1;
				break;
			case UNDEFINED:
				val = 0;
				break;
			default:
				throw new IllegalStateException("Unknown tristate provided " + tristate);
		}

		this.cache.update(identifier, data.setPermission(set, s, val));
		return false;
	}

	@Override
	public boolean clearPermissions() {
		this.cache.update(identifier, data.clearPermissions());
		return false;
	}

	@Override
	public boolean clearPermissions(Set<Context> set) {
		this.cache.update(identifier, data.clearPermissions(set));
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
