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

package ca.stellardrift.permissionsex.logging;

import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.Locale;

/**
 * An implementation of {@link FormattedLogger} that delegates to an existing logger
 */
class WrappingFormattedLogger implements FormattedLogger {
    private final Logger wrapping;
    private String prefix;
    private final boolean supportsFormatting;

    public WrappingFormattedLogger(Logger wrapping, boolean supportsFormatting) {
        this.wrapping = wrapping;
        this.supportsFormatting = supportsFormatting;
    }

    @Override
    public Locale getLogLocale() {
        return Locale.getDefault();
    }

    @Override
    public Locale getLogLocale(Marker marker) {
        return getLogLocale();
    }

    @Nullable
    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public void setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
    }

    @Override
    public ComponentSerializer<Component, ?, String> getSerializer() {
        if (supportsFormatting) {
            return LegacyComponentSerializer.legacyLinking();
        } else {
            return PlainComponentSerializer.INSTANCE;
        }
    }

    private String applyPrefix(String input) {
        final String prefix = this.prefix;
        return prefix == null ? input : (prefix + input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return wrapping.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTraceEnabled() {
        return wrapping.isTraceEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(String msg) {
        wrapping.trace(applyPrefix(msg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(String format, Object arg) {
        wrapping.trace(applyPrefix(format), arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(String format, Object arg1, Object arg2) {
        wrapping.trace(applyPrefix(format), arg1, arg2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(String format, Object... arguments) {
        wrapping.trace(applyPrefix(format), arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(String msg, Throwable t) {
        wrapping.trace(applyPrefix(msg), t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTraceEnabled(Marker marker) {
        return wrapping.isTraceEnabled(marker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(Marker marker, String msg) {
        wrapping.trace(marker, applyPrefix(msg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(Marker marker, String format, Object arg) {
        wrapping.trace(marker, applyPrefix(format), arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        wrapping.trace(marker, applyPrefix(format), arg1, arg2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        wrapping.trace(marker, applyPrefix(format), argArray);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        wrapping.trace(marker, applyPrefix(msg), t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDebugEnabled() {
        return wrapping.isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(String msg) {
        wrapping.debug(applyPrefix(msg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(String format, Object arg) {
        wrapping.debug(applyPrefix(format), arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(String format, Object arg1, Object arg2) {
        wrapping.debug(applyPrefix(format), arg1, arg2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(String format, Object... arguments) {
        wrapping.debug(applyPrefix(format), arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(String msg, Throwable t) {
        wrapping.debug(applyPrefix(msg), t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDebugEnabled(Marker marker) {
        return wrapping.isDebugEnabled(marker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(Marker marker, String msg) {
        wrapping.debug(marker, applyPrefix(msg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(Marker marker, String format, Object arg) {
        wrapping.debug(marker, applyPrefix(format), arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        wrapping.debug(marker, applyPrefix(format), arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        wrapping.debug(marker, applyPrefix(format), arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        wrapping.debug(marker, applyPrefix(msg), t);
    }

    @Override
    public boolean isInfoEnabled() {
        return wrapping.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        wrapping.info(applyPrefix(msg));
    }

    @Override
    public void info(String format, Object arg) {
        wrapping.info(applyPrefix(format), arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        wrapping.info(applyPrefix(format), arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        wrapping.info(applyPrefix(format), arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        wrapping.info(applyPrefix(msg), t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return wrapping.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        wrapping.info(marker, applyPrefix(msg));
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        wrapping.info(marker, applyPrefix(format), arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        wrapping.info(marker, applyPrefix(format), arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        wrapping.info(marker, applyPrefix(format), arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        wrapping.info(marker, applyPrefix(msg), t);
    }

    @Override
    public boolean isWarnEnabled() {
        return wrapping.isWarnEnabled();
    }


    @Override
    public void warn(String msg) {
        wrapping.warn(applyPrefix(msg));
    }

    @Override
    public void warn(String format, Object arg) {
        wrapping.warn(applyPrefix(format), arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        wrapping.warn(applyPrefix(format), arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        wrapping.warn(applyPrefix(format), arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        wrapping.warn(applyPrefix(msg), t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return wrapping.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        wrapping.warn(marker, applyPrefix(msg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        wrapping.warn(marker, applyPrefix(format), arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        wrapping.warn(marker, applyPrefix(format), arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        wrapping.warn(marker, applyPrefix(format), arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        wrapping.warn(marker, applyPrefix(msg), t);
    }

    @Override
    public boolean isErrorEnabled() {
        return wrapping.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        wrapping.error(applyPrefix(msg));
    }

    @Override
    public void error(String format, Object arg) {
        wrapping.error(applyPrefix(format), arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        wrapping.error(applyPrefix(format), arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        wrapping.error(applyPrefix(format), arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        wrapping.error(applyPrefix(msg), t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return wrapping.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        wrapping.error(marker, applyPrefix(msg));
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        wrapping.error(marker, applyPrefix(format), arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        wrapping.error(marker, applyPrefix(format), arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        wrapping.error(marker, applyPrefix(format), arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        wrapping.error(marker, applyPrefix(msg), t);
    }
}
