/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;

import com.oracle.truffle.r.runtime.data.RAttributeStorage;

abstract class AbstractValuePrinter<T> implements ValuePrinter<T> {

    @Override
    public void print(T value, PrintContext printCtx) throws IOException {
        printCtx.output().begin(value);
        printCtx.output().beginValue(value);
        printValue(value, printCtx);
        printCtx.output().endValue(value);
        printAttributes(value, printCtx);
        printCtx.output().end(value);
    }

    private void printAttributes(T value, PrintContext printCtx) throws IOException {
        if (value instanceof RAttributeStorage) {
            printCtx.output().beginAttributes((RAttributeStorage) value);
            AttributesPrinter.INSTANCE.print((RAttributeStorage) value, printCtx);
            printCtx.output().endAttributes((RAttributeStorage) value);
        }
    }

    protected abstract void printValue(T value, PrintContext printCtx) throws IOException;

}
