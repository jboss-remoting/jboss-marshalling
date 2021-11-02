/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.marshalling;

import static java.lang.Math.abs;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JavaEE2JakartaEEClassNameTransformer implements ClassNameTransformer {

    private static final String JAVAEE_PACKAGE_PREFIX = "javax.";
    private static final String JAKARTAEE_PACKAGE_PREFIX = "jakarta.";
    /**
     * Extracted from Batavia's Eclipse Transformer jakarta-renames.properties file
     */
    private static final String[] subpackages = new String[] {
            "activation.",
            "annotation.",
            "batch.",
            "decorator.",
            "ejb.",
            "el.",
            "enterprise.",
            "faces.",
            "inject.",
            "interceptor.",
            "jms.",
            "json.",
            "jws.",
            "mail.",
            "persistence.",
            "resource.",
            "security.auth.message.",
            "security.enterprise.",
            "security.jacc.",
            "servlet.",
            "transaction.",
            "validation.",
            "websocket.",
            "ws.rs.",
            "xml.bind.",
            "xml.soap.",
            "xml.ws."
    };

    static {
        sort(subpackages); // finalize data for use in binary search
    }

    @Override
    public String transformInput(final String className) {
        return transform(className, JAVAEE_PACKAGE_PREFIX, JAKARTAEE_PACKAGE_PREFIX);
    }

    @Override
    public String transformOutput(final String className) {
        return transform(className, JAKARTAEE_PACKAGE_PREFIX, JAVAEE_PACKAGE_PREFIX);
    }

    private String transform(final String className, final String sourcePrefix, final String targetPrefix) {
        if (className.startsWith(sourcePrefix)) {
            final String classNameNoPrefix = className.substring(sourcePrefix.length());
            final int index = abs(binarySearch(subpackages, classNameNoPrefix)) - 2;
            return index >= 0 && classNameNoPrefix.startsWith(subpackages[index]) ? targetPrefix + classNameNoPrefix : className;
        }
        return className;
    }

}
