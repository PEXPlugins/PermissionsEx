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
package ca.stellardrift.permissionsex.rank;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public interface RankLadderCollection {

    CompletableFuture<RankLadder> get(String identifier, @Nullable Consumer<RankLadder> listener);

    CompletableFuture<RankLadder> update(String identifier, UnaryOperator<RankLadder> updateFunc);

    CompletableFuture<Boolean> has(String identifier);

    CompletableFuture<RankLadder> set(String identifier, RankLadder newData);

    void addListener(String identifier, Consumer<RankLadder> listener);

    Stream<String> names();

}
