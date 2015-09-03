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
package ninja.leaping.permissionsex.logging;

import ninja.leaping.permissionsex.util.Translatable;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.Locale;

public interface TranslatableLogger extends Logger {

    static TranslatableLogger forLogger(Logger logger) {
        return logger instanceof TranslatableLogger ? ((TranslatableLogger) logger) : new WrappingTranslatableLogger(logger);
    }

    Locale getLogLocale();

    Locale getLogLocale(Marker marker);

    /**
     * Log a message at the TRACE level.
     *
     * @param msg the translatable message to be logged
     * @since 1.4
     */
    default void trace(Translatable msg) {
        if (isTraceEnabled()) {
            trace(msg.translateFormatted(getLogLocale()));
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
    default void trace(Translatable msg, Throwable t) {
        if (isTraceEnabled()) {
            trace(msg.translateFormatted(getLogLocale()), t);
        }

    }

    /**
     * Log a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @since 1.4
     */
    default void trace(Marker marker, Translatable msg) {
        if (isTraceEnabled(marker)) {
            trace(marker, msg.translateFormatted(getLogLocale(marker)));
        }

    }

    /**
     * This method is similar to {@link #trace(Translatable, Throwable)} method except that the
     * marker data is also taken into consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     * @since 1.4
     */
    default void trace(Marker marker, Translatable msg, Throwable t) {
        if (isTraceEnabled(marker)) {
            trace(marker, msg.translateFormatted(getLogLocale(marker)), t);
        }

    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param msg the message string to be logged
     */
    default void debug(Translatable msg) {
        if (isDebugEnabled()) {
            debug(msg.translateFormatted(getLogLocale()));
        }
    }

    /**
     * Log an exception (throwable) at the DEBUG level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void debug(Translatable msg, Throwable t) {
        if (isDebugEnabled()) {
            debug(msg.translateFormatted(getLogLocale()), t);
        }

    }

    /**
     * Log a message with the specific Marker at the DEBUG level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    default void debug(Marker marker, Translatable msg) {
        if (isDebugEnabled(marker)) {
            debug(marker, msg.translateFormatted(getLogLocale(marker)));
        }

    }

    /**
     * This method is similar to {@link #debug(Translatable, Throwable)} method except that the
     * marker data is also taken into consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void debug(Marker marker, Translatable msg, Throwable t) {
        if (isDebugEnabled(marker)) {
            debug(marker, msg.translateFormatted(getLogLocale(marker)), t);
        }

    }

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    default void info(Translatable msg) {
        if (isInfoEnabled()) {
            info(msg.translateFormatted(getLogLocale()));
        }

    }

    /**
     * Log an exception (throwable) at the INFO level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void info(Translatable msg, Throwable t) {
        if (isInfoEnabled()) {
            info(msg.translateFormatted(getLogLocale()), t);
        }

    }

    /**
     * Log a message with the specific Marker at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    default void info(Marker marker, Translatable msg) {
        if (isInfoEnabled(marker)) {
            info(marker, msg.translateFormatted(getLogLocale(marker)));
        }

    }

    /**
     * This method is similar to {@link #info(Translatable, Throwable)} method
     * except that the marker data is also taken into consideration.
     *
     * @param marker the marker data for this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void info(Marker marker, Translatable msg, Throwable t) {
        if (isInfoEnabled(marker)) {
            info(marker, msg.translateFormatted(getLogLocale(marker)), t);
        }

    }

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    default void warn(Translatable msg) {
        if (isWarnEnabled()) {
            warn(msg.translateFormatted(getLogLocale()));
        }

    }

    /**
     * Log an exception (throwable) at the WARN level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void warn(Translatable msg, Throwable t) {
        if (isWarnEnabled()) {
            warn(msg.translateFormatted(getLogLocale()), t);
        }

    }

    /**
     * Log a message with the specific Marker at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    default void warn(Marker marker, Translatable msg) {
        if (isWarnEnabled(marker)) {
            warn(marker, msg.translateFormatted(getLogLocale(marker)));
        }

    }

    /**
     * This method is similar to {@link #warn(Translatable, Throwable)} method
     * except that the marker data is also taken into consideration.
     *
     * @param marker the marker data for this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void warn(Marker marker, Translatable msg, Throwable t) {
        if (isWarnEnabled(marker)) {
            warn(marker, msg.translateFormatted(getLogLocale(marker)), t);
        }

    }

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    default void error(Translatable msg) {
        if (isErrorEnabled()) {
            error(msg.translateFormatted(getLogLocale()));
        }

    }

    /**
     * Log an exception (throwable) at the ERROR level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    default void error(Translatable msg, Throwable t) {
        if (isErrorEnabled()) {
            error(msg.translateFormatted(getLogLocale()), t);
        }

    }

    /**
     * Log a message with the specific Marker at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    default void error(Marker marker, Translatable msg) {
        if (isErrorEnabled(marker)) {
            error(marker, msg.translateFormatted(getLogLocale(marker)));
        }

    }

    /**
     * This method is similar to {@link #error(Translatable, Throwable)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    default void error(Marker marker, Translatable msg, Throwable t) {
        if (isErrorEnabled(marker)) {
            error(marker, msg.translateFormatted(getLogLocale(marker)), t);
        }

    }
}
