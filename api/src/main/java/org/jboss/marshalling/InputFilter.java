/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Filter that implementations of {@link Unmarshaller} can apply after resolving the class to decide if deserialization has to stop.
 */
@FunctionalInterface
public interface InputFilter {

  Status checkInput(FilterInfo info);

  enum Status {
    /**
     * The status is undecided, not allowed and not rejected.
     */
    UNDECIDED,
    /**
     * The status is allowed.
     */
    ALLOWED,
    /**
     * The status is rejected.
     */
    REJECTED;
}

  interface FilterInfo {

    Class<?> serialClass();

    long arrayLength();

    long depth();

    long references();

    long streamBytes();

  }

  final static class Global implements InputFilter {
      /**
       * The pattern used to create the filter.
       */
      private final String pattern;
      /**
       * The list of class filters.
       */
      private final List<Function<Class<?>, Status>> filters;
      /**
       * Maximum allowed bytes in the stream.
       */
      private long maxStreamBytes;
      /**
       * Maximum depth of the graph allowed.
       */
      private long maxDepth;
      /**
       * Maximum number of references in a graph.
       */
      private long maxReferences;
      /**
       * Maximum length of any array.
       */
      private long maxArrayLength;

      /**
       * Returns an InputFilter from a string of patterns.
       *
       * @param pattern the pattern string to parse
       * @return a filter to check a class being deserialized; not null
       * @throws IllegalArgumentException if the parameter is malformed
       *                if the pattern is missing the name, the long value
       *                is not a number or is negative.
       */
      public static InputFilter createFilter(String pattern) {
          Global filter = new Global(pattern);
          return filter.isEmpty() ? null : filter;
      }

      /**
       * Construct a new filter from the pattern String.
       *
       * @param pattern a pattern string of filters
       * @throws IllegalArgumentException if the pattern is malformed
       */
      private Global(String pattern) {
          this.pattern = pattern;

          maxArrayLength = Long.MAX_VALUE; // Default values are unlimited
          maxDepth = Long.MAX_VALUE;
          maxReferences = Long.MAX_VALUE;
          maxStreamBytes = Long.MAX_VALUE;

          String[] patterns = pattern.split(";");
          filters = new ArrayList<>(patterns.length);
          for (int i = 0; i < patterns.length; i++) {
              String p = patterns[i];
              int nameLen = p.length();
              if (nameLen == 0) {
                  continue;
              }
              if (parseLimit(p)) {
                  // If the pattern contained a limit setting, i.e. type=value
                  continue;
              }
              boolean negate = p.charAt(0) == '!';
              int poffset = negate ? 1 : 0;

              // JBMAR-193 No need to check module name
              // // isolate module name, if any
              // int slash = p.indexOf('/', poffset);
              // if (slash == poffset) {
              // throw new IllegalArgumentException("module name is missing in: \"" + pattern + "\"");
              // }
              // final String moduleName = (slash >= 0) ? p.substring(poffset, slash) : null;
              // poffset = (slash >= 0) ? slash + 1 : poffset;

              final Function<Class<?>, Status> patternFilter;
              if (p.endsWith("*")) {
                  // Wildcard cases
                  if (p.endsWith(".*")) {
                      // Pattern is a package name with a wildcard
                      final String pkg = p.substring(poffset, nameLen - 1);
                      if (pkg.length() < 2) {
                          throw new IllegalArgumentException("package missing in: \"" + pattern + "\"");
                      }
                      if (negate) {
                          // A Function that fails if the class starts with the pattern, otherwise don't care
                          patternFilter = c -> matchesPackage(c, pkg) ? Status.REJECTED : Status.UNDECIDED;
                      } else {
                          // A Function that succeeds if the class starts with the pattern, otherwise don't care
                          patternFilter = c -> matchesPackage(c, pkg) ? Status.ALLOWED : Status.UNDECIDED;
                      }
                  } else if (p.endsWith(".**")) {
                      // Pattern is a package prefix with a double wildcard
                      final String pkgs = p.substring(poffset, nameLen - 2);
                      if (pkgs.length() < 2) {
                          throw new IllegalArgumentException("package missing in: \"" + pattern + "\"");
                      }
                      if (negate) {
                          // A Function that fails if the class starts with the pattern, otherwise don't care
                          patternFilter = c -> c.getName().startsWith(pkgs) ? Status.REJECTED : Status.UNDECIDED;
                      } else {
                          // A Function that succeeds if the class starts with the pattern, otherwise don't care
                          patternFilter = c -> c.getName().startsWith(pkgs) ? Status.ALLOWED : Status.UNDECIDED;
                      }
                  } else {
                      // Pattern is a classname (possibly empty) with a trailing wildcard
                      final String className = p.substring(poffset, nameLen - 1);
                      if (negate) {
                          // A Function that fails if the class starts with the pattern, otherwise don't care
                          patternFilter = c -> c.getName().startsWith(className) ? Status.REJECTED : Status.UNDECIDED;
                      } else {
                          // A Function that succeeds if the class starts with the pattern, otherwise don't care
                          patternFilter = c -> c.getName().startsWith(className) ? Status.ALLOWED : Status.UNDECIDED;
                      }
                  }
              } else {
                  final String name = p.substring(poffset);
                  if (name.isEmpty()) {
                      throw new IllegalArgumentException("class or package missing in: \"" + pattern + "\"");
                  }
                  // Pattern is a class name
                  if (negate) {
                      // A Function that fails if the class equals the pattern, otherwise don't care
                      patternFilter = c -> c.getName().equals(name) ? Status.REJECTED : Status.UNDECIDED;
                  } else {
                      // A Function that succeeds if the class equals the pattern, otherwise don't care
                      patternFilter = c -> c.getName().equals(name) ? Status.ALLOWED : Status.UNDECIDED;
                  }
              }
              // // JBMAR-193 No need to check module name
              // If there is a moduleName, combine the module name check with the package/class check
              // if (moduleName == null) {
              // filters.add(patternFilter);
              // } else {
              // filters.add(c -> moduleName.equals(c.getModule().getName()) ? patternFilter.apply(c) : Status.UNDECIDED);
              // }
              filters.add(patternFilter);
          }
      }

