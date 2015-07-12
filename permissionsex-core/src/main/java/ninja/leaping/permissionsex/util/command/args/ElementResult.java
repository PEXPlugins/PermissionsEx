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
package ninja.leaping.permissionsex.util.command.args;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import ninja.leaping.permissionsex.util.Translatable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ninja.leaping.permissionsex.util.Translations._;

/**
 * Represents the current parsing state of a CommandElement.
 */
public class ElementResult {
    private final String rawInput;
    private List<SingleArg> args;
    private int index;
    private final CommandElement key;
    private final ElementResult holder;
    private List<Object> values = null;

    public static ElementResult root(String rawInput, List<SingleArg> args) {
        return new ElementResult(rawInput, args, -1, null, null);
    }

    ElementResult(String rawInput, List<SingleArg> args, int index, CommandElement key, ElementResult holder) {
        this.rawInput = rawInput;
        this.args = args;
        this.index = index;
        this.key = key;
        this.holder = holder;
    }

    // -- Raw args
    public boolean hasNext() {
        return index + 1 < args.size();
    }

    public String peek() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError(_("Not enough arguments"));
        }
        return args.get(index + 1).getValue();
    }

    public String next() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError(_("Not enough arguments!"));
        }
        return args.get(++index).getValue();
    }

    public Optional<String> nextIfPresent() {
        return hasNext() ? Optional.of(args.get(++index).getValue()) : Optional.<String>absent();
    }

    public String current() {
        return args.get(index).getValue();
    }

    public List<String> getAllArgs() {
        return Lists.transform(args, new Function<SingleArg, String>() {
            @Nullable
            @Override
            public String apply(SingleArg input) {
                return input.getValue();
            }
        });
    }

    List<SingleArg> getArgs() {
        return args;
    }

    public void filterArgs(Predicate<String> filter) {
        SingleArg currentArg = index == -1 ? null : args.get(index);
        List<SingleArg> newArgs = new ArrayList<>();
        for (SingleArg arg : args) {
            if (filter.apply(arg.getValue())) {
                newArgs.add(arg);
            }
        }
        index = currentArg == null ? -1 : newArgs.indexOf(currentArg);
        this.args = newArgs;

    }

    public String getRaw() {
        return rawInput;
    }

    public void insertArg(String value) {
        int index = this.index < 0 ? 0 : args.get(this.index).getEndIdx();
        this.args = new ArrayList<>(this.args);
        this.args.add(index, new SingleArg(value, index, index));
    }

    public void removeArgs(ElementResult fromPosition) {
        this.args = new ArrayList<>(args); // copy to keep from other elements
        int startIdx = Math.max(fromPosition.index, 0);
        int endIdx = this.index;
        if (index >= startIdx) {
            if (index < endIdx) {
                index = startIdx - 1;
            } else {
                index -= (endIdx - startIdx) + 1;
            }
        }
        for (int i = startIdx; i <= endIdx; ++i) {
            args.remove(startIdx);
        }
    }

    public void setPosition(ElementResult position) {
        this.index = position.index;
    }

    // -- Parsed args

    /**
     * Add a value to this argument instance.
     *
     * @param value The value to add
     * @return this
     */
    public ElementResult addValue(Object value) {
        if (this.key == null) {
            throw new IllegalStateException("Unable to set value for keyless ElementResult");
        }

        if (values == null) {
            values = Collections.singletonList(value);
            return this;
        } else if (values.size() == 1) {
            values = new ArrayList<>(values);
        }
        values.add(value);
        return this;
    }

    /**
     * Add multiple values to the current instance
     *
     * @param value The values to add
     * @return this
     */
    public ElementResult addValues(Iterable<Object> value) {
        if (this.key == null) {
            throw new IllegalStateException("Unable to set value for keyless ElementResult");
        }

        if (values == null) {
            values = Lists.newArrayList(value);
            return this;
        } else if (values.size() == 1) {
            values = new ArrayList<>(values);
        }
        for (Object o : value) {
            values.add(o);
        }
        if (values.isEmpty()) {
            values = null;
        }
        return this;
    }

    // -- Relations

    public ElementResult openChild(CommandElement key) {
        ElementResult child = new ElementResult(rawInput, args, index, key, this); // Add to children
        return child;
    }

    public ArgumentParseException createError(Translatable message) {
        // TODO: Mark failed, or remove from structure?
        return new ArgumentParseException(message, rawInput, index < 0 ? 0 : args.get(index).getStartIdx(), this);
    }

    public CommandElement getKey() {
        return key;
    }

    public boolean hasValue() {
        return this.values != null;
    }

    public List<Object> getValues() {
        return this.values;
    }

    public ElementResult getHolder() {
        return this.holder;
    }

    /**
     * Get the distance in element results from this to other.
     *
     * @param other The other point to calculate distance from
     * @return the distance, or -1 if other is not a parent of this
     */
    public int distanceFromParent(ElementResult other) {
        ElementResult par = this;
        int distance = 0;
        while (par != other) {
            ++distance;
            par = par.getHolder();
            if (par == null) {
                return -1;
            }
        }
        return distance;
    }

    static class SingleArg {
        private final String value;
        private final int startIdx, endIdx;

        SingleArg(String value, int startIdx, int endIdx) {
            this.value = value;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        public String getValue() {
            return value;
        }

        public int getStartIdx() {
            return startIdx;
        }

        public int getEndIdx() {
            return endIdx;
        }
    }
}
