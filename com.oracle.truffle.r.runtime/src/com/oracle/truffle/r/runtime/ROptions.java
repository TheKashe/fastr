/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.launcher.RStartParams;
import static com.oracle.truffle.r.runtime.context.FastROptions.AdditionalOptions;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Central location for all R options, that is for the {@code options(...)} and {@code getOption}
 * builtins.
 *
 * An unset option does not appear in the map but is represented as the value {@link RNull#instance}
 * . Setting with {@link RNull#instance} removes the option from the map and, therefore, from being
 * visible in a call to {@code options()}. N.B. An option in the {@link #CHECKED_OPTIONS_SET} set
 * can never be removed and this is handled by checking the value passed on update, where
 * {@link RNull#instance} is illegal.
 *
 */
public class ROptions {

    public static final class ContextStateImpl implements RContext.ContextState {
        /**
         * The current values for a given context.
         */
        private final HashMap<String, Object> map;
        private final REnvVars envVars;

        private ContextStateImpl(HashMap<String, Object> map, REnvVars envVars) {
            this.map = map;
            this.envVars = envVars;
            // cannot call updateDotOptions here
        }

        @TruffleBoundary
        public Set<Entry<String, Object>> getValues() {
            Set<Map.Entry<String, Object>> result = new HashSet<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    result.add(entry);
                }
            }
            return result;
        }

        @TruffleBoundary
        public Object getValue(String name) {
            Object value = map.get(name);
            if (value == null) {
                value = RNull.instance;
            }
            return value;
        }

        @TruffleBoundary
        public Object setValue(String name, Object value) throws OptionsException {
            Object coercedValue = value;
            if (CHECKED_OPTIONS_SET.contains(name)) {
                coercedValue = check(name, value);
            }
            Object previous = map.get(name);
            assert coercedValue != null;
            if (coercedValue == RNull.instance) {
                map.remove(name);
            } else {
                map.put(name, coercedValue);
            }
            updateDotOptions();
            return previous;
        }

        @TruffleBoundary
        public static ContextStateImpl newContextState(REnvVars envVars) {
            HashMap<String, Object> map = new HashMap<>();
            return new ContextStateImpl(map, envVars);
        }

        @Override
        public RContext.ContextState initialize(RContext context) {
            if (context.getKind() == ContextKind.SHARE_NOTHING) {
                applyDefaults(map, context.getStartParams(), envVars);
            } else {
                map.putAll(context.getParent().stateROptions.map);
            }
            return this;
        }

        /**
         * Creates/updates the {@code .Options} variable in {@code baseenv}.
         */
        public void updateDotOptions() {
            // TODO make incremental?
            RPairList ppl = null;
            RPairList head = null;
            for (Map.Entry<String, Object> entry : getValues()) {
                RPairList pl = RDataFactory.createPairList(entry.getValue(), RNull.instance, RDataFactory.createSymbol(Utils.intern(entry.getKey())));
                if (ppl != null) {
                    ppl.setCdr(pl);
                } else {
                    head = pl;
                }
                ppl = pl;
            }
            REnvironment.baseEnv().safePut(DOT_OPTIONS, head);
        }
    }

    @SuppressWarnings("serial")
    public static final class OptionsException extends RError.RErrorException {
        private OptionsException(RError.Message msg, Object... args) {
            super(null, msg, args);
        }

        private static OptionsException createInvalid(String name) {
            return new OptionsException(RError.Message.INVALID_VALUE_FOR, name);
        }
    }

    /**
     * S compatibility - pair list of the options.
     */
    private static final String DOT_OPTIONS = ".Options";

    private static final Set<String> CHECKED_OPTIONS_SET = new HashSet<>(Arrays.asList("width", "deparse.cutoff", "digits", "expressions", "keep.source", "editor", "continue", "prompt", "contrasts",
                    "check.bounds", "warn", "warning.length", "warning.expression", "max.print", "nwarnings", "error", "show.error.messages", "echo", "OutDec", "max.contour.segments",
                    "rl_word_breaks", "warnPartialMatchDollar", "warnPartialMatchArgs", "warnPartialMatchAttr", "showWarnCalls", "showErrorCalls", "showNCalls", "par.ask.default",
                    "browserNLdisabled", "CBoundsCheck"));

    private static void applyDefaults(HashMap<String, Object> map, RStartParams startParams, REnvVars envVars) {
        map.put("add.smooth", RDataFactory.createSharedLogicalVectorFromScalar(true));
        map.put("check.bounds", RDataFactory.createSharedLogicalVectorFromScalar(false));
        map.put("continue", RDataFactory.createSharedStringVectorFromScalar("+ "));
        map.put("deparse.cutoff", RDataFactory.createSharedIntVectorFromScalar(60));
        map.put("digits", RDataFactory.createSharedIntVectorFromScalar(7));
        map.put("echo", RDataFactory.createSharedLogicalVectorFromScalar(!startParams.isSlave()));
        map.put("encoding", RDataFactory.createSharedStringVectorFromScalar("native.enc"));
        map.put("expressions", RDataFactory.createSharedIntVectorFromScalar(5000));
        boolean keepPkgSource = optionFromEnvVar("R_KEEP_PKG_SOURCE", envVars);
        map.put("keep.source", RDataFactory.createSharedLogicalVectorFromScalar(keepPkgSource));
        map.put("keep.source.pkgs", RDataFactory.createSharedLogicalVectorFromScalar(keepPkgSource));
        map.put("OutDec", RDataFactory.createSharedStringVectorFromScalar("."));
        map.put("prompt", RDataFactory.createSharedStringVectorFromScalar("> "));
        map.put("verbose", RDataFactory.createSharedLogicalVectorFromScalar(false));
        map.put("nwarnings", RDataFactory.createSharedIntVectorFromScalar(50));
        map.put("warning.length", RDataFactory.createSharedIntVectorFromScalar(1000));
        map.put("width", RDataFactory.createSharedIntVectorFromScalar(80));
        map.put("browserNLdisabled", RDataFactory.createSharedLogicalVectorFromScalar(false));
        boolean cBoundsCheck = optionFromEnvVar("R_C_BOUNDS_CHECK", envVars);
        map.put("CBoundsCheck", RDataFactory.createSharedLogicalVectorFromScalar(cBoundsCheck));

        String cranMirror = REnvVars.getCRANMirror();
        if (cranMirror != null) {
            map.put("repos", cranMirror);
        }

        if (FastRConfig.DefaultDownloadMethod != null && !FastRConfig.DefaultDownloadMethod.isEmpty()) {
            map.put("download.file.method", FastRConfig.DefaultDownloadMethod);
        }

        String additional = RContext.getInstance().getOption(AdditionalOptions);
        if (additional == null || additional.isEmpty()) {
            return;
        }

        String[] namesValues = additional.trim().split(";");
        for (int i = 0; i < namesValues.length; i++) {
            String[] items = namesValues[i].trim().split(":");
            if (items.length != 2) {
                RError.warning(RError.NO_CALLER, Message.GENERIC, "Invalid value of AdditionalOptions option: " + namesValues[i]);
                continue;
            }
            Object value = items[1].trim();
            if (items[1].equals("T")) {
                value = RRuntime.LOGICAL_TRUE;
            } else if (items[1].equals("F")) {
                value = RRuntime.LOGICAL_FALSE;
            }
            map.put(items[0], value);
        }
    }

    private static boolean optionFromEnvVar(String envVar, REnvVars envVars) {
        return "yes".equals(envVars.get(envVar));
    }

    private static Object check(String name, Object value) throws OptionsException {
        Object coercedValue = value;
        switch (name) {
            case "width": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 10 || intValue > 10000) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = RDataFactory.createIntVectorFromScalar(intValue);
                }
                break;
            }

            case "deparse.cutoff": {
                coercedValue = RRuntime.asInteger(value);
                break;
            }

            case "digits": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 0 || intValue > 22) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = intValue;
                }
                break;
            }

            case "expressions": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 25 || intValue > 50000) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = intValue;
                }
                break;
            }

            case "browserNLdisabled":
            case "CBoundsCheck":
            case "warnPartialMatchDollar":
            case "warnPartialMatchArgs":
            case "warnPartialMatchAttr":
            case "showWarnCalls":
            case "showErrorCalls":
            case "show.error.messages":
            case "check.bounds":
            case "keep.source": {
                Object valueAbs = RRuntime.asAbstractVector(value);
                if (!(valueAbs instanceof RLogicalVector && ((RLogicalVector) valueAbs).getLength() == 1)) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = valueAbs;
                }
                break;
            }

            case "editor":
                if (value instanceof RFunction || value instanceof RNull) {
                    break;
                }
                coercedValue = coerceStringVector(value, name);
                break;

            case "continue":
            case "prompt": {
                coercedValue = coerceStringVector(value, name);
                break;
            }

            case "contrasts": {
                Object valueAbs = RRuntime.asAbstractVector(value);
                if (!(valueAbs instanceof RStringVector && ((RStringVector) valueAbs).getLength() == 2)) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = valueAbs;
                }
                break;
            }

            case "warn": {
                Object valueAbs = RRuntime.asAbstractVector(value);
                if (valueAbs instanceof RIntVector && ((RIntVector) valueAbs).getLength() == 1) {
                    coercedValue = valueAbs;
                } else if (valueAbs instanceof RDoubleVector && ((RDoubleVector) valueAbs).getLength() == 1) {
                    coercedValue = RDataFactory.createIntVectorFromScalar((int) ((RDoubleVector) valueAbs).getDataAt(0));
                } else {
                    throw OptionsException.createInvalid(name);
                }
                break;
            }

            case "warning.length": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 100 || intValue > 8170) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = intValue;
                }
                break;
            }

            case "warning.expression": {
                if (!((value instanceof RPairList && ((RPairList) value).isLanguage()) || value instanceof RExpression)) {
                    throw OptionsException.createInvalid(name);
                }
                break;
            }

            case "max.print": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 1) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = intValue;
                }
                break;
            }

            case "nwarnings": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 1) {
                    throw OptionsException.createInvalid(name);
                } else {
                    // TODO affect RErrorHandling?
                    coercedValue = intValue;
                }
                break;
            }

            case "error": {
                if (!(value == RNull.instance || value instanceof RFunction || (value instanceof RPairList && ((RPairList) value).isLanguage()) || value instanceof RExpression)) {
                    throw OptionsException.createInvalid(name);
                }
                break;
            }

            case "echo": {
                Object echoValue = RRuntime.asAbstractVector(value);
                if (!(echoValue instanceof RLogicalVector && ((RLogicalVector) echoValue).getLength() == 1)) {
                    throw OptionsException.createInvalid(name);
                }
                break;
            }

            case "OutDec": {
                Object valueAbs = RRuntime.asAbstractVector(value);
                if (!(valueAbs instanceof RStringVector && ((RStringVector) valueAbs).getLength() == 1 && ((RStringVector) valueAbs).getDataAt(0).length() == 1)) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = valueAbs;
                }
                break;
            }

            case "max.contour.segments": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 0) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = intValue;
                }
                break;
            }

            case "rl_word_breaks": {
                Object valueAbs = RRuntime.asAbstractVector(value);
                if (!(valueAbs instanceof RStringVector && ((RStringVector) valueAbs).getLength() == 1)) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = valueAbs;
                }
                break;
            }

            case "showNCalls": {
                int intValue = RRuntime.asInteger(value);
                if (intValue < 30 || intValue > 500) {
                    throw OptionsException.createInvalid(name);
                } else {
                    coercedValue = intValue;
                }
                break;
            }

            case "par.ask.default": {
                throw new OptionsException(RError.Message.GENERIC, "\"par.ask.default\" has been replaced by \"device.ask.default\"");
            }

            default:
                throw RInternalError.shouldNotReachHere();
        }
        return coercedValue;
    }

    private static Object coerceStringVector(Object value, String name) throws OptionsException {
        Object valueAbs = RRuntime.convertScalarVectors(value);
        // TODO supposed to be coerced
        if (valueAbs instanceof RAbstractStringVector) {
            String p = ((RAbstractStringVector) valueAbs).getDataAt(0);
            if (p.length() == 0 || RRuntime.isNA(p)) {
                throw OptionsException.createInvalid(name);
            } else {
                return valueAbs;
            }
        } else {
            throw OptionsException.createInvalid(name);
        }
    }
}
