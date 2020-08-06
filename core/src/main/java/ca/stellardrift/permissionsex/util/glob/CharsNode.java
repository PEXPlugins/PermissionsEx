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

package ca.stellardrift.permissionsex.util.glob;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class CharsNode extends GlobNode {
    private final String[] characters;

    /* package */ CharsNode(String characters) {
        this.characters = requireNonNull(characters, "characters").codePoints()
                .mapToObj( cp -> String.valueOf(Character.toChars(cp)))
                .toArray(String[]::new);
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index < (characters.length);
            }

            @Override
            public String next() {
                return characters[index++];
            }
        };
    }

    @Override
    public void forEach(Consumer<? super String> action) {
        for (String character : this.characters) {
            action.accept(character);
        }
    }

    @Override
    public Spliterator<String> spliterator() {
        return Arrays.spliterator(this.characters);
    }

    @Override
    public int hashCode() {
        return 31 + Arrays.hashCode(this.characters);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CharsNode
                && Arrays.equals(((CharsNode) obj).characters, this.characters);
    }

    @Override
    public String toString() {
        return "CharsNode{" + Arrays.toString(this.characters) + '}';
    }
}
