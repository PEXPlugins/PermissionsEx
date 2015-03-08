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
package ninja.leaping.permissionsex.sponge.option;

import org.spongepowered.api.service.permission.MemorySubjectData;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.context.Context;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Extension of MemorySubjectData that implements option handling
 */
public class MemoryOptionSubjectData extends MemorySubjectData implements OptionSubjectData {
    private final ConcurrentMap<Set<Context>, Map<String, String>> options = new ConcurrentHashMap<>();

    public MemoryOptionSubjectData(PermissionService service) {
        super(service);
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return null;
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {

        return null;
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, String value) {
        return false;
    }

    @Override
    public boolean clearOptions(Set<Context> contexts) {
        return false;
    }

    @Override
    public boolean clearOptions() {
        return false;
    }
}
