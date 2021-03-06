/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi.interop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = NativeDoubleArray.class)
public class NativeDoubleArrayMR {

    @Resolve(message = "READ")
    public abstract static class NDAReadNode extends Node {
        protected double access(NativeDoubleArray receiver, int index) {
            return receiver.read(index);
        }

        protected double access(NativeDoubleArray receiver, long index) {
            return receiver.read((int) index);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class NDAWriteNode extends Node {
        protected double access(NativeDoubleArray receiver, int index, double value) {
            receiver.write(index, value);
            return value;
        }

        protected double access(NativeDoubleArray receiver, long index, double value) {
            receiver.write((int) index, value);
            return value;
        }

        protected double access(NativeDoubleArray receiver, int index, int value) {
            receiver.write(index, value);
            return value;
        }

        protected double access(NativeDoubleArray receiver, long index, int value) {
            receiver.write((int) index, value);
            return value;
        }

        protected double access(NativeDoubleArray receiver, int index, long value) {
            receiver.write(index, value);
            return value;
        }

        protected double access(NativeDoubleArray receiver, long index, long value) {
            receiver.write((int) index, value);
            return value;
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class NDAToNativeNode extends Node {
        protected Object access(NativeDoubleArray receiver) {
            return new DoubleNativePointer(receiver);
        }
    }

    @Resolve(message = "IS_POINTER")
    public abstract static class IsPointerNode extends Node {
        protected boolean access(@SuppressWarnings("unused") NativeDoubleArray receiver) {
            return true;
        }
    }

    @Resolve(message = "AS_POINTER")
    public abstract static class AsPointerNode extends Node {
        protected Object access(NativeDoubleArray receiver) {
            return receiver.convertToNative();
        }
    }

    @CanResolve
    public abstract static class NDACheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof NativeDoubleArray;
        }
    }

    private static final class DoubleNativePointer extends NativePointer {
        private final NativeDoubleArray nativeDoubleArray;

        private DoubleNativePointer(NativeDoubleArray object) {
            super(object);
            this.nativeDoubleArray = object;
        }

        @Override
        protected long asPointerImpl() {
            long result = nativeDoubleArray.convertToNative();
            return result;
        }
    }

}
