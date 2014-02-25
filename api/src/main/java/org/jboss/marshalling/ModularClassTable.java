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
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Proxy;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * A class table which implements an alternate class resolution strategy based on JBoss Modules.
 * Each class name is stored along with its corresponding module identifier, which allows the object graph
 * to be exactly reconstituted on the remote side.  This class should only be used when the marshalling
 * and unmarshalling side share the same class files.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModularClassTable implements ClassTable {

    private static final Writer PROXY_WRITER = new ProxyWriter();
    private static final Writer CLASS_WRITER = new ClassWriter();

    private final ModuleLoader moduleLoader;

    private ModularClassTable(final ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
    }

    /**
     * Get an instance using the given module loader.
     *
     * @param moduleLoader the module loader to use
     * @return the modular class table
     */
    public static ModularClassTable getInstance(final ModuleLoader moduleLoader) {
        return new ModularClassTable(moduleLoader);
    }

    /** {@inheritDoc} */
    public Writer getClassWriter(final Class<?> clazz) throws IOException {
        return Proxy.isProxyClass(clazz) ? PROXY_WRITER : CLASS_WRITER;
    }

    /** {@inheritDoc} */
    public Class<?> readClass(final Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        final byte b = unmarshaller.readByte();
        switch (b) {
            case 0: {
                final String name = (String) unmarshaller.readObject();
                final ClassLoader classLoader;
                final String className;
                if (name == null) {
                    classLoader = Module.class.getClassLoader();
                    className = (String) unmarshaller.readObject();
                } else {
                    final String slot = (String) unmarshaller.readObject();
                    final ModuleIdentifier identifier = ModuleIdentifier.create(name, slot);
                    className = (String) unmarshaller.readObject();
                    try {
                        classLoader = moduleLoader.loadModule(identifier).getClassLoader();
                    } catch (ModuleLoadException e) {
                        final InvalidClassException ce = new InvalidClassException(className, "Module load failed");
                        ce.initCause(e);
                        throw ce;
                    }
                }
                return Class.forName(className, false, classLoader);
            }
            case 1: {
                final String name = (String) unmarshaller.readObject();
                final ClassLoader classLoader;
                if (name == null) {
                    classLoader = Module.class.getClassLoader();
                } else {
                    final String slot = (String) unmarshaller.readObject();
                    final ModuleIdentifier identifier = ModuleIdentifier.create(name, slot);
                    final Module module;
                    try {
                        module = moduleLoader.loadModule(identifier);
                    } catch (ModuleLoadException e) {
                        final InvalidClassException ce = new InvalidClassException("Module load failed");
                        ce.initCause(e);
                        throw ce;
                    }
                    classLoader = module.getClassLoader();
                }
                final int len = unmarshaller.readInt();
                final Class<?>[] interfaces = new Class<?>[len];
                for (int i = 0; i < len; i ++) {
                    interfaces[i] = Class.forName((String) unmarshaller.readObject(), false, classLoader);
                }
                return Proxy.getProxyClass(classLoader, interfaces);
            }
            default: throw new StreamCorruptedException(String.format("Invalid class type byte: %02x", Integer.valueOf(b & 0xff)));
        }
    }

    private static final class ClassWriter implements Writer {
        public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
            marshaller.write(0);
            final Module module = Module.forClass(clazz);
            if (module == null) {
                marshaller.writeObject(null);
            } else {
                final ModuleIdentifier identifier = module.getIdentifier();
                marshaller.writeObject(identifier.getName());
                marshaller.writeObject(identifier.getSlot());
            }
            marshaller.writeObject(clazz.getName());
        }
    }

    private static final class ProxyWriter implements Writer {
        public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
            marshaller.write(1);
            final Module module = Module.forClass(clazz);
            if (module == null) {
                marshaller.writeObject(null);
            } else {
                final ModuleIdentifier identifier = module.getIdentifier();
                marshaller.writeObject(identifier.getName());
                marshaller.writeObject(identifier.getSlot());
            }
            final Class<?>[] interfaces = clazz.getInterfaces();
            marshaller.writeInt(interfaces.length);
            for (Class<?> interfaze : interfaces) {
                marshaller.writeObject(interfaze.getName());
            }
        }
    }
}
