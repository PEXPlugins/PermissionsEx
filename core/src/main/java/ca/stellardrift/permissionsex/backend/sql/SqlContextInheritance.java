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

package ca.stellardrift.permissionsex.backend.sql;

import com.google.common.collect.ImmutableList;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.ContextInheritance;
import ca.stellardrift.permissionsex.util.CheckedBiConsumer;
import ca.stellardrift.permissionsex.util.Util;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SqlContextInheritance implements ContextInheritance {
    private final Map<ContextValue<?>, List<ContextValue<?>>> inheritance;
    private final AtomicReference<ImmutableList<CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException>>> updatesToPerform = new AtomicReference<>();

    SqlContextInheritance(Map<ContextValue<?>, List<ContextValue<?>>> inheritance, List<CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException>> updates) {
        this.inheritance = inheritance;
        if (updates != null) {
            this.updatesToPerform.set(ImmutableList.copyOf(updates));
        }
    }

    @Override
    public List<ContextValue<?>> getParents(ContextValue<?> context) {
        List<ContextValue<?>> ret = inheritance.get(context);
        return ret == null ? ImmutableList.of() : ret;
    }

    @Override
    public SqlContextInheritance setParents(ContextValue<?> context, List<ContextValue<?>> parents) {
        if (parents == null) {
            return new SqlContextInheritance(Util.updateImmutable(this.inheritance, context, null), Util.appendImmutable(this.updatesToPerform.get(), (dao, inherit) -> {
                dao.setContextInheritance(context, null);
            }));
        } else {
            return new SqlContextInheritance(Util.updateImmutable(this.inheritance, context, parents), Util.appendImmutable(this.updatesToPerform.get(), (dao, inherit) -> {
                List<ContextValue<?>> newParents = inherit.getParents(context);
                if (newParents != null) {
                    dao.setContextInheritance(context, newParents);
                }
            }));
        }
    }

    @Override
    public Map<ContextValue<?>, List<ContextValue<?>>> getAllParents() {
        return inheritance;
    }

    void doUpdate(SqlDao dao) throws SQLException {
        List<CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException>> updates = updatesToPerform.getAndSet(null);
        if (updates != null) {
            for (CheckedBiConsumer<SqlDao, SqlContextInheritance, SQLException> action : updates) {
                action.accept(dao, this);
            }
        }
    }
}
