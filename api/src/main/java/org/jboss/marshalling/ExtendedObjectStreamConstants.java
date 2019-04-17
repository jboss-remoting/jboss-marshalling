/**
 * 
 */
package org.jboss.marshalling;

import java.io.ObjectStreamConstants;
import java.io.SerializablePermission;

/**
 * @author wangc
 *
 */
interface ExtendedObjectStreamConstants extends ObjectStreamConstants {

    /**
     * 
     * Enable setting the process-wide serial filter.
     * 
     * @see org.jboss.marshalling.MarshallingConfiguration.setSerialFilter(InputFilter)
     * 
     * @since 9
     * 
     */

    SerializablePermission SERIAL_FILTER_PERMISSION = new SerializablePermission("serialFilter");
}
