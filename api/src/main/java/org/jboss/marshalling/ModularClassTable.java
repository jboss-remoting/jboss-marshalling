/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.marshalling;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Proxy;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
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
                final ModuleIdentifier identifier = ModuleIdentifier.create(
                        (String) unmarshaller.readObject(),
                        (String) unmarshaller.readObject()
                );
                final String className = (String) unmarshaller.readObject();
                try {
                    return Class.forName(className, false, moduleLoader.loadModule(identifier).getClassLoader());
                } catch (ModuleLoadException e) {
                    final InvalidClassException ce = new InvalidClassException(className, "Module load failed");
                    ce.initCause(e);
                    throw ce;
                }
            }
            case 1: {
                final ModuleIdentifier identifier = ModuleIdentifier.create(
                        (String) unmarshaller.readObject(),
                        (String) unmarshaller.readObject()
                );
                final Module module;
                try {
                    module = moduleLoader.loadModule(identifier);
                } catch (ModuleLoadException e) {
                    final InvalidClassException ce = new InvalidClassException("Module load failed");
                    ce.initCause(e);
                    throw ce;
                }
                final ModuleClassLoader classLoader = module.getClassLoader();
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
                throw new InvalidClassException(clazz.getName(), "Class is not present in any module");
            }
            final ModuleIdentifier identifier = module.getIdentifier();
            marshaller.writeObject(identifier.getName());
            marshaller.writeObject(identifier.getSlot());
            marshaller.writeObject(clazz.getName());
        }
    }

    private static final class ProxyWriter implements Writer {
        public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
            marshaller.write(1);
            final Module module = Module.forClass(clazz);
            if (module == null) {
                throw new InvalidClassException(clazz.getName(), "Class is not present in any module");
            }
            final ModuleIdentifier identifier = module.getIdentifier();
            marshaller.writeObject(identifier.getName());
            marshaller.writeObject(identifier.getSlot());
            final Class<?>[] interfaces = clazz.getInterfaces();
            marshaller.writeInt(interfaces.length);
            for (Class<?> interfaze : interfaces) {
                marshaller.writeObject(interfaze.getName());
            }
        }
    }
}
