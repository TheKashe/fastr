/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.function;

import static com.oracle.truffle.r.nodes.function.opt.EagerEvalHelper.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.opt.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;

/**
 * This {@link RNode} implementations are used as a factory-nodes for {@link RPromise}s OR direct
 * evaluation of these {@link RPromise}s, depending on the promises {@link EvalPolicy}.<br/>
 * All these classes are created during/after argument matching and get cached afterwards, so they
 * get (and need to get) called every repeated call to a function with the same arguments.
 */
public abstract class PromiseNode extends RNode {
    /**
     * The {@link RPromiseFactory} which holds all information necessary to construct a proper
     * {@link RPromise} for every case that might occur.
     */
    protected final RPromiseFactory factory;

    /**
     * @param factory {@link #factory}
     */
    protected PromiseNode(RPromiseFactory factory) {
        this.factory = factory;
    }

    /**
     * @param src The {@link SourceSection} of the argument for debugging purposes
     * @param factory {@link #factory}
     * @return Depending on {@link RPromiseFactory#getEvalPolicy()} and
     *         {@link RPromiseFactory#getType()} the proper {@link PromiseNode} implementation
     */
    @TruffleBoundary
    public static RNode create(SourceSection src, RPromiseFactory factory, boolean noOpt) {
        assert factory.getType() != PromiseType.NO_ARG;

        RNode pn = null;
        switch (factory.getEvalPolicy()) {
            case INLINED:
                if (factory.getType() == PromiseType.ARG_SUPPLIED) {
                    // TODO Correct??
                    pn = factory.getExpr() instanceof ConstantNode ? (RNode) factory.getExpr() : new InlinedSuppliedPromiseNode(factory);
                } else {
                    // TODO Correct??
                    pn = factory.getDefaultExpr() instanceof ConstantNode ? (RNode) factory.getDefaultExpr() : new InlinedPromiseNode(factory);
                }
                break;

            case PROMISED:
                // For ARG_DEFAULT, expr == defaultExpr!
                RNode expr = unfold(factory.getExpr());
                if (isOptimizableConstant(expr)) {
                    // As Constants don't care where they are evaluated, we don't need to
                    // distinguish between ARG_DEFAULT and ARG_SUPPLIED
                    pn = new OptConstantPromiseNode(factory);
                    break;
                }

                if (factory.getType() == PromiseType.ARG_SUPPLIED) {
                    if (!noOpt && isOptimizableVariable(expr)) {
                        pn = new OptVariableSuppliedPromiseNode(factory, (ReadVariableNode) expr);
                        break;
                    }

                    if (isVararg(expr)) {
                        pn = new VarargPromiseNode(factory, (VarArgNode) expr);
                        break;
                    }

// if (isOptimizableExpression(expr)) {
// System.err.println(" >>> SUP " + src.getCode());
// }
                }

                pn = new PromisedNode(factory);
                break;

            default:
                throw new AssertionError();
        }

        pn.assignSourceSection(src);
        return pn;
    }

    /**
     * This method checks whether to apply optimizations to RNodes created for single "..."
     * elements.
     *
     * @param expr
     * @return Whether the given {@link RNode} is a {@link VarArgNode}
     */
    private static boolean isVararg(RNode expr) {
        return expr instanceof VarArgNode;
    }

    /**
     * @param promise
     * @return Creates a {@link VarArgNode} for the given {@link RPromise}
     */
    public static VarArgNode createVarArg(RPromise promise) {
        VarArgNode result = new VarArgNode(promise);
        result.assignSourceSection(((RNode) promise.getRep()).getSourceSection());
        return result;
    }

    /**
     * A {@link PromiseNode} for supplied arguments.
     */
    private static final class PromisedNode extends PromiseNode {

        private final ConditionProfile isSuppliedArgProfile = ConditionProfile.createBinaryProfile();

        private PromisedNode(RPromiseFactory factory) {
            super(factory);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            MaterializedFrame execFrame = isSuppliedArgProfile.profile(factory.getType() == PromiseType.ARG_SUPPLIED) ? frame.materialize() : null;
            return factory.createPromise(execFrame);
        }
    }

    /**
     * TODO Expand!
     */
    private static final class OptVariableSuppliedPromiseNode extends OptVariablePromiseBaseNode {

        public OptVariableSuppliedPromiseNode(RPromiseFactory factory, ReadVariableNode rvn) {
            super(factory, rvn);
        }

        @Override
        protected RNode createFallback() {
            return new PromisedNode(factory);
        }

// @TruffleBoundary
        public void onSuccess(RPromise promise) {
// System.err.println("Opt SUCCESS: " + promise.getOptType());
        }

// @TruffleBoundary
        public void onFailure(RPromise promise) {
// System.err.println("Opt FAILURE: " + promise.getOptType());
            rewriteToFallback();
        }
    }

    /**
     * TODO Expand!
     */
    public static final class VarargPromiseNode extends PromiseNode {
        @Child private VarArgNode varargNode;

