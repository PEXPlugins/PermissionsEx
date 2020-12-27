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
package ca.stellardrift.permissionsex.datastore.sql;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SqlContextInheritance implements ContextInheritance {
    private final PMap<ContextValue<?>, PVector<ContextValue<?>>> inheritance;
    private final AtomicReference<PVector<CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException>>> updatesToPerform = new AtomicReference<>();

    SqlContextInheritance(final PMap<ContextValue<?>, PVector<ContextValue<?>>> inheritance) {
        this(inheritance, TreePVector.empty());
    }

    SqlContextInheritance(final PMap<ContextValue<?>, PVector<ContextValue<?>>> inheritance, final PVector<CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException>> updates) {
        this.inheritance = inheritance;
        this.updatesToPerform.set(updates);
    }

    @Override
    public PVector<ContextValue<?>> parents(ContextValue<?> context) {
        PVector<ContextValue<?>> ret = this.inheritance.get(context);
        return ret == null ? PCollections.vector() : ret;
    }

    @Override
    public SqlContextInheritance parents(final ContextValue<?> context, final @Nullable List<ContextValue<?>> parents) {
        if (parents == null) {
            return new SqlContextInheritance(this.inheritance.minus(context), this.updatesToPerform.get().plus((dao, inherit) -> {
                dao.setContextInheritance(context, null);
            }));
        } else {
            final PVector<ContextValue<?>> pParent = PCollections.asVector(parents);
            return new SqlContextInheritance(this.inheritance.plus(context, pParent), this.updatesToPerform.get().plus((dao, inherit) -> {
                final PVector<ContextValue<?>> newParents = inherit.parents(context);
                if (!newParents.isEmpty()) {
                    dao.setContextInheritance(context, newParents);
                }
            }));
        }
    }

    @Override
    public Map<ContextValue<?>, List<ContextValue<?>>> allParents() {
        return PCollections.narrow(this.inheritance);
    }

    void doUpdate(SqlDao dao) throws SQLException {
        List<CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException>> updates = updatesToPerform.getAndSet(TreePVector.empty());
        for (CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException> action : updates) {
            action.accept(dao, this);
        }
    }
}
