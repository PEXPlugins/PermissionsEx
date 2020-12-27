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
package ca.stellardrift.permissionsex.query;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.Segment;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.Change;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A query for permissions data.
 *
 * <p>Queries are build in <em>stages</em>, and each stage Parameters can be applied to control
 * almost every aspect of permissions resolution.
 *
 * <h2>Stages</h2>
 * <ol>
 *     <li>select type and/or identifier</li>
 * </ol>
 */
public interface PermissionsQuery {

    <I> IdentifierStage<I> inType(final SubjectType<I> type);

    AllOrAnyState subject(final SubjectRef<?> ref);

    AllOrAnyState anySubject();

    /**
     * Specifies an identifier for a specific subject type
     * @param <I> the subject's identifier
     */
    interface IdentifierStage<I> {
        AllOrAnyState withName(final I name);

        /**
         * Act on any subject in this type
         *
         * @return a stream providing all identifiers
         */
        AllOrAnyState anySubject();
    }


    interface AllOrAnyState {
        EitherStage matchingAllOf();
        EitherStage matchingAnyOf();
    }

    interface EitherStage {
        
        // Filtering operators

        EitherStage hasContext(final ContextDefinition<?> definition);

        EitherStage hasContext(final ContextValue<?> context);

        /**
         * Has a permission that may or may not evaluate to true
         * @param permission
         * @return
         */
        EitherStage hasPermissionSet(final String permission);

        /**
         * Has a permission that evaluates to true
         * @param permission
         * @return
         */
        EitherStage hasPermission(final String permission);

        EitherStage withOption(final String option, final String value);

        EitherStage isChildOf(final SubjectRef<?> parent);
        
        // Terminal operators

        SubjectStage subjects();

        SegmentsStage segments();

    }

    interface SubjectStage {

        Stream<CalculatedSubject> loaded();
        CompletableFuture<Set<String>> names();
        CompletableFuture<Set<CalculatedSubject>> subjects();
    }

    interface SegmentsStage {
        SegmentsStage containingContext(final ContextValue<?> value);

        SegmentsStage contextSet(final ContextDefinition<?> definition);
        
        // TODO: weight, inheritability, etc

        CompletableFuture<Set<Segment>> all();

        Set<Segment> loaded();

        CompletableFuture<Change<Segment>> update(final BiFunction<Set<ContextValue<?>>, Segment, Segment> updater);
    }
}
