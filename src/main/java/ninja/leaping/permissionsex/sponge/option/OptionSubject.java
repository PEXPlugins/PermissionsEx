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

import com.google.common.base.Optional;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import java.util.Set;

public interface OptionSubject extends Subject {
    @Override
    OptionSubjectData getData();

    @Override
    OptionSubjectData getTransientData();

    /**
     * Get the value of a given option in the given context
     *
     * @param key The key to get an option by
     * @return The value of the option, if any is present
     */
    public Optional<String> getOption(Set<Context> contexts, String key);

    /**
     * Get the value of a given option in the subject's current context
     *
     * @param key The key to get an option by
     * @return The value of the option, if any is present
     */
    public Optional<String> getOption(String key);
}
