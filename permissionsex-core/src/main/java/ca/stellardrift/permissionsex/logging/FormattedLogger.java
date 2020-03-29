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

import ca.stellardrift.permissionsex.util.PEXComponentRenderer;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.Locale;

public interface FormattedLogger extends Logger {

    static FormattedLogger forLogger(Logger logger, boolean supportsFormatting) {
        return logger instanceof FormattedLogger ? ((FormattedLogger) logger) : new WrappingFormattedLogger(logger, supportsFormatting);
    }

    Locale getLogLocale();

    Locale getLogLocale(Marker marker);

    @Nullable
    String getPrefix();

    void setPrefix(@Nullable String prefix);

    ComponentSerializer<Component, ?, String> getSerializer();

    default String formatText(Component component) {
        return formatText(component, null);
    }

    default String formatText(Component component, Marker marker) {
        return getSerializer().serialize(PEXComponentRenderer.INSTANCE.render(component, getLogLocale(marker)));
    }

    /**
     * Log a message at the TRACE level.
     *
     * @param msg the translatable message to be logged
     * @since 1.4
     */
    default void trace(Component msg) {
        if (isTraceEnabled()) {
            trace(formatText(msg));
        }
    }

    /**
     * Log an exception (throwable) at the TRACE level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     * @since 1.4
     */
    default void trace(Component msg, Throwable t) {
        if (isTraceEnabled()) {
            trace(formatText(msg), t);
        }

    }

    /**
     * Log a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @since 1.4
     */
    default void trace(Marker marker, Component msg) {
        if (isTraceEnabled(marker)) {
            trace(marker, formatText(msg, marker));
        }

    }

    /**
     * This method is similar to {@link #trace(Component, Throwable)} method except that the
     * marker data is also taken into consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     * @since 1.4
     */
    default void trace(Marker marker, Component msg, Throwable t) {
        if (isTraceEnabled(marker)) {
            trace(marker, formatText(msg, marker), t);
        }

    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param msg the message string to be logged
     */
    default void debug(Component msg) {
        if (isDebugEnabled()) {
            debug(formatText(msg));
        }
    }

    /**
     * Log an exception (throwable) at the DEBUG level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void debug(Component msg, Throwable t) {
        if (isDebugEnabled()) {
            debug(formatText(msg), t);
        }

    }

    /**
     * Log a message with the specific Marker at the DEBUG level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    default void debug(Marker marker, Component msg) {
        if (isDebugEnabled(marker)) {
            debug(marker, formatText(msg, marker));
        }

    }

    /**
     * This method is similar to {@link #debug(Component, Throwable)} method except that the
     * marker data is also taken into consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void debug(Marker marker, Component msg, Throwable t) {
        if (isDebugEnabled(marker)) {
            debug(marker, formatText(msg, marker), t);
        }

    }

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    default void info(Component msg) {
        if (isInfoEnabled()) {
            info(formatText(msg));
        }

    }

    /**
     * Log an exception (throwable) at the INFO level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void info(Component msg, Throwable t) {
        if (isInfoEnabled()) {
            info(formatText(msg), t);
        }

    }

    /**
     * Log a message with the specific Marker at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    default void info(Marker marker, Component msg) {
        if (isInfoEnabled(marker)) {
            info(marker, formatText(msg, marker));
        }

    }

    /**
     * This method is similar to {@link #info(Component, Throwable)} method
     * except that the marker data is also taken into consideration.
     *
     * @param marker the marker data for this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void info(Marker marker, Component msg, Throwable t) {
        if (isInfoEnabled(marker)) {
            info(marker, formatText(msg, marker), t);
        }

    }

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    default void warn(Component msg) {
        if (isWarnEnabled()) {
            warn(formatText(msg));
        }

    }

    /**
     * Log an exception (throwable) at the WARN level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void warn(Component msg, Throwable t) {
        if (isWarnEnabled()) {
            warn(formatText(msg), t);
        }

    }

    /**
     * Log a message with the specific Marker at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    default void warn(Marker marker, Component msg) {
        if (isWarnEnabled(marker)) {
            warn(marker, formatText(msg, marker));
        }

    }

    /**
     * This method is similar to {@link #warn(Component, Throwable)} method
     * except that the marker data is also taken into consideration.
     *
     * @param marker the marker data for this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void warn(Marker marker, Component msg, Throwable t) {
        if (isWarnEnabled(marker)) {
            warn(marker, formatText(msg, marker), t);
        }

    }

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    default void error(Component msg) {
        if (isErrorEnabled()) {
            error(formatText(msg));
        }

    }

    /**
     * Log an exception (throwable) at the ERROR level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void error(Component msg, Throwable t) {
        if (isErrorEnabled()) {
            error(formatText(msg), t);
        }

    }

    /**
     * Log a message with the specific Marker at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    default void error(Marker marker, Component msg) {
        if (isErrorEnabled(marker)) {
            error(marker, formatText(msg, marker));
        }

    }

    /**
     * This method is similar to {@link #error(Component, Throwable)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void error(Marker marker, Component msg, Throwable t) {
        if (isErrorEnabled(marker)) {
            error(marker, formatText(msg, marker), t);
        }

    }
}
