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

import ninja.leaping.permissionsex.data.SubjectRef;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Log debug messages
 */
public class DebugPermissionCheckNotifier implements PermissionCheckNotifier {
    private final TranslatableLogger logger;
    private final PermissionCheckNotifier delegate;
    private final Predicate<String> filterPredicate;

    public DebugPermissionCheckNotifier(TranslatableLogger logger, PermissionCheckNotifier delegate, @Nullable Predicate<String> filterPredicate) {
        this.logger = logger;
        this.delegate = delegate;
        this.filterPredicate = filterPredicate == null ? x -> true : filterPredicate;
    }

    private String stringIdentifier(SubjectRef identifier) {
        return identifier.getType() + " " + identifier.getIdentifier();
    }

    public PermissionCheckNotifier getDelegate() {
        return this.delegate;
    }

    @Override
    public void onPermissionCheck(SubjectRef subject, Set<Map.Entry<String, String>> contexts, String permission, int value) {
        if (this.filterPredicate.test(permission)) {
            logger.info(t("Permission %s checked in %s for %s: %s", permission, contexts, stringIdentifier(subject), value));
        }
        delegate.onPermissionCheck(subject, contexts, permission, value);
    }

    @Override
    public void onOptionCheck(SubjectRef subject, Set<Map.Entry<String, String>> contexts, String option, String value) {
        if (this.filterPredicate.test(option)) {
            logger.info(t("Option %s checked in %s for %s: %s", option, contexts, stringIdentifier(subject), value));
        }
        delegate.onOptionCheck(subject, contexts, option, value);
    }

    @Override
    public void onParentCheck(SubjectRef subject, Set<Map.Entry<String, String>> contexts, List<SubjectRef> parents) {
        logger.info(t("Parents checked in %s for %s: %s", contexts, stringIdentifier(subject), parents));
        delegate.onParentCheck(subject, contexts, parents);
    }
}
