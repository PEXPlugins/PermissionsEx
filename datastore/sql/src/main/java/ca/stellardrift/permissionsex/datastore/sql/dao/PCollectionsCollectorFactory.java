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
package ca.stellardrift.permissionsex.datastore.sql.dao;

import ca.stellardrift.permissionsex.impl.util.PCollections;
import org.jdbi.v3.core.collector.CollectorFactory;
import org.pcollections.PSet;
import org.pcollections.PStack;
import org.pcollections.PVector;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;

import static io.leangen.geantyref.GenericTypeReflector.erase;

final class PCollectionsCollectorFactory implements CollectorFactory {
    static final PCollectionsCollectorFactory INSTANCE = new PCollectionsCollectorFactory();
    private final IdentityHashMap<Class<?>, Collector<?, ?, ?>> collectors = new IdentityHashMap<>();
    
    private PCollectionsCollectorFactory() {
        this.collectors.put(PSet.class, PCollections.toPSet());
        this.collectors.put(PVector.class, PCollections.toPVector());
        this.collectors.put(PStack.class, PCollections.toPStack());
        
    }
    
    @Override
    public boolean accepts(Type containerType) {
        final Type erased = erase(containerType);
        return this.collectors.containsKey(erased);
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        if (!(containerType instanceof ParameterizedType)) {
            return Optional.empty();
        }
        return Optional.ofNullable(((ParameterizedType) containerType).getActualTypeArguments()[0]);
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        final Collector<?, ?, ?> collector = this.collectors.get(erase(containerType));
        if (collector == null) {
            throw new IllegalArgumentException("Does not accept " + containerType);
        }
        return collector;
    }
}
