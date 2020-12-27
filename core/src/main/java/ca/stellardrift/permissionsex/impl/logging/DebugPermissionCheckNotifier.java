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
package ca.stellardrift.permissionsex.impl.logging;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.logging.PermissionCheckNotifier;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static ca.stellardrift.permissionsex.impl.logging.Messages.CHECK_OPTION;
import static ca.stellardrift.permissionsex.impl.logging.Messages.CHECK_PERMISSION;

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

    private <I> String stringIdentifier(SubjectRef<I> identifier) {
        return identifier.type().name() + " " + identifier.type().serializeIdentifier(identifier.identifier());
    }

    public PermissionCheckNotifier getDelegate() {
        return this.delegate;
    }

    @Override
    public void onPermissionCheck(SubjectRef<?> subject, Set<ContextValue<?>> contexts, String permission, int value) {
        if (this.filterPredicate.test(permission)) {
            logger.info(CHECK_PERMISSION.tr(permission, contexts, stringIdentifier(subject), value));
        }
        delegate.onPermissionCheck(subject, contexts, permission, value);
    }

    @Override
    public void onOptionCheck(SubjectRef<?> subject, Set<ContextValue<?>> contexts, String option, String value) {
        if (this.filterPredicate.test(option)) {
            logger.info(CHECK_OPTION.tr(option, contexts, stringIdentifier(subject), value));
        }
        delegate.onOptionCheck(subject, contexts, option, value);
    }

    @Override
    public void onParentCheck(SubjectRef<?> subject, Set<ContextValue<?>> contexts, List<SubjectRef<?>> parents) {
        logger.info(Messages.CHECK_PARENT.tr(contexts, stringIdentifier(subject), parents));
        delegate.onParentCheck(subject, contexts, parents);
    }
}
