/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.dyalog.apldev.debug.core.content;

import java.io.Serializable;


/**
 * Defines a tuple of some object, adding equals and hashCode operations
 * 
 * @author Fabio
 */
public final class Tuple<X, Y> implements Serializable {

    private static final long serialVersionUID = 1L;

    public X o1;
    public Y o2;

    public Tuple(X o1, Y o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Tuple)) {
            return false;
        }

        Tuple t2 = (Tuple) obj;
        if (o1 == t2.o1 && o2 == t2.o2) { //all the same 
            return true;
        }

        if (o1 == null && t2.o1 != null) {
            return false;
        }
        if (o2 == null && t2.o2 != null) {
            return false;
        }
        if (o1 != null && t2.o1 == null) {
            return false;
        }
        if (o2 != null && t2.o2 == null) {
            return false;
        }

        if (!o1.equals(t2.o1)) {
            return false;
        }
        if (!o2.equals(t2.o2)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (o1 != null && o2 != null) {
            return o1.hashCode() * o2.hashCode();
        }
        if (o1 != null) {
            return o1.hashCode();
        }
        if (o2 != null) {
            return o2.hashCode();
        }
        return 7;
    }

    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("Tuple [");
        buffer.appendObject(o1);
        buffer.append(" -- ");
        buffer.appendObject(o2);
        buffer.append("]");
        return buffer.toString();
    }
}
