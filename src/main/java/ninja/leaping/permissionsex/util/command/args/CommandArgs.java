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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import ninja.leaping.permissionsex.util.Translatable;

import javax.annotation.Nullable;
import java.util.List;

import static ninja.leaping.permissionsex.util.Translations.tr;

/**
 * Created by zml on 25.03.15.
 */
public class CommandArgs {
    private final String rawInput;
    private final List<SingleArg> args;
    private int index = -1;

    public CommandArgs(String rawInput, List<SingleArg> args) {
        this.rawInput = rawInput;
        this.args = ImmutableList.copyOf(args);
    }

    public boolean hasNext() {
        return index + 1 < args.size();
    }

    public String peek() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError(tr("Not enough arguments"));
        }
        return args.get(index + 1).getValue();
    }

    public String next() throws ArgumentParseException {
        if (!hasNext()) {
            throw createError(tr("Not enough arguments!"));
        }
        return args.get(++index).getValue();
    }

    public Optional<String> nextIfPresent() {
        return hasNext() ? Optional.of(args.get(++index).getValue()) : Optional.<String>absent();
    }

    public ArgumentParseException createError(Translatable message, Object... formatArgs) {
        return new ArgumentParseException(message, rawInput, index < 0 ? 0 : args.get(index).getStartIdx(), formatArgs);
    }

    public List<String> getAll() {
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
