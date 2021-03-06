/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2010 Sun Microsystems, Inc.
 * Portions copyright 2012-2016 ForgeRock AS.
 */
package org.forgerock.opendj.ldap.requests;

import java.util.Collections;
import java.util.List;

import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Functions;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.ControlDecoder;
import org.forgerock.opendj.ldap.controls.GenericControl;
import org.forgerock.util.Reject;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

import com.forgerock.opendj.util.Collections2;

/**
 * Unmodifiable request implementation.
 *
 * @param <R>
 *            The type of request.
 */
abstract class AbstractUnmodifiableRequest<R extends Request> implements Request {

    protected final R impl;

    AbstractUnmodifiableRequest(final R impl) {
        this.impl = impl;
    }

    @Override
    public final R addControl(final Control control) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsControl(final String oid) {
        return impl.containsControl(oid);
    }

    @Override
    public final <C extends Control> C getControl(final ControlDecoder<C> decoder,
            final DecodeOptions options) throws DecodeException {
        Reject.ifNull(decoder, options);

        final List<Control> controls = impl.getControls();
        final Control control = AbstractRequestImpl.getControl(controls, decoder.getOID());
        if (control == null) {
            return null;
        }

        // Got a match. Return a defensive copy only if necessary.
        final C decodedControl = decoder.decodeControl(control, options);
        if (decodedControl != control) {
            // This was not the original control so return it
            // immediately.
            return decodedControl;
        } else if (decodedControl instanceof GenericControl) {
            // Generic controls are immutable, so return it immediately.
            return decodedControl;
        } else {
            // Re-decode to get defensive copy.
            final GenericControl genericControl = GenericControl.newControl(control);
            return decoder.decodeControl(genericControl, options);
        }
    }

    @Override
    public final List<Control> getControls() {
        // We need to make all controls unmodifiable as well, which implies
        // making defensive copies where necessary.
        final Function<Control, Control, NeverThrowsException> function =
                new Function<Control, Control, NeverThrowsException>() {
                    @Override
                    public Control apply(final Control value) {
                        // Return defensive copy.
                        return GenericControl.newControl(value);
                    }
                };

        final List<Control> unmodifiableControls =
                Collections2.transformedList(impl.getControls(), function, Functions
                        .<Control> identityFunction());
        return Collections.unmodifiableList(unmodifiableControls);
    }

    @Override
    public final String toString() {
        return impl.toString();
    }

}
