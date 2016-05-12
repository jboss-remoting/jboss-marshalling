/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * The version of the Marshalling API.
 * @apiviz.exclude
 */
public final class Version {

    static {
        String jarName = "(unknown)";
        String versionString = "(unknown)";
        try (final InputStream stream = Version.class.getResourceAsStream("Version.properties")) {
            if (stream != null) try (final InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Properties versionProps = new Properties();
                versionProps.load(reader);
                jarName = versionProps.getProperty("jarName", jarName);
                versionString = versionProps.getProperty("version", versionString);
            }
        } catch (IOException ignored) {
        }
        JAR_NAME = jarName;
        VERSION = versionString;
    }

    private Version() {
    }

    static final String JAR_NAME;

    /**
     * The version.
     */
    public static final String VERSION;

    /**
     * Get the version string.
     *
     * @return the version string
     */
    public static String getVersionString() {
        return VERSION;
    }

    /**
     * Get the JAR file name.
     *
     * @return the JAR file name
     */
    public static String getJarName() {
        return JAR_NAME;
    }

    /**
     * Print the version to {@code System.out}.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.printf("JBoss Marshalling version %s\n", VERSION);
    }
}
