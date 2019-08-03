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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.MoreObjects;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

public class PEXSubjectReference implements SubjectReference, Map.Entry<String, String> {
    private final String collection, ident;
    private final PermissionsExPlugin pex;

    public PEXSubjectReference(String collection, String ident, PermissionsExPlugin pex) {
        this.ident = ident;
        this.collection = collection;
        this.pex = pex;
    }

    @Override
    public String getCollectionIdentifier() {
        return this.collection;
    }

    @Override
    public String getSubjectIdentifier() {
        return this.ident;
    }

    @Override
	public CompletableFuture<Subject> resolve() {
        return this.pex.loadCollection(this.collection).thenCompose(coll -> coll.loadSubject(this.ident));
	}

    @Override
    public String getKey() {
        return this.collection;
    }

    @Override
    public String getValue() {
        return this.ident;
    }

    @Override
    public String setValue(String value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PEXSubjectReference that = (PEXSubjectReference) o;
        return collection.equals(that.collection) &&
                ident.equals(that.ident);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, ident);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("collection", this.collection)
                .add("identifier", this.ident)
                .toString();
    }
}