/**
 * The marshalling API.  Marshalling is done by use of {@link org.jboss.marshalling.Marshaller Marshaller} and {@link org.jboss.marshalling.Unmarshaller Unmarshaller} instances.  These
 * instances are acquired from a {@link org.jboss.marshalling.MarshallerFactory MarshallerFactory} using a {@link MarshallingConfiguration Configuration} to configure them.  The
 * default implementation is the River protocol, usable by way of the {@link org.jboss.marshalling.river} package.
 */
package org.jboss.marshalling;