        public VarargPromiseNode(RPromiseFactory factory, VarArgNode varargNode) {
            super(factory);
            this.varargNode = varargNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // At this point we simply circumvent VarArgNode (by directly passing the contained
            // Promise); BUT we have to respect the RPromise! Thus we use a RPromise class which
            // knows that it contains a RPromise..
            return factory.createVarargPromise(varargNode.getPromise());
        }

        public VarArgNode getVarArgNode() {
            return varargNode;
        }
    }

    /**
     * This class is meant for supplied arguments (which have to be evaluated in the caller frame)
     * which are supposed to be evaluated {@link EvalPolicy#INLINED}: This means we can simply
     * evaluate it here, and as it's {@link EvalPolicy#INLINED}, return its value and not the
     * {@link RPromise} itself! {@link EvalPolicy#INLINED} {@link PromiseType#ARG_SUPPLIED}
     */
    private static final class InlinedSuppliedPromiseNode extends PromiseNode {
        @Child private RNode expr;
        @Child private PromiseHelperNode promiseHelper;
        @Child private PromiseCheckHelperNode promiseCheckHelper;

        private final BranchProfile isMissingProfile = BranchProfile.create();
        private final BranchProfile isVarArgProfile = BranchProfile.create();
        private final BranchProfile checkPromiseProfile = BranchProfile.create();

        public InlinedSuppliedPromiseNode(RPromiseFactory factory) {
            super(factory);
            this.expr = (RNode) factory.getExpr();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // builtin.inline: We do re-evaluation every execute inside the caller frame, as we
            // know that the evaluation of default values has no side effects (has to be assured by
            // builtin implementations)
            Object obj = expr.execute(frame);
            if (obj == RMissing.instance) {
                isMissingProfile.enter();
                if (factory.getDefaultExpr() == null) {
                    return RMissing.instance;
                }
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                RPromise promise = factory.createPromiseDefault();
                return promiseHelper.evaluate(frame, promise);
            } else if (obj instanceof RArgsValuesAndNames) {
                isVarArgProfile.enter();
                if (promiseCheckHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseCheckHelper = insert(new PromiseCheckHelperNode());
                }
                return promiseCheckHelper.checkEvaluateArgs(frame, (RArgsValuesAndNames) obj);
            } else {
                checkPromiseProfile.enter();
                if (promiseCheckHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseCheckHelper = insert(new PromiseCheckHelperNode());
                }
                return promiseCheckHelper.checkEvaluate(frame, obj);
            }
        }

        @Override
        public void deparse(State state) {
            expr.deparse(state);
        }
    }

    /**
     * This class is meant for default arguments which have to be evaluated in the callee frame -
     * usually. But as this is for {@link EvalPolicy#INLINED}, arguments are simply evaluated inside
     * the caller frame: This means we can simply evaluate it here, and as it's
     * {@link EvalPolicy#INLINED}, return its value and not the {@link RPromise} itself!
     */
    private static final class InlinedPromiseNode extends PromiseNode {
        @Child private RNode defaultExpr;

        public InlinedPromiseNode(RPromiseFactory factory) {
            super(factory);
            // defaultExpr and expr are identical here!
            this.defaultExpr = (RNode) factory.getDefaultExpr();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // builtin.inline: We do re-evaluation every execute inside the caller frame, based on
            // the assumption that the evaluation of default values should have no side effects
            return defaultExpr.execute(frame);
        }

        @Override
        public void deparse(State state) {
            defaultExpr.deparse(state);
        }
    }

    /**
     * This {@link RNode} is used to evaluate the expression given in a {@link RPromise} formerly
     * wrapped into a "..." after unrolling.<br/>
     * In a certain sense this is the class corresponding class for GNU R's PROMSXP (AST equivalent
     * of RPromise, only needed for varargs in FastR TODO Move to separate package together with
     * other varargs classes)
     *
     * FIXME This class effectively captures frame-specific state since the {@link RPromise} value
     * refers to a specific frame through the {@code execFrame} field. So subsequent calls to a
     * function containing one of these nodes will return a stale value when the promise is
     * evaluated. We need to find a better way. The current workaround is to call
     * {@link #setPromise} to update the state.
     */
    public static final class VarArgNode extends RNode {
        private RPromise promise;
        private boolean isEvaluated = false;

        private VarArgNode(RPromise promise) {
            this.promise = promise;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // ExpressionExecutorNode would be overkill, as this is only executed once, and not in
            // the correct frame anyway
            if (!isEvaluated) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                Object result = PromiseHelperNode.evaluateSlowPath(frame, promise);
                isEvaluated = promise.isEvaluated();
                return result;
            }

            // Should easily compile to constant
            return promise.getValue();
        }

        public RPromise getPromise() {
            return promise;
        }

