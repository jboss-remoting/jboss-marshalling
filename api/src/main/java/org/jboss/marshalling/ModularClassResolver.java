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
import java.lang.reflect.Proxy;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * A class table which implements an alternate class resolution strategy based on JBoss Modules.
 * Each class name is stored along with its corresponding module identifier, which allows the object graph
 * to be exactly reconstituted on the remote side.  This class should be used when the marshalling and
 * unmarshalling side may have differing class files.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModularClassResolver implements ClassResolver {
    private final ModuleLoader moduleLoader;

    private ModularClassResolver(final ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
    }

    /**
     * Construct a new instance using the given module loader.
     *
     * @param moduleLoader the module loader
     * @return the new instance
     */
    public static ModularClassResolver getInstance(final ModuleLoader moduleLoader) {
        return new ModularClassResolver(moduleLoader);
    }

    /** {@inheritDoc} */
    public void annotateClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
        final Module module = Module.forClass(clazz);
        if (module == null) {
            marshaller.writeObject(null);
        } else {
            final ModuleIdentifier identifier = module.getIdentifier();
            marshaller.writeObject(identifier.getName());
            marshaller.writeObject(identifier.getSlot());
        }
    }

    /** {@inheritDoc} */
    public void annotateProxyClass(final Marshaller marshaller, final Class<?> proxyClass) throws IOException {
        final Module module = Module.forClass(proxyClass);
        if (module == null) {
            marshaller.writeObject(null);
        } else {
            final ModuleIdentifier identifier = module.getIdentifier();
            marshaller.writeObject(identifier.getName());
            marshaller.writeObject(identifier.getSlot());
        }
    }

    /** {@inheritDoc} */
    public String getClassName(final Class<?> clazz) throws IOException {
        return clazz.getName();
    }

    /** {@inheritDoc} */
    public String[] getProxyInterfaces(final Class<?> proxyClass) throws IOException {
        final Class<?>[] interfaces = proxyClass.getInterfaces();
        final String[] names = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            names[i] = getClassName(interfaces[i]);
        }
        return names;
    }

    /** {@inheritDoc} */
    public Class<?> resolveClass(final Unmarshaller unmarshaller, final String className, final long serialVersionUID) throws IOException, ClassNotFoundException {
        final String name = (String) unmarshaller.readObject();
        if (name == null) {
            return Class.forName(className, false, Module.class.getClassLoader());
        }
        final String slot = (String) unmarshaller.readObject();
        final ModuleIdentifier identifier = ModuleIdentifier.create(name, slot);
        try {
            return Class.forName(className, false, moduleLoader.loadModule(identifier).getClassLoader());
        } catch (ModuleLoadException e) {
            final InvalidClassException ce = new InvalidClassException(className, "Module load failed");
            ce.initCause(e);
            throw ce;
        }
    }

    /** {@inheritDoc} */
    public Class<?> resolveProxyClass(final Unmarshaller unmarshaller, final String[] names) throws IOException, ClassNotFoundException {
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
        final int len = names.length;
        final Class<?>[] interfaces = new Class<?>[len];
        for (int i = 0; i < len; i ++) {
            interfaces[i] = Class.forName(names[i], false, classLoader);
        }
        return Proxy.getProxyClass(classLoader, interfaces);
    }
}
