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

import static org.jboss.marshalling.ClassNameTransformer.JAVAEE_TO_JAKARTAEE;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test case for {@link org.jboss.marshalling.ClassNameTransformer#JAVAEE_TO_JAKARTAEE} default translator.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class Javaee2JakartaeeMappingTestCase {
    @Test
    public void testTransformInput() {
        assertEquals(JAVAEE_TO_JAKARTAEE.transformInput("javax.servlet.ServletRequest"), "jakarta.servlet.ServletRequest");
        assertEquals(JAVAEE_TO_JAKARTAEE.transformInput("javax.servleta.ServletRequest"), "javax.servleta.ServletRequest");
        assertEquals(JAVAEE_TO_JAKARTAEE.transformInput("javax.security.auth.message.Principal"), "jakarta.security.auth.message.Principal");
        assertEquals(JAVAEE_TO_JAKARTAEE.transformInput("javax.Bar"), "javax.Bar");
    }

    @Test
    public void testTransformOutput() {
        assertEquals(JAVAEE_TO_JAKARTAEE.transformOutput("jakarta.el.ELContextListener"), "javax.el.ELContextListener");
        assertEquals(JAVAEE_TO_JAKARTAEE.transformOutput("jakarta.xml.bind.Element"), "javax.xml.bind.Element");
        assertEquals(JAVAEE_TO_JAKARTAEE.transformOutput("jakarta.xml.binda.Element"), "jakarta.xml.binda.Element");
        assertEquals(JAVAEE_TO_JAKARTAEE.transformOutput("jakarta.Foo"), "jakarta.Foo");
    }
}
