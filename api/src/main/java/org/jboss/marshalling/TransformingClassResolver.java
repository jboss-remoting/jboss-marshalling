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

import java.io.IOException;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class TransformingClassResolver implements ClassResolver {

    private final ClassResolver delegate;
    private final ClassNameTransformer transformer;
    private final boolean in;

    TransformingClassResolver(final ClassResolver delegate, final ClassNameTransformer transformer, final boolean in) {
        this.delegate = delegate;
        this.transformer = transformer;
        this.in = in;
    }

    @Override
    public void annotateClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
        delegate.annotateClass(marshaller, clazz);
    }

    @Override
    public void annotateProxyClass(final Marshaller marshaller, final Class<?> proxyClass) throws IOException {
        delegate.annotateProxyClass(marshaller, proxyClass);
    }

    @Override
    public String getClassName(final Class<?> clazz) throws IOException {
        return translate(delegate.getClassName(clazz));
    }

    @Override
    public String[] getProxyInterfaces(final Class<?> proxyClass) throws IOException {
        final String[] retVal = delegate.getProxyInterfaces(proxyClass);
        if (retVal != null) {
            for (int i = 0; i < retVal.length; i++) {
                retVal[i] = translate(retVal[i]);
            }
        }
        return retVal;
    }

    private String translate(final String className) {
        if (in) {
            return transformer.transformInput(className);
        } else {
            return transformer.transformOutput(className);
        }
    }

    @Override
    public Class<?> resolveClass(final Unmarshaller unmarshaller, final String name, final long serialVersionUID) throws IOException, ClassNotFoundException {
        return delegate.resolveClass(unmarshaller, translate(name), serialVersionUID);
    }

    @Override
    public Class<?> resolveProxyClass(final Unmarshaller unmarshaller, final String[] interfaces) throws IOException, ClassNotFoundException {
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                interfaces[i] = translate(interfaces[i]);
            }
        }
        return delegate.resolveProxyClass(unmarshaller, interfaces);
    }

}
