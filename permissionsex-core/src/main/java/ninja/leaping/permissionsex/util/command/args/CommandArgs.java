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

import com.google.common.collect.Lists;
import ninja.leaping.permissionsex.util.Translatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Holder for command arguments
 */
public class CommandArgs {
    private final String rawInput;
    private List<SingleArg> args;
    private int index = -1;

    public CommandArgs(String rawInput, List<SingleArg> args) {
        this.rawInput = rawInput;
        this.args = new ArrayList<>(args);
    }

    public boolean hasNext() {
        return index + 1 < args.size();
    }

    public String peek() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError(t("Not enough arguments"));
        }
        return args.get(index + 1).getValue();
    }

    public String next() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError(t("Not enough arguments!"));
        }
        return args.get(++index).getValue();
    }

    public Optional<String> nextIfPresent() {
        return hasNext() ? Optional.of(args.get(++index).getValue()) : Optional.empty();
    }

    public ArgumentParseException createError(Translatable message) {
        //System.out.println("Creating error: " + message.translateFormatted(Locale.getDefault()));
        //Thread.dumpStack();
        return new ArgumentParseException(message, rawInput, index < 0 ? 0 : args.get(index).getStartIdx());
    }

    public List<String> getAll() {
        return Lists.transform(args, SingleArg::getValue);
    }

    List<SingleArg> getArgs() {
        return args;
    }

    public void filterArgs(Predicate<String> filter) {
        SingleArg currentArg = index == -1 ? null : args.get(index);
        List<SingleArg> newArgs = new ArrayList<>();
        for (SingleArg arg : args) {
            if (filter.test(arg.getValue())) {
                newArgs.add(arg);
            }
        }
        index = currentArg == null ? -1 : newArgs.indexOf(currentArg);
        this.args = newArgs;

    }

    /**
     * Return the position of the last next() call, or -1 if next() has never been called
     *
     * @return The current position
     */
    public int getPosition() {
        return index;
    }

    public void setPosition(int position) {
        this.index = position;
    }

    public String getRaw() {
        return rawInput;
    }

    public void insertArg(String value) {
        int index = this.index < 0 ? 0 : args.get(this.index).getEndIdx();
        this.args.add(index, new SingleArg(value, index, index));
    }

    public void removeArgs(int startIdx, int endIdx) {
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
