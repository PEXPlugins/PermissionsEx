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
package ca.stellardrift.permissionsex.exception;

import org.slf4j.Logger;

import java.net.URL;
import java.nio.file.Path;

/**
 * A way to collect information about the environment when an error occurs
 */
public interface ErrorReport {

    static ErrorReport.Visitor report() {
        throw new UnsupportedOperationException("nyi");
    }

    ErrorReport write(final Path file);

    ErrorReport write(final Logger logger);

    void printDescription(final Logger logger);

    /**
     * Upload this error report to a paste service
     * @return
     */
    URL upload();

    String toString();


    interface Visitor {

        Visitor exception(final Throwable error);

        Visitor description(final String description);

        Visitor beginSection(final String header);

        ListStage beginList();

        Visitor endSection();

        interface ListStage {
            ListStage element(final String element);

            Visitor endList();
        }
    }




}
