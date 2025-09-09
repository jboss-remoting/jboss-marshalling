package org.jboss.marshalling;

import java.io.IOException;

/**
 * Some helper methods for module stuff.
 */
final class ModuleHelpers {
    private ModuleHelpers() {}

    static String readModuleName(Unmarshaller u) throws IOException, ClassNotFoundException {
        String name = u.readObject(String.class);
        if (name == null) {
            return null;
        } else {
            // read "slot"
            final String slot = u.readObject(String.class);
            final String identifier;
            if (slot == null || slot.equals("main")) {
                identifier = name;
            } else {
                // slots are deprecated and historically not used, but provide token minimal support
                identifier = name + ":" + slot;
            }
            return identifier;
        }
    }

    static void writeModuleName(Marshaller marshaller, String name) throws IOException {
        int colon = name.indexOf(':');
        if (colon != -1) {
            marshaller.writeObject(name.substring(0, colon));
            marshaller.writeObject(name.substring(colon + 1));
        } else {
            marshaller.writeObject(name);
            marshaller.writeObject("main");
        }
    }
}
