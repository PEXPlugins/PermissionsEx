/*
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

package ca.stellardrift.permissionsex.logging;

import ca.stellardrift.permissionsex.context.ContextValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static ca.stellardrift.permissionsex.logging.Messages.CHECK_OPTION;
import static ca.stellardrift.permissionsex.logging.Messages.CHECK_PERMISSION;

/**
 * Log debug messages
 */
public class DebugPermissionCheckNotifier implements PermissionCheckNotifier {
    private final FormattedLogger logger;
    private final PermissionCheckNotifier delegate;
    private final Predicate<String> filterPredicate;

    public DebugPermissionCheckNotifier(FormattedLogger logger, PermissionCheckNotifier delegate, @Nullable Predicate<String> filterPredicate) {
        this.logger = logger;
        this.delegate = delegate;
        this.filterPredicate = filterPredicate == null ? x -> true : filterPredicate;
    }

    private String stringIdentifier(Map.Entry<String, String> identifier) {
        return identifier.getKey() + " " + identifier.getValue();
    }

    public PermissionCheckNotifier getDelegate() {
        return this.delegate;
    }

    @Override
    public void onPermissionCheck(Map.Entry<String, String> subject, Set<ContextValue<?>> contexts, String permission, int value) {
        if (this.filterPredicate.test(permission)) {
            logger.info(CHECK_PERMISSION.toComponent(permission, contexts, stringIdentifier(subject), value));
        }
        delegate.onPermissionCheck(subject, contexts, permission, value);
    }

    @Override
    public void onOptionCheck(Map.Entry<String, String> subject, Set<ContextValue<?>> contexts, String option, String value) {
        if (this.filterPredicate.test(option)) {
            logger.info(CHECK_OPTION.toComponent(option, contexts, stringIdentifier(subject), value));
        }
        delegate.onOptionCheck(subject, contexts, option, value);
    }

    @Override
    public void onParentCheck(Map.Entry<String, String> subject, Set<ContextValue<?>> contexts, List<Map.Entry<String, String>> parents) {
        logger.info(Messages.CHECK_PARENT.toComponent(contexts, stringIdentifier(subject), parents));
        delegate.onParentCheck(subject, contexts, parents);
    }
}