      /**
       * Returns if this filter has any checks.
       * @return {@code true} if the filter has any checks, {@code false} otherwise
       */
      private boolean isEmpty() {
          return filters.isEmpty() &&
                  maxArrayLength == Long.MAX_VALUE &&
                  maxDepth == Long.MAX_VALUE &&
                  maxReferences == Long.MAX_VALUE &&
                  maxStreamBytes == Long.MAX_VALUE;
      }

      /**
       * Parse out a limit for one of maxarray, maxdepth, maxbytes, maxreferences.
       *
       * @param pattern a string with a type name, '=' and a value
       * @return {@code true} if a limit was parsed, else {@code false}
       * @throws IllegalArgumentException if the pattern is missing
       *                the name, the Long value is not a number or is negative.
       */
      private boolean parseLimit(String pattern) {
          int eqNdx = pattern.indexOf('=');
          if (eqNdx < 0) {
              // not a limit pattern
              return false;
          }
          String valueString = pattern.substring(eqNdx + 1);
          if (pattern.startsWith("maxdepth=")) {
              maxDepth = parseValue(valueString);
          } else if (pattern.startsWith("maxarray=")) {
              maxArrayLength = parseValue(valueString);
          } else if (pattern.startsWith("maxrefs=")) {
              maxReferences = parseValue(valueString);
          } else if (pattern.startsWith("maxbytes=")) {
              maxStreamBytes = parseValue(valueString);
          } else {
              throw new IllegalArgumentException("unknown limit: " + pattern.substring(0, eqNdx));
          }
          return true;
      }

      /**
       * Parse the value of a limit and check that it is non-negative.
       * @param string inputstring
       * @return the parsed value
       * @throws IllegalArgumentException if parsing the value fails or the value is negative
       */
      private static long parseValue(String string) throws IllegalArgumentException {
          // Parse a Long from after the '=' to the end
          long value = Long.parseLong(string);
          if (value < 0) {
              throw new IllegalArgumentException("negative limit: " + string);
          }
          return value;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Status checkInput(FilterInfo filterInfo) {
          if (filterInfo.references() < 0
                  || filterInfo.depth() < 0
                  || filterInfo.streamBytes() < 0
                  || filterInfo.references() > maxReferences
                  || filterInfo.depth() > maxDepth
                  || filterInfo.streamBytes() > maxStreamBytes) {
              return Status.REJECTED;
          }

          Class<?> clazz = filterInfo.serialClass();
          if (clazz != null) {
              if (clazz.isArray()) {
                  if (filterInfo.arrayLength() >= 0 && filterInfo.arrayLength() > maxArrayLength) {
                      // array length is too big
                      return Status.REJECTED;
                  }
                  do {
                      // Arrays are decided based on the component type
                      clazz = clazz.getComponentType();
                  } while (clazz.isArray());
              }

              if (clazz.isPrimitive())  {
                  // Primitive types are undecided; let someone else decide
                  return Status.UNDECIDED;
              } else {
                  // Find any filter that allowed or rejected the class
                  final Class<?> cl = clazz;
                  Optional<Status> status = filters.stream()
                          .map(f -> f.apply(cl))
                          .filter(p -> p != Status.UNDECIDED)
                          .findFirst();
                  return status.orElse(Status.UNDECIDED);
              }
          }
          return Status.UNDECIDED;
      }

      /**
       * Returns {@code true} if the class is in the package.
       *
       * @param c   a class
       * @param pkg a package name (including the trailing ".")
       * @return {@code true} if the class is in the package,
       * otherwise {@code false}
       */
      private static boolean matchesPackage(Class<?> c, String pkg) {
          String n = c.getName();
          return n.startsWith(pkg) && n.lastIndexOf('.') == pkg.length() - 1;
      }

      /**
       * Returns the pattern used to create this filter.
       * @return the pattern used to create this filter
       */
      @Override
      public String toString() {
          return pattern;
      }
  }

}
