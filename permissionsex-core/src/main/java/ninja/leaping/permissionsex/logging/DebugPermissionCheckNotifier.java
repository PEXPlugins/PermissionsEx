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
package ninja.leaping.permissionsex.logging;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Log debug messages
 */
public class DebugPermissionCheckNotifier implements PermissionCheckNotifier {
    private final Logger logger;
    private final PermissionCheckNotifier delegate;

    public DebugPermissionCheckNotifier(Logger logger, PermissionCheckNotifier delegate) {
        this.logger = logger;
        this.delegate = delegate;
    }

    private String stringIdentifier(Map.Entry<String, String> identifier) {
        return identifier.getKey() + " " + identifier.getValue();
    }

    public PermissionCheckNotifier getDelegate() {
        return this.delegate;
    }

    @Override
    public void onPermissionCheck(Map.Entry<String, String> subject, Set<Map.Entry<String, String>> contexts, String permission, int value) {
        logger.info("Permission " + permission + " checked in " + contexts + " for " + stringIdentifier(subject) + ": " + value);
        delegate.onPermissionCheck(subject, contexts, permission, value);
    }

    @Override
    public void onOptionCheck(Map.Entry<String, String> subject, Set<Map.Entry<String, String>> contexts, String option, String value) {
        logger.info("Option " + option + " checked in " + contexts + " for " + stringIdentifier(subject) + ": " + value);
        delegate.onOptionCheck(subject, contexts, option, value);
    }

    @Override
    public void onParentCheck(Map.Entry<String, String> subject, Set<Map.Entry<String, String>> contexts, List<Map.Entry<String, String>> parents) {
        logger.info("Parents checked in " + contexts + " for " +  stringIdentifier(subject) + ": " + parents);
        delegate.onParentCheck(subject, contexts, parents);

    }
}