        public void setPromise(RPromise promise) {
            isEvaluated = false;
            this.promise = promise;
        }
    }

    /**
     * @param src
     * @param evalPolicy {@link EvalPolicy}
     * @param nodes The argument {@link RNode}s that got wrapped into this "..."
     * @param names The argument's names
     * @param callSrc The {@link SourceSection} of the call this "..." belongs to
     * @return Creates either a {@link InlineVarArgsPromiseNode} or a {@link VarArgsPromiseNode},
     *         depending on the {@link EvalPolicy}
     */
    @TruffleBoundary
    public static RNode createVarArgs(SourceSection src, EvalPolicy evalPolicy, RNode[] nodes, String[] names, ClosureCache closureCache, SourceSection callSrc) {
        RNode node;
        switch (evalPolicy) {
            case INLINED:
                node = new InlineVarArgsPromiseNode(nodes, names);
                break;

            case PROMISED:
                node = new VarArgsPromiseNode(nodes, names, closureCache);
                break;

            default:
                throw new AssertionError();
        }

        node.assignSourceSection(src);
        return node;
    }

    /**
     * This class is used for wrapping arguments into "..." ({@link RArgsValuesAndNames}).
     */
    public static final class VarArgsPromiseNode extends RNode {
        @Children protected final RNode[] nodes;
        @CompilationFinal protected final String[] names;
        protected final ClosureCache closureCache;

        public VarArgsPromiseNode(RNode[] nodes, String[] names, ClosureCache closureCache) {
            this.nodes = nodes;
            this.names = names;
            this.closureCache = closureCache;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            Object[] promises = new Object[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                Closure closure = closureCache.getOrCreateClosure(nodes[i]);
                promises[i] = RDataFactory.createPromise(EvalPolicy.PROMISED, PromiseType.ARG_SUPPLIED, frame.materialize(), closure);
            }
            return new RArgsValuesAndNames(promises, names);
        }

        @Override
        public void deparse(State state) {
            // In support of match.call(expand.dots=FALSE)
            // GnuR represents this with a pairlist and deparses it as "list(a,b,..)"
            state.append("list(");
            for (int i = 0; i < nodes.length; i++) {
                String name = names[i];
                if (name != null) {
                    state.append(name);
                    state.append(" = ");
                }
                nodes[i].deparse(state);
                if (i < nodes.length - 1) {
                    state.append(", ");
                }
            }
            state.append(')');
        }

        public RNode[] getNodes() {
            return nodes;
        }

        public String[] getNames() {
            return names;
        }
    }

    /**
     * The {@link EvalPolicy#INLINED} counterpart of {@link VarArgsPromiseNode}: This gets a bit
     * more complicated, as "..." might also values from an outer "...", which might resolve to an
     * empty argument list.
     */
    public static final class InlineVarArgsPromiseNode extends RNode {
        @Children private final RNode[] varargs;
        @CompilationFinal protected final String[] names;

        @Child private PromiseCheckHelperNode promiseCheckHelper = new PromiseCheckHelperNode();
        private final ConditionProfile argsValueAndNamesProfile = ConditionProfile.createBinaryProfile();

        public InlineVarArgsPromiseNode(RNode[] nodes, String[] names) {
            this.varargs = nodes;
            this.names = names;
        }

        public RNode[] getVarArgs() {
            return varargs;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            Object[] evaluatedArgs = new Object[varargs.length];
            String[] evaluatedNames = names;
            int index = 0;
            for (int i = 0; i < varargs.length; i++) {
                Object argValue = varargs[i].execute(frame);
                if (argsValueAndNamesProfile.profile(argValue instanceof RArgsValuesAndNames)) {
                    // this can happen if ... is simply passed around (in particular when the call
                    // chain contains two functions with just the ... argument)
                    RArgsValuesAndNames argsValuesAndNames = (RArgsValuesAndNames) argValue;
                    int newLength = evaluatedArgs.length + argsValuesAndNames.length() - 1;
                    if (newLength == 0) {
                        // Corner case: "f <- function(...) g(...); g <- function(...)"
                        // In this case, "..." gets evaluated, and its only content is "...", which
                        // itself is missing. Result: Both disappear!
                        return RArgsValuesAndNames.EMPTY;
                    }
                    evaluatedArgs = Utils.resizeArray(evaluatedArgs, newLength);
                    evaluatedNames = Utils.resizeArray(evaluatedNames, newLength);
                    Object[] varargValues = argsValuesAndNames.getValues();
                    for (int j = 0; j < argsValuesAndNames.length(); j++) {
                        evaluatedArgs[index] = promiseCheckHelper.checkEvaluate(frame, varargValues[j]);
                        evaluatedNames[index] = argsValuesAndNames.getNames()[j];
                        index++;
                    }
                } else {
                    evaluatedArgs[index++] = promiseCheckHelper.checkEvaluate(frame, argValue);
                }
            }
            if (evaluatedArgs.length == 0) {
                // No need to create an extra object, already have one
                return RArgsValuesAndNames.EMPTY;
            }
            return new RArgsValuesAndNames(evaluatedArgs, evaluatedNames);
        }
    }
}
