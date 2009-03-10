/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
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

package org.jboss.marshalling.serialization.jboss;

import org.jboss.serial.objectmetamodel.DataContainerConstants;

/**
 *
 */
public final class Protocol implements DataContainerConstants {
   
    private static final byte BASE = NULLREF + 10;

    public static final byte ID_PREDEFINED_OBJECT        = BASE + 0x01;
    public static final byte ID_NEW_OBJECT               = BASE + 0x02;
    public static final byte ID_PROXY_OBJECT             = BASE + 0x03;

    public static final byte ID_NO_CLASS_DESC            = BASE + 0x04;
    public static final byte ID_ORDINARY_CLASS           = BASE + 0x05;
    public static final byte ID_PROXY_CLASS              = BASE + 0x06;
    public static final byte ID_EXTERNALIZER_CLASS       = BASE + 0x07;
    public static final byte ID_PREDEFINED_CLASS         = BASE + 0x08;
    public static final byte ID_RESOLVED_CLASS           = BASE + 0x09;

    private Protocol() {
    }
}
