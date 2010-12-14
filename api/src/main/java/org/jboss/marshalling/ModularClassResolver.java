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
import java.lang.reflect.Proxy;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
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
            throw new InvalidClassException(clazz.getName(), "Class is not present in any module");
        }
        final ModuleIdentifier identifier = module.getIdentifier();
        marshaller.writeObject(identifier.getName());
        marshaller.writeObject(identifier.getSlot());
    }

    /** {@inheritDoc} */
    public void annotateProxyClass(final Marshaller marshaller, final Class<?> proxyClass) throws IOException {
        final Module module = Module.forClass(proxyClass);
        if (module == null) {
            throw new InvalidClassException(proxyClass.getName(), "Class is not present in any module");
        }
        final ModuleIdentifier identifier = module.getIdentifier();
        marshaller.writeObject(identifier.getName());
        marshaller.writeObject(identifier.getSlot());
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
        final ModuleIdentifier identifier = ModuleIdentifier.create(
                (String) unmarshaller.readObject(),
                (String) unmarshaller.readObject()
        );
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
        final int len = names.length;
        final Class<?>[] interfaces = new Class<?>[len];
        for (int i = 0; i < len; i ++) {
            interfaces[i] = Class.forName(names[i], false, classLoader);
        }
        return Proxy.getProxyClass(classLoader, interfaces);
    }
}
