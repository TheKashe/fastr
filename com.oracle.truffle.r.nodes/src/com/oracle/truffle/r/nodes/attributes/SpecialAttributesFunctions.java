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
package com.oracle.truffle.r.nodes.attributes;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import static com.oracle.truffle.r.runtime.RError.Message.LENGTH_ZERO_DIM_INVALID;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.GetDimAttributeNodeGen;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.nmath.TOMS708;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * This class defines a number of nodes used to handle the special attributes, such as names, dims,
 * dimnames and rownames.
 */
public final class SpecialAttributesFunctions {

    /**
     * A node used in guards, for example, to determine whether an attribute is a special one.
     */
    public static final class IsSpecialAttributeNode extends RBaseNode {

        private final BranchProfile namesProfile = BranchProfile.create();
        private final BranchProfile dimProfile = BranchProfile.create();
        private final BranchProfile dimNamesProfile = BranchProfile.create();
        private final BranchProfile rowNamesProfile = BranchProfile.create();
        private final BranchProfile tspProfile = BranchProfile.create();
        private final BranchProfile commentProfile = BranchProfile.create();
        private final BranchProfile classProfile = BranchProfile.create();

        public static IsSpecialAttributeNode create() {
            return new IsSpecialAttributeNode();
        }

        /**
         * The fast-path method.
         */
        public boolean execute(String name) {
            assert Utils.isInterned(name);
            if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
                namesProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
                dimProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
                dimNamesProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
                rowNamesProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
                tspProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
                commentProfile.enter();
                return true;
            } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
                classProfile.enter();
                return false;
            }
            return false;
        }

        /**
         * The slow-path method.
         */
        public static boolean isSpecialAttribute(String name) {
            assert Utils.isInterned(name);
            return Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY) ||
                            Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY);

        }
    }

    /**
     * A node for setting a value to any special attribute.
     */
    public static final class GenericSpecialAttributeNode extends RBaseNode {

        private final BranchProfile namesProfile = BranchProfile.create();
        private final BranchProfile dimProfile = BranchProfile.create();
        private final BranchProfile dimNamesProfile = BranchProfile.create();
        private final BranchProfile rowNamesProfile = BranchProfile.create();
        private final BranchProfile tspProfile = BranchProfile.create();
        private final BranchProfile commentProfile = BranchProfile.create();

        @Child private SetNamesAttributeNode namesAttrNode;
        @Child private SetDimAttributeNode dimAttrNode;
        @Child private SetDimNamesAttributeNode dimNamesAttrNode;
        @Child private SetRowNamesAttributeNode rowNamesAttrNode;
        @Child private SetTspAttributeNode tspAttrNode;
        @Child private SetCommentAttributeNode commentAttrNode;

        public static GenericSpecialAttributeNode create() {
            return new GenericSpecialAttributeNode();
        }

        public void execute(RAttributable x, String name, Object value) {
            if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
                namesProfile.enter();
                if (namesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    namesAttrNode = insert(SetNamesAttributeNode.create());
                }
                namesAttrNode.execute(x, value);
            } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
                dimProfile.enter();
                if (dimAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dimAttrNode = insert(SetDimAttributeNode.create());
                }
                dimAttrNode.execute(x, value);
            } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
                dimNamesProfile.enter();
                if (dimNamesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dimNamesAttrNode = insert(SetDimNamesAttributeNode.create());
                }
                dimNamesAttrNode.execute(x, value);
            } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
                rowNamesProfile.enter();
                if (rowNamesAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    rowNamesAttrNode = insert(SetRowNamesAttributeNode.create());
                }
                rowNamesAttrNode.execute(x, value);
            } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
                tspProfile.enter();
                if (tspAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    tspAttrNode = insert(SetTspAttributeNode.create());
                }
                tspAttrNode.execute(x, value);
            } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
                commentProfile.enter();
                if (commentAttrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    commentAttrNode = insert(SetCommentAttributeNode.create());
                }
                commentAttrNode.execute(x, value);
            } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw RInternalError.unimplemented("The \"class\" attribute should be set using a separate method");
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    /**
     * A factory method for creating a node setting the given special attribute.
     *
     * @param name the special attribute name
     * @return the node
     */
    public static SetSpecialAttributeNode createSetSpecialAttributeNode(String name) {
        if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
            return SetNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
            return SetDimAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
            return SetDimNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
            return SetRowNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
            return SetTspAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
            return SetCommentAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
            return SetClassAttributeNode.create();
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * A factory method for creating a node removing the given special attribute.
     *
     * @param name the special attribute name
     * @return the node
     */
    public static RemoveSpecialAttributeNode createRemoveSpecialAttributeNode(String name) {
        assert Utils.isInterned(name);
        if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
            return RemoveNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
            return RemoveDimAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
            return RemoveDimNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
            return RemoveRowNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
            return RemoveTspAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
            return RemoveCommentAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
            return RemoveClassAttributeNode.create();
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * A factory method for creating a node retrieving the given special attribute.
     *
     * @param name the special attribute name
     * @return the node
     */
    public static GetFixedAttributeNode createGetSpecialAttributeNode(String name) {
        assert Utils.isInterned(name);
        if (Utils.identityEquals(name, RRuntime.NAMES_ATTR_KEY)) {
            return GetNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIM_ATTR_KEY)) {
            return GetDimAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.DIMNAMES_ATTR_KEY)) {
            return GetDimNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.ROWNAMES_ATTR_KEY)) {
            return GetRowNamesAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.TSP_ATTR_KEY)) {
            return GetTspAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.COMMENT_ATTR_KEY)) {
            return GetCommentAttributeNode.create();
        } else if (Utils.identityEquals(name, RRuntime.CLASS_ATTR_KEY)) {
            return GetClassAttributeNode.create();
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * The base class for the nodes setting values to special attributes.
     */
    public abstract static class SetSpecialAttributeNode extends SetFixedAttributeNode {

        protected SetSpecialAttributeNode(String name) {
            super(name);
        }

        public abstract void execute(RAttributable x, Object attrValue);

    }

    /**
     * The base class for the nodes removing values from special attributes.
     */
    public abstract static class RemoveSpecialAttributeNode extends RemoveFixedAttributeNode {

        protected RemoveSpecialAttributeNode(String name) {
            super(name);
        }

        public abstract void execute(RAttributable x);

        @Specialization(insertBefore = "removeAttrFromAttributable")
        protected void removeAttrFromVector(RVector<?> x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("create()") BranchProfile attrEmptyProfile) {
            DynamicObject attributes = x.getAttributes();
            if (attributes == null) {
                attrNullProfile.enter();
                return;
            }

            attributes.delete(name);

            if (attributes.isEmpty()) {
                attrEmptyProfile.enter();
                x.initAttributes(null);
            }
        }
    }

    public abstract static class SetNamesAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimNamesProfile = ConditionProfile.createBinaryProfile();

        protected SetNamesAttributeNode() {
            super(RRuntime.NAMES_ATTR_KEY);
        }

        public static SetNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetNamesAttributeNodeGen.create();
        }

        public void setNames(RAbstractContainer x, RStringVector newNames) {
            if (nullDimNamesProfile.profile(newNames == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, newNames);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetDimNames(RAbstractContainer x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveNamesAttributeNode removeNamesAttrNode) {
            removeNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setNamesInVector(RAbstractVector x, RStringVector newNames,
                        @Cached("createBinaryProfile()") ConditionProfile useDimNamesProfile,
                        @Cached("create()") GetDimAttributeNode getDimNode,
                        @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RAbstractVector xProfiled = xTypeProfile.profile(x);
            if (newNames.getLength() > xProfiled.getLength()) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.ATTRIBUTE_VECTOR_SAME_LENGTH, RRuntime.NAMES_ATTR_KEY, newNames.getLength(), xProfiled.getLength());
            }

            int[] dimensions = getDimNode.getDimensions(x);
            if (useDimNamesProfile.profile(dimensions != null && dimensions.length == 1)) {
                // for one dimensional array, "names" is really "dimnames[[1]]" (see R
                // documentation for "names" function)
                RList newDimNames = RDataFactory.createList(new Object[]{newNames});
                setDimNamesNode.setDimNames(xProfiled, newDimNames);
            } else {
                assert newNames != xProfiled;
                DynamicObject attrs = xProfiled.getAttributes();
                if (attrs == null) {
                    attrNullProfile.enter();
                    attrs = RAttributesLayout.createNames(newNames);
                    xProfiled.initAttributes(attrs);
                    return;
                }

                super.setAttrInAttributable(xProfiled, newNames, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected void setNamesInContainer(RAbstractContainer x, RStringVector newNames,
                        @Cached("createClassProfile()") ValueProfile contClassProfile) {
            RAbstractContainer xProfiled = contClassProfile.profile(x);
            xProfiled.setNames(newNames);
        }
    }

    public abstract static class RemoveNamesAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveNamesAttributeNode() {
            super(RRuntime.NAMES_ATTR_KEY);
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }

        public static RemoveNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveNamesAttributeNodeGen.create();
        }
    }

    public abstract static class GetNamesAttributeNode extends GetFixedAttributeNode {

        protected GetNamesAttributeNode() {
            super(RRuntime.NAMES_ATTR_KEY);
        }

        public static GetNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetNamesAttributeNodeGen.create();
        }

        public final RStringVector getNames(Object x) {
            return (RStringVector) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorNames(@SuppressWarnings("unused") RScalarVector x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getPairListNames(RPairList x) {
            return x.getNames();
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getSequenceVectorNames(@SuppressWarnings("unused") RSequence x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorNames(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") BranchProfile namesNullProfile,
                        @Cached("create()") BranchProfile dimNamesAvlProfile,
                        @Cached("create()") GetDimNamesAttributeNode getDimNames,
                        @Cached("create()") ExtractListElement extractListElement) {
            RStringVector names = (RStringVector) super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
            if (names == null) {
                namesNullProfile.enter();
                RList dimNames = getDimNames.getDimNames(x);
                if (dimNames != null && dimNames.getLength() == 1) {
                    dimNamesAvlProfile.enter();
                    Object dimName = extractListElement.execute(dimNames, 0);
                    // RNull for ".Dimnames=list(NULL)"
                    return (dimName != RNull.instance) ? dimName : null;
                }
                return null;
            }
            return names;
        }

        @Specialization(insertBefore = "getAttrFromAttributable", guards = "!isRAbstractVector(x)")
        @TruffleBoundary
        protected Object getVectorNames(RAbstractContainer x,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return xTypeProfile.profile(x).getNames();
        }
    }

    public abstract static class ExtractNamesAttributeNode extends RBaseNode {

        @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();
        @Child private UpdateShareableChildValueNode updateRefCount = UpdateShareableChildValueNode.create();

        private final ConditionProfile nonNullValue = ConditionProfile.createBinaryProfile();

        public static ExtractNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.ExtractNamesAttributeNodeGen.create();
        }

        public abstract RStringVector execute(Object x);

        @Specialization
        protected RStringVector extractNames(Object x) {
            RStringVector names = getNames.getNames(x);
            if (nonNullValue.profile(names != null)) {
                updateRefCount.updateState(x, names);
            }
            return names;
        }

    }

    public abstract static class SetDimAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile contArgClassProfile = ValueProfile.createClassProfile();
        private final ValueProfile dimArgClassProfile = ValueProfile.createClassProfile();
        private final LoopConditionProfile verifyLoopProfile = LoopConditionProfile.createCountingProfile();
        @Child private CastNode castValue = newCastBuilder().asIntegerVector().mustBe(notEmpty(), LENGTH_ZERO_DIM_INVALID).buildCastNode();

        protected SetDimAttributeNode() {
            super(RRuntime.DIM_ATTR_KEY);
        }

        public static SetDimAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetDimAttributeNodeGen.create();
        }

        @Override
        protected Object castValue(Object value) {
            return castValue.doCast(value);
        }

        public void setDimensions(RAbstractContainer x, int[] dims) {
            if (nullDimProfile.profile(dims == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, RDataFactory.createIntVector(dims, RDataFactory.COMPLETE_VECTOR));
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetDims(RAbstractContainer x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveDimAttributeNode removeDimAttrNode,
                        @Cached("create()") SetDimNamesAttributeNode setDimNamesNode) {
            removeDimAttrNode.execute(x);
            setDimNamesNode.setDimNames(x, null);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setOneDimInVector(RVector<?> x, int dim,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RAbstractContainer xProfiled = contArgClassProfile.profile(x);

            int[] dims = new int[]{dim};
            verifyOneDimensions(xProfiled.getLength(), dim);

            RIntVector dimVec = RDataFactory.createIntVector(dims, RDataFactory.COMPLETE_VECTOR);

            DynamicObject attrs = xProfiled.getAttributes();
            if (attrs == null) {
                attrNullProfile.enter();
                attrs = RAttributesLayout.createDim(dimVec);
                xProfiled.initAttributes(attrs);
                updateRefCountNode.execute(dimVec);
                return;
            }

            super.setAttrInAttributable(x, dimVec, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setDimsInVector(RAbstractVector x, RAbstractIntVector dims,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            RAbstractContainer xProfiled = contArgClassProfile.profile(x);
            verifyDimensions(xProfiled.getLength(), dims);

            DynamicObject attrs = xProfiled.getAttributes();
            if (attrs == null) {
                attrNullProfile.enter();
                attrs = RAttributesLayout.createDim(dims);
                xProfiled.initAttributes(attrs);
                updateRefCountNode.execute(dims);
                return;
            }

            super.setAttrInAttributable(x, dims, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        protected void setDimsInContainerFallback(RAbstractContainer x, RAbstractIntVector dims,
                        @Cached("create()") SetDimAttributeNode setDimNode) {
            int[] dimsArr = dims.materialize().getDataCopy();
            setDimNode.setDimensions(x, dimsArr);
        }

        private void verifyOneDimensions(int vectorLength, int dim) {
            int length = dim;
            if (RRuntime.isNA(dim)) {
                throw error(RError.Message.DIMS_CONTAIN_NA);
            } else if (dim < 0) {
                throw error(RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
            }
            if (length != vectorLength && vectorLength > 0) {
                throw error(RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
            }
        }

        public void verifyDimensions(int vectorLength, RAbstractIntVector dims) {
            RAbstractIntVector dimsProfiled = dimArgClassProfile.profile(dims);
            int dimLen = dims.getLength();
            verifyLoopProfile.profileCounted(dimLen);
            int length = 1;
            for (int i = 0; i < dimLen; i++) {
                int dim = dimsProfiled.getDataAt(i);
                if (RRuntime.isNA(dim)) {
                    throw error(RError.Message.DIMS_CONTAIN_NA);
                } else if (dim < 0) {
                    throw error(RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
                }
                length *= dim;
            }
            if (length != vectorLength && vectorLength > 0) {
                throw error(RError.Message.DIMS_DONT_MATCH_LENGTH, length, vectorLength);
            }
        }
    }

    public abstract static class RemoveDimAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveDimAttributeNode() {
            super(RRuntime.DIM_ATTR_KEY);
        }

        public static RemoveDimAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveDimAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }
    }

    public abstract static class GetDimAttributeNode extends GetFixedAttributeNode {

        private final BranchProfile isPairListProfile = BranchProfile.create();
        private final ConditionProfile nullDimsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nonEmptyDimsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile twoDimsOrMoreProfile = ConditionProfile.createBinaryProfile();
        @Child private GetReadonlyData.Int getReadonlyData;

        protected GetDimAttributeNode() {
            super(RRuntime.DIM_ATTR_KEY);
        }

        public static GetDimAttributeNode create() {
            return GetDimAttributeNodeGen.create();
        }

        // TODO: getDimensions returns a naked array, which is in many places used to create a fresh
        // vector ignoring the reference counting. This should really return a vector and the users
        // should increment its ref-count if they want to put it into other
        // attributes/list/environment/... This way, we wouldn't need to call getReadonlyData, which
        // may copy the contents.

        public final int[] getDimensions(Object x) {
            // Let's handle the following two types directly so as to avoid wrapping and unwrapping
            // RIntVector. The getContainerDims spec would be invoked otherwise.
            if (x instanceof RPairList) {
                isPairListProfile.enter();
                return ((RPairList) x).getDimensions();
            }
            if (getReadonlyData == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getReadonlyData = insert(GetReadonlyData.Int.create());
            }
            RIntVector dims = (RIntVector) execute(x);
            return nullDimsProfile.profile(dims == null) ? null : getReadonlyData.execute(dims);
        }

        public static boolean isArray(int[] dimensions) {
            return dimensions != null && dimensions.length > 0;
        }

        public static boolean isMatrix(int[] dimensions) {
            return dimensions != null && dimensions.length == 2;
        }

        public static boolean isArray(RIntVector dimensions) {
            return dimensions != null && dimensions.getLength() > 0;
        }

        public static boolean isMatrix(RIntVector dimensions) {
            return dimensions != null && dimensions.getLength() == 2;
        }

        public final boolean isArray(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            return nullDimsProfile.profile(dims == null) ? false : dims.getLength() > 0;
        }

        public final boolean isMatrix(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            return nullDimsProfile.profile(dims == null) ? false : dims.getLength() == 2;
        }

        public final boolean isSquareMatrix(RAbstractVector vector) {
            RIntVector dims = (RIntVector) execute(vector);
            if (nullDimsProfile.profile(dims == null) || dims.getLength() != 2) {
                return false;
            }
            return dims.getDataAt(0) == dims.getDataAt(1);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorDims(@SuppressWarnings("unused") RScalarVector x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorDims(@SuppressWarnings("unused") RSequence x) {
            return null;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorDims(RAbstractVector x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getContainerDims(RAbstractContainer x,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullResultProfile) {
            int[] res = xTypeProfile.profile(x).getDimensions();
            return nullResultProfile.profile(res == null) ? null : RDataFactory.createIntVector(res, true);
        }

        public int nrows(Object x) {
            if (x instanceof RAbstractContainer) {
                RAbstractContainer xa = (RAbstractContainer) x;
                int[] dims = getDimensions(xa);
                if (nonEmptyDimsProfile.profile(dims != null && dims.length > 0)) {
                    return dims[0];
                } else {
                    return xa.getLength();
                }
            } else {
                throw error(RError.Message.OBJECT_NOT_MATRIX);
            }
        }

        public int ncols(Object x) {
            if (x instanceof RAbstractContainer) {
                RAbstractContainer xa = (RAbstractContainer) x;
                int[] dims = getDimensions(xa);
                if (nonEmptyDimsProfile.profile(dims != null && dims.length > 0)) {
                    if (twoDimsOrMoreProfile.profile(dims.length >= 2)) {
                        return dims[1];
                    } else {
                        return 1;
                    }
                } else {
                    return 1;
                }
            } else {
                throw error(RError.Message.OBJECT_NOT_MATRIX);
            }
        }
    }

    public abstract static class SetDimNamesAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullDimNamesProfile = ConditionProfile.createBinaryProfile();

        protected SetDimNamesAttributeNode() {
            super(RRuntime.DIMNAMES_ATTR_KEY);
        }

        public static SetDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetDimNamesAttributeNodeGen.create();
        }

        public void setDimNames(RAbstractContainer x, RList dimNames) {
            if (nullDimNamesProfile.profile(dimNames == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, dimNames);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetDimNames(RAbstractContainer x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveDimNamesAttributeNode removeDimNamesAttrNode) {
            removeDimNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setDimNamesInVector(RAbstractContainer x, RList newDimNames,
                        @Cached("create()") GetDimAttributeNode getDimNode,
                        @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                        @Cached("create()") BranchProfile nullDimProfile,
                        @Cached("create()") BranchProfile resizeDimsProfile,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            int[] dimensions = getDimNode.getDimensions(x);
            if (dimensions == null) {
                throw error(RError.Message.DIMNAMES_NONARRAY);
            }
            int newDimNamesLength = newDimNames.getLength();
            if (newDimNamesLength > dimensions.length) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.DIMNAMES_DONT_MATCH_DIMS, newDimNamesLength, dimensions.length);
            }

            loopProfile.profileCounted(newDimNamesLength);
            for (int i = 0; loopProfile.inject(i < newDimNamesLength); i++) {
                Object dimObject = newDimNames.getDataAt(i);

                if (dimObject instanceof RStringVector && ((RStringVector) dimObject).getLength() == 0) {
                    nullDimProfile.enter();
                    newDimNames.updateDataAt(i, RNull.instance, null);
                } else if ((dimObject instanceof String && dimensions[i] != 1) ||
                                (dimObject instanceof RStringVector && !isValidDimLength((RStringVector) dimObject, dimensions[i]))) {
                    CompilerDirectives.transferToInterpreter();
                    throw error(RError.Message.DIMNAMES_DONT_MATCH_EXTENT, i + 1);
                }
            }

            RList resDimNames = newDimNames;
            if (newDimNamesLength < dimensions.length) {
                resizeDimsProfile.enter();
                // resize the array and fill the missing entries with NULL-s
                resDimNames = (RList) resDimNames.copyResized(dimensions.length, true);
                resDimNames.setAttributes(newDimNames);
                for (int i = newDimNamesLength; i < dimensions.length; i++) {
                    resDimNames.updateDataAt(i, RNull.instance, null);
                }
            }

            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createDimNames(resDimNames));
                updateRefCountNode.execute(resDimNames);
                return;
            }

            super.setAttrInAttributable(x, resDimNames, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        private static boolean isValidDimLength(RStringVector x, int expectedDim) {
            int len = x.getLength();
            return len == 0 || len == expectedDim;
        }
    }

    public abstract static class RemoveDimNamesAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveDimNamesAttributeNode() {
            super(RRuntime.DIMNAMES_ATTR_KEY);
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }

        public static RemoveDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveDimNamesAttributeNodeGen.create();
        }
    }

    public abstract static class GetDimNamesAttributeNode extends GetFixedAttributeNode {

        protected GetDimNamesAttributeNode() {
            super(RRuntime.DIMNAMES_ATTR_KEY);
        }

        public static GetDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetDimNamesAttributeNodeGen.create();
        }

        public final RList getDimNames(Object x) {
            Object result = execute(x);
            return result == RNull.instance ? null : (RList) result;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorDimNames(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            return super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
        }
    }

    public abstract static class ExtractDimNamesAttributeNode extends RBaseNode {

        @Child private GetDimNamesAttributeNode getDimNames = GetDimNamesAttributeNode.create();
        @Child private UpdateShareableChildValueNode updateRefCount = UpdateShareableChildValueNode.create();

        private final ConditionProfile nonNullValue = ConditionProfile.createBinaryProfile();

        public static ExtractDimNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.ExtractDimNamesAttributeNodeGen.create();
        }

        public abstract RList execute(Object x);

        @Specialization
        protected RList extractDimNames(Object x) {
            RList dimNames = getDimNames.getDimNames(x);
            if (nonNullValue.profile(dimNames != null)) {
                updateRefCount.updateState(x, dimNames);
            }
            return dimNames;
        }

    }

    public abstract static class InitDimsNamesDimNamesNode extends RBaseNode {

        private final ConditionProfile doAnythingProfile = ConditionProfile.createBinaryProfile();

        @Child private GetDimAttributeNode getDimNode;
        @Child private ExtractNamesAttributeNode extractNamesNode;
        @Child private ExtractDimNamesAttributeNode extractDimNamesNode;

        protected InitDimsNamesDimNamesNode() {
        }

        public static InitDimsNamesDimNamesNode create() {
            return SpecialAttributesFunctionsFactory.InitDimsNamesDimNamesNodeGen.create();
        }

        public void initAttributes(RAbstractContainer x, int[] dimensions, RStringVector names, RList dimNames) {
            if (doAnythingProfile.profile(dimensions != null || names != null || dimNames != null)) {
                execute(x, dimensions, names, dimNames);
            }
        }

        public void initAttributes(RAbstractContainer x, RAbstractContainer source) {
            if (getDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDimNode = insert(GetDimAttributeNode.create());
            }
            if (extractNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractNamesNode = insert(ExtractNamesAttributeNode.create());
            }
            if (extractDimNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractDimNamesNode = insert(ExtractDimNamesAttributeNode.create());
            }
            this.initAttributes(x, getDimNode.getDimensions(source), extractNamesNode.execute(source), extractDimNamesNode.execute(source));
        }

        public abstract void execute(RAbstractContainer x, int[] dimensions, RStringVector names, RList dimNames);

        @Specialization
        protected void initContainerAttributes(RAbstractContainer x, int[] dimensions, RStringVector initialNames, RList initialDimNames,
                        @Cached("create()") ShareObjectNode shareObjectNode) {
            RStringVector names = initialNames;
            RList dimNames = initialDimNames;
            assert names != x;
            assert dimNames != x;
            DynamicObject attrs = x.getAttributes();
            if (dimNames != null) {
                shareObjectNode.execute(dimNames);
            }
            if (names != null) {
                assert names.getLength() == x.getLength() : "size mismatch: names.length=" + names.getLength() + " vs. length=" + x.getLength();
                if (dimensions != null && dimensions.length == 1) {
                    // one-dimensional arrays do not have names, only dimnames with one value
                    if (dimNames == null) {
                        shareObjectNode.execute(names);
                        dimNames = RDataFactory.createList(new Object[]{names});
                    }
                    names = null;
                } else {
                    shareObjectNode.execute(names);
                }
            }

            if (attrs == null) {
                if (dimensions != null) {
                    RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                    if (dimNames != null) {
                        attrs = RAttributesLayout.createDimAndDimNames(dimensionsVector, dimNames);
                        if (names != null) {
                            attrs.define(RRuntime.NAMES_ATTR_KEY, names);
                        }
                    } else {
                        if (names != null) {
                            attrs = RAttributesLayout.createNamesAndDim(names, dimensionsVector);
                        } else {
                            attrs = RAttributesLayout.createDim(dimensionsVector);
                        }
                    }
                } else {
                    if (dimNames != null) {
                        attrs = RAttributesLayout.createDimNames(dimNames);
                        if (names != null) {
                            attrs.define(RRuntime.NAMES_ATTR_KEY, names);
                        }
                    } else {
                        assert (names != null); // only called with at least one attr != null
                        attrs = RAttributesLayout.createNames(names);
                    }
                }
                x.initAttributes(attrs);
            } else { // attrs != null
                if (dimensions != null) {
                    RIntVector dimensionsVector = RDataFactory.createIntVector(dimensions, true);
                    x.setAttr(RRuntime.DIM_ATTR_KEY, dimensionsVector);
                }
                if (names != null) {
                    x.setAttr(RRuntime.NAMES_ATTR_KEY, names);
                }
                if (dimNames != null) {
                    x.setAttr(RRuntime.DIMNAMES_ATTR_KEY, dimNames);
                }
            }
        }
    }

    public abstract static class SetRowNamesAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullRowNamesProfile = ConditionProfile.createBinaryProfile();

        protected SetRowNamesAttributeNode() {
            super(RRuntime.ROWNAMES_ATTR_KEY);
        }

        public static SetRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetRowNamesAttributeNodeGen.create();
        }

        public void setRowNames(RAbstractContainer x, RAbstractVector rowNames) {
            if (nullRowNamesProfile.profile(rowNames == null)) {
                execute(x, RNull.instance);
            } else {
                execute(x, rowNames);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetRowNames(RVector<?> x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveRowNamesAttributeNode removeRowNamesAttrNode) {
            removeRowNamesAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setRowNamesInVector(RAbstractContainer x, RAbstractVector newRowNames,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createRowNames(newRowNames));
                updateRefCountNode.execute(newRowNames);
                return;
            }
            setAttrInAttributable(x, newRowNames, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }
    }

    public abstract static class RemoveRowNamesAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveRowNamesAttributeNode() {
            super(RRuntime.ROWNAMES_ATTR_KEY);
        }

        public static RemoveRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveRowNamesAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }
    }

    public abstract static class GetRowNamesAttributeNode extends GetFixedAttributeNode {

        protected GetRowNamesAttributeNode() {
            super(RRuntime.ROWNAMES_ATTR_KEY);
        }

        public static GetRowNamesAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetRowNamesAttributeNodeGen.create();
        }

        public Object getRowNames(RAbstractContainer x) {
            return execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getScalarVectorRowNames(@SuppressWarnings("unused") RScalarVector x) {
            return RNull.instance;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getSequenceRowNames(@SuppressWarnings("unused") RSequence x) {
            return RNull.instance;
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorRowNames(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullRowNamesProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            Object res = super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
            return nullRowNamesProfile.profile(res == null) ? RNull.instance : res;
        }

        /**
         * If <code>row.names</code> are in the GnuR compact format they will be converted to an int
         * sequence.
         */
        public static Object convertRowNamesToSeq(Object rowNames) {
            if (rowNames == RNull.instance) {
                return RNull.instance;
            } else {
                if (rowNames instanceof RAbstractIntVector) {
                    RAbstractIntVector vec = (RAbstractIntVector) rowNames;
                    if (vec.getLength() == 2 && RRuntime.isNA(vec.getDataAt(0))) {
                        return RDataFactory.createIntSequence(1, 1, Math.abs(vec.getDataAt(1)));
                    }
                } else if (rowNames instanceof RAbstractDoubleVector) {
                    RAbstractDoubleVector vec = (RAbstractDoubleVector) rowNames;
                    if (vec.getLength() == 2 && RRuntime.isNA(vec.getDataAt(0))) {
                        return RDataFactory.createIntSequence(1, 1, Math.abs((int) (vec.getDataAt(1))));
                    }
                }
                return rowNames;
            }
        }

        public static Object ensureRowNamesCompactFormat(Object rowNames) {
            if (rowNames == RNull.instance) {
                return RNull.instance;
            } else {
                if (rowNames instanceof RAbstractDoubleVector) {
                    RAbstractDoubleVector vec = (RAbstractDoubleVector) rowNames;
                    if (vec.getLength() == 2 && RRuntime.isNA(vec.getDataAt(0))) {
                        return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA, (int) vec.getDataAt(1)}, false);
                    }
                }
                return rowNames;
            }
        }

    }

    public abstract static class SetClassAttributeNode extends SetSpecialAttributeNode {

        protected SetClassAttributeNode() {
            super(RRuntime.CLASS_ATTR_KEY);
        }

        public static SetClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetClassAttributeNodeGen.create();
        }

        public void reset(RAttributable x) {
            execute(x, RNull.instance);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected <T> void handleVectorNullClass(RAbstractVector vector, @SuppressWarnings("unused") RNull classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                        @Cached("createBinaryProfile()") ConditionProfile initAttrProfile,
                        @Cached("create()") BranchProfile nullAttrProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            handleVector(vector, null, removeClassAttrNode, initAttrProfile, nullAttrProfile, nullClassProfile, notNullClassProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected <T> void handleVector(RAbstractVector vector, RStringVector classAttr,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassAttrNode,
                        @Cached("createBinaryProfile()") ConditionProfile initAttrProfile,
                        @Cached("create()") BranchProfile nullAttrProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile notNullClassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {

            DynamicObject attrs = vector.getAttributes();
            boolean initializeAttrs = initAttrProfile.profile(attrs == null && classAttr != null && classAttr.getLength() != 0);
            if (initializeAttrs) {
                nullAttrProfile.enter();
                attrs = RAttributesLayout.createClass(classAttr);
                vector.initAttributes(attrs);
                updateRefCountNode.execute(classAttr);
            }
            if (nullClassProfile.profile(attrs != null && (classAttr == null || classAttr.getLength() == 0))) {
                removeClassAttrNode.execute(vector);
            } else if (notNullClassProfile.profile(classAttr != null && classAttr.getLength() != 0)) {
                for (int i = 0; i < classAttr.getLength(); i++) {
                    String attr = classAttr.getDataAt(i);
                    if (RRuntime.CLASS_FACTOR.equals(attr)) {
                        if (!(vector instanceof RAbstractIntVector)) {
                            CompilerDirectives.transferToInterpreter();
                            throw error(RError.Message.ADDING_INVALID_CLASS, "factor");
                        }
                    }
                }

                if (!initializeAttrs) {
                    super.setAttrInAttributable(vector, classAttr, nullAttrProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
                }
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable", guards = "!isRAbstractVector(x)")
        protected void handleAttributable(RAttributable x, @SuppressWarnings("unused") RNull classAttr,
                        @Cached("create()") RemoveClassAttributeNode removeClassNode) {
            removeClassNode.execute(x);
        }
    }

    public abstract static class RemoveClassAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveClassAttributeNode() {
            super(RRuntime.CLASS_ATTR_KEY);
        }

        public static RemoveClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveClassAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }
    }

    public abstract static class GetClassAttributeNode extends GetFixedAttributeNode {

        protected GetClassAttributeNode() {
            super(RRuntime.CLASS_ATTR_KEY);
        }

        public static GetClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetClassAttributeNodeGen.create();
        }

        public final RStringVector getClassAttr(Object x) {
            return (RStringVector) execute(x);
        }

        public final boolean isObject(Object x) {
            return getClassAttr(x) != null;
        }
    }

    public abstract static class ExtractClassAttributeNode extends RBaseNode {

        @Child private GetClassAttributeNode getClassAttr = GetClassAttributeNode.create();
        @Child private UpdateShareableChildValueNode updateRefCount = UpdateShareableChildValueNode.create();

        private final ConditionProfile nonNullValue = ConditionProfile.createBinaryProfile();

        public static ExtractClassAttributeNode create() {
            return SpecialAttributesFunctionsFactory.ExtractClassAttributeNodeGen.create();
        }

        public abstract RStringVector execute(Object x);

        @Specialization
        protected RStringVector extractClassAttr(Object x) {
            RStringVector classAttr = getClassAttr.getClassAttr(x);
            if (nonNullValue.profile(classAttr != null)) {
                updateRefCount.updateState(x, classAttr);
            }
            return classAttr;
        }

    }

    public abstract static class SetTspAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullTspProfile = ConditionProfile.createBinaryProfile();

        protected SetTspAttributeNode() {
            super(RRuntime.TSP_ATTR_KEY);
        }

        public static SetTspAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetTspAttributeNodeGen.create();
        }

        public void setTsp(RAttributable x, RAbstractDoubleVector tsp) {
            if (nullTspProfile.profile(tsp == null)) {
                execute(x, RNull.instance);
            } else {
                if (tsp.getLength() != 3) {
                    throw error(RError.Message.TSP_NUMERIC_LENGTH3);
                }
                double start = tsp.getDataAt(0);
                double end = tsp.getDataAt(1);
                double frequency = tsp.getDataAt(2);
                if (frequency <= 0) {
                    throw error(RError.Message.INVALID_TSP);
                }
                int n = RRuntime.nrows(x);
                if (n == 0) {
                    throw error(RError.Message.CANNOT_ASSIGN_EMPTY_VECTOR, "tsp");
                }
                if (TOMS708.fabs(end - start - (n - 1) / frequency) > 1.e-5) {
                    throw error(RError.Message.INVALID_TSP);
                }
                execute(x, tsp);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetTsp(RVector<?> x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveTspAttributeNode removeTspAttrNode) {
            removeTspAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setTspInVector(RAttributable x, RAbstractDoubleVector newTsp,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createTsp(newTsp));
                updateRefCountNode.execute(newTsp);
                return;
            }
            setAttrInAttributable(x, newTsp, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }
    }

    public abstract static class RemoveTspAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveTspAttributeNode() {
            super(RRuntime.TSP_ATTR_KEY);
        }

        public static RemoveTspAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveTspAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }
    }

    public abstract static class GetTspAttributeNode extends GetFixedAttributeNode {

        protected GetTspAttributeNode() {
            super(RRuntime.TSP_ATTR_KEY);
        }

        public static GetTspAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetTspAttributeNodeGen.create();
        }

        public RAbstractDoubleVector getTsp(RAttributable x) {
            return (RAbstractDoubleVector) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getVectorTsp(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullTspProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            Object res = super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
            return nullTspProfile.profile(res == null) ? RNull.instance : res;
        }

    }

    public abstract static class SetCommentAttributeNode extends SetSpecialAttributeNode {

        private final ConditionProfile nullCommentProfile = ConditionProfile.createBinaryProfile();

        @Child private CastToVectorNode castVector;

        protected SetCommentAttributeNode() {
            super(RRuntime.COMMENT_ATTR_KEY);
        }

        public static SetCommentAttributeNode create() {
            return SpecialAttributesFunctionsFactory.SetCommentAttributeNodeGen.create();
        }

        public void setComment(RAttributable x, Object value) {
            if (nullCommentProfile.profile(value == null)) {
                execute(x, RNull.instance);
            } else {
                Object comment = null;
                if (value == RNull.instance) {
                    comment = value;
                } else if (value instanceof String) {
                    if (castVector == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        castVector = insert(CastToVectorNode.create());
                    }
                    comment = castVector.doCast(value);
                } else if (value instanceof RAbstractStringVector) {
                    RAbstractStringVector str = (RAbstractStringVector) value;
                    NACheck naCheck = NACheck.create();
                    naCheck.enable(str);
                    for (int j = str.getLength() - 1; j >= 0; j--) {
                        if (!naCheck.check(str.getDataAt(j))) {
                            comment = value;
                            break;
                        }
                    }
                }
                if (comment == null) {
                    throw error(RError.Message.SET_INVALID_ATTR, "comment");
                }
                execute(x, comment);
            }
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void resetComment(RVector<?> x, @SuppressWarnings("unused") RNull rnull,
                        @Cached("create()") RemoveCommentAttributeNode removeCommentAttrNode) {
            removeCommentAttrNode.execute(x);
        }

        @Specialization(insertBefore = "setAttrInAttributable")
        protected void setCommentInVector(RAttributable x, RAbstractVector newComment,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile,
                        @Cached("create()") ShareObjectNode updateRefCountNode) {
            if (x.getAttributes() == null) {
                attrNullProfile.enter();
                x.initAttributes(RAttributesLayout.createComment(newComment));
                updateRefCountNode.execute(newComment);
                return;
            }
            setAttrInAttributable(x, newComment, attrNullProfile, attrStorageProfile, xTypeProfile, updateRefCountNode);
        }
    }

    public abstract static class RemoveCommentAttributeNode extends RemoveSpecialAttributeNode {

        protected RemoveCommentAttributeNode() {
            super(RRuntime.COMMENT_ATTR_KEY);
        }

        public static RemoveCommentAttributeNode create() {
            return SpecialAttributesFunctionsFactory.RemoveCommentAttributeNodeGen.create();
        }

        @Override
        @Specialization
        protected void removeAttrFallback(DynamicObject attrs) {
            super.removeAttrFallback(attrs);
        }
    }

    public abstract static class GetCommentAttributeNode extends GetFixedAttributeNode {

        protected GetCommentAttributeNode() {
            super(RRuntime.COMMENT_ATTR_KEY);
        }

        public static GetCommentAttributeNode create() {
            return SpecialAttributesFunctionsFactory.GetCommentAttributeNodeGen.create();
        }

        public RAbstractStringVector getComment(RAttributable x) {
            return (RAbstractStringVector) execute(x);
        }

        @Specialization(insertBefore = "getAttrFromAttributable")
        protected Object getComment(RAbstractContainer x,
                        @Cached("create()") BranchProfile attrNullProfile,
                        @Cached("createBinaryProfile()") ConditionProfile attrStorageProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nullCommentProfile,
                        @Cached("createClassProfile()") ValueProfile xTypeProfile) {
            Object res = super.getAttrFromAttributable(x, attrNullProfile, attrStorageProfile, xTypeProfile);
            return nullCommentProfile.profile(res == null) ? RNull.instance : res;
        }

    }

}
