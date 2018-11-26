/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

// Code generated by com.oracle.truffle.r.ffi.codegen.FFITestsCodeGen class run with option '-h'
// All the generated files in testrffi can be regenerated by running 'mx testrfficodegen'
// See the corresponding C file for more details
#define NO_FASTR_REDEFINE
#include <R.h>
#include <Rdefines.h>
#include <Rinterface.h>
#include <Rinternals.h>
#include <Rinterface.h>
#include <R_ext/Parse.h>
#include <R_ext/Connections.h>
#include <Rmath.h>

SEXP api_OBJECT(SEXP x);

SEXP api_Rf_ScalarInteger(SEXP value);

SEXP api_Rf_ScalarLogical(SEXP value);

SEXP api_Rf_ScalarReal(SEXP value);

SEXP api_Rf_ScalarString(SEXP value);

SEXP api_Rf_asInteger(SEXP x);

SEXP api_Rf_asReal(SEXP x);

SEXP api_Rf_asLogical(SEXP x);

SEXP api_Rf_asChar(SEXP x);

SEXP api_Rf_coerceVector(SEXP x, SEXP mode);

SEXP api_Rf_mkCharLenCE(SEXP bytes, SEXP len, SEXP encoding);

SEXP api_Rf_cons(SEXP car, SEXP cdr);

SEXP api_Rf_defineVar(SEXP symbolArg, SEXP value, SEXP envArg);

SEXP api_R_getClassDef(SEXP clazz);

SEXP api_R_do_MAKE_CLASS(SEXP clazz);

SEXP api_R_do_new_object(SEXP classDef);

SEXP api_Rf_findVar(SEXP symbolArg, SEXP envArg);

SEXP api_Rf_findVarInFrame(SEXP envArg, SEXP symbolArg);

SEXP api_Rf_findVarInFrame3(SEXP envArg, SEXP symbolArg, SEXP doGet);

SEXP api_ATTRIB(SEXP obj);

SEXP api_Rf_getAttrib(SEXP obj, SEXP name);

SEXP api_Rf_setAttrib(SEXP obj, SEXP name, SEXP val);

SEXP api_Rf_inherits(SEXP x, SEXP clazz);

SEXP api_Rf_install(SEXP name);

SEXP api_Rf_installChar(SEXP name);

SEXP api_Rf_lengthgets(SEXP x, SEXP newSize);

SEXP api_Rf_isString(SEXP x);

SEXP api_Rf_isNull(SEXP x);

SEXP api_Rf_PairToVectorList(SEXP x);

SEXP api_Rf_error(SEXP msg);

SEXP api_Rf_warning(SEXP msg);

SEXP api_Rf_warningcall(SEXP call, SEXP msg);

SEXP api_Rf_errorcall(SEXP call, SEXP msg);

SEXP api_Rf_allocVector(SEXP mode, SEXP n);

SEXP api_Rf_allocArray(SEXP mode, SEXP dimsObj);

SEXP api_Rf_allocMatrix(SEXP mode, SEXP nrow, SEXP ncol);

SEXP api_Rf_nrows(SEXP x);

SEXP api_Rf_ncols(SEXP x);

SEXP api_LENGTH(SEXP x);

SEXP api_SET_STRING_ELT(SEXP x, SEXP i, SEXP v);

SEXP api_SETLENGTH(SEXP x, SEXP l);

SEXP api_SET_TRUELENGTH(SEXP x, SEXP l);

SEXP api_TRUELENGTH(SEXP x);

SEXP api_LEVELS(SEXP x);

SEXP api_SETLEVELS(SEXP x, SEXP gpbits);

SEXP api_SET_VECTOR_ELT(SEXP x, SEXP i, SEXP v);

SEXP api_SET_ATTRIB(SEXP target, SEXP attributes);

SEXP api_STRING_ELT(SEXP x, SEXP i);

SEXP api_VECTOR_ELT(SEXP x, SEXP i);

SEXP api_NAMED(SEXP x);

SEXP api_SET_OBJECT(SEXP x, SEXP flag);

SEXP api_SET_NAMED(SEXP x, SEXP v);

SEXP api_SET_TYPEOF(SEXP x, SEXP v);

SEXP api_TYPEOF(SEXP x);

SEXP api_Rf_duplicated(SEXP x, SEXP fromLast);

SEXP api_Rf_any_duplicated(SEXP x, SEXP fromLast);

SEXP api_Rf_any_duplicated3(SEXP x, SEXP incomparables, SEXP fromLast);

SEXP api_PRINTNAME(SEXP x);

SEXP api_TAG(SEXP e);

SEXP api_CAR(SEXP e);

SEXP api_CAAR(SEXP e);

SEXP api_CDR(SEXP e);

SEXP api_CDAR(SEXP e);

SEXP api_CADR(SEXP e);

SEXP api_CADDR(SEXP e);

SEXP api_CADDDR(SEXP e);

SEXP api_CAD4R(SEXP e);

SEXP api_CDDR(SEXP e);

SEXP api_CDDDR(SEXP e);

SEXP api_SET_TAG(SEXP x, SEXP y);

SEXP api_SETCAR(SEXP x, SEXP y);

SEXP api_SETCDR(SEXP x, SEXP y);

SEXP api_FORMALS(SEXP x);

SEXP api_BODY(SEXP x);

SEXP api_CLOENV(SEXP x);

SEXP api_SET_FORMALS(SEXP x, SEXP y);

SEXP api_SET_BODY(SEXP x, SEXP y);

SEXP api_SET_CLOENV(SEXP x, SEXP y);

SEXP api_SETCADR(SEXP x, SEXP y);

SEXP api_SETCADDR(SEXP x, SEXP y);

SEXP api_SETCADDDR(SEXP x, SEXP y);

SEXP api_SETCAD4R(SEXP x, SEXP y);

SEXP api_SYMVALUE(SEXP x);

SEXP api_SET_SYMVALUE(SEXP x, SEXP v);

SEXP api_R_BindingIsLocked(SEXP sym, SEXP env);

SEXP api_R_LockBinding(SEXP sym, SEXP env);

SEXP api_R_unLockBinding(SEXP sym, SEXP env);

SEXP api_R_FindNamespace(SEXP name);

SEXP api_Rf_eval(SEXP expr, SEXP env);

SEXP api_Rf_findFun(SEXP symbolObj, SEXP envObj);

SEXP api_Rf_GetOption1(SEXP tag);

SEXP api_Rf_gsetVar(SEXP symbol, SEXP value, SEXP rho);

SEXP api_Rf_setVar(SEXP symbol, SEXP value, SEXP rho);

SEXP api_DUPLICATE_ATTRIB(SEXP to, SEXP from);

SEXP api_R_compute_identical(SEXP x, SEXP y, SEXP flags);

SEXP api_Rf_copyListMatrix(SEXP s, SEXP t, SEXP byrow);

SEXP api_Rf_copyMatrix(SEXP s, SEXP t, SEXP byrow);

SEXP api_RDEBUG(SEXP x);

SEXP api_SET_RDEBUG(SEXP x, SEXP v);

SEXP api_RSTEP(SEXP x);

SEXP api_SET_RSTEP(SEXP x, SEXP v);

SEXP api_ENCLOS(SEXP x);

SEXP api_SET_ENCLOS(SEXP x, SEXP enc);

SEXP api_PRVALUE(SEXP x);

SEXP api_R_lsInternal3(SEXP envArg, SEXP allArg, SEXP sortedArg);

SEXP api_R_HomeDir();

SEXP api_IS_S4_OBJECT(SEXP x);

SEXP api_SET_S4_OBJECT(SEXP x);

SEXP api_UNSET_S4_OBJECT(SEXP x);

SEXP api_Rprintf(SEXP message);

SEXP api_GetRNGstate();

SEXP api_PutRNGstate();

SEXP api_unif_rand();

SEXP api_norm_rand();

SEXP api_exp_rand();

SEXP api_Rf_classgets(SEXP x, SEXP y);

SEXP api_R_ExternalPtrAddr(SEXP x);

SEXP api_R_ExternalPtrTag(SEXP x);

SEXP api_R_ExternalPtrProtected(SEXP x);

SEXP api_R_SetExternalPtrAddr(SEXP x, SEXP addr);

SEXP api_R_SetExternalPtrTag(SEXP x, SEXP tag);

SEXP api_R_SetExternalPtrProtected(SEXP x, SEXP prot);

SEXP api_PRSEEN(SEXP x);

SEXP api_PRENV(SEXP x);

SEXP api_R_PromiseExpr(SEXP x);

SEXP api_PRCODE(SEXP x);

SEXP api_R_new_custom_connection(SEXP description, SEXP mode, SEXP className, SEXP readAddr);

SEXP api_R_ReadConnection(SEXP fd, SEXP bufAddress, SEXP size);

SEXP api_R_WriteConnection(SEXP fd, SEXP bufAddress, SEXP size);

SEXP api_R_GetConnection(SEXP fd);

SEXP api_R_do_slot(SEXP o, SEXP name);

SEXP api_R_do_slot_assign(SEXP o, SEXP name, SEXP value);

SEXP api_Rf_str2type(SEXP name);

SEXP api_Rf_dunif(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_qunif(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_punif(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_runif(SEXP a, SEXP b);

SEXP api_Rf_dchisq(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_pchisq(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_qchisq(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_rchisq(SEXP a);

SEXP api_Rf_dnchisq(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pnchisq(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qnchisq(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rnchisq(SEXP a, SEXP b);

SEXP api_Rf_dnorm4(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pnorm5(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qnorm5(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rnorm(SEXP a, SEXP b);

SEXP api_Rf_dlnorm(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_plnorm(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qlnorm(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rlnorm(SEXP a, SEXP b);

SEXP api_Rf_dgamma(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pgamma(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qgamma(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rgamma(SEXP a, SEXP b);

SEXP api_Rf_log1pmx(SEXP a);

SEXP api_Rf_log1pexp(SEXP a);

SEXP api_Rf_lgamma1p(SEXP a);

SEXP api_Rf_logspace_add(SEXP a, SEXP b);

SEXP api_Rf_logspace_sub(SEXP a, SEXP b);

SEXP api_Rf_dbeta(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pbeta(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qbeta(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rbeta(SEXP a, SEXP b);

SEXP api_Rf_df(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pf(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qf(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rf(SEXP a, SEXP b);

SEXP api_Rf_dt(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_pt(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_qt(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_rt(SEXP a);

SEXP api_Rf_dbinom(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pbinom(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qbinom(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rbinom(SEXP a, SEXP b);

SEXP api_Rf_dcauchy(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pcauchy(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qcauchy(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rcauchy(SEXP a, SEXP b);

SEXP api_Rf_dexp(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_pexp(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_qexp(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_rexp(SEXP a);

SEXP api_Rf_dgeom(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_pgeom(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_qgeom(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_rgeom(SEXP a);

SEXP api_Rf_dhyper(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_phyper(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_qhyper(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_rhyper(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_dnbinom(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pnbinom(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qnbinom(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rnbinom(SEXP a, SEXP b);

SEXP api_Rf_dnbinom_mu(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pnbinom_mu(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qnbinom_mu(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rnbinom_mu(SEXP a, SEXP b);

SEXP api_Rf_dpois(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_ppois(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_qpois(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_rpois(SEXP a);

SEXP api_Rf_dweibull(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pweibull(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qweibull(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rweibull(SEXP a, SEXP b);

SEXP api_Rf_dlogis(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_plogis(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qlogis(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rlogis(SEXP a, SEXP b);

SEXP api_Rf_dnbeta(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_pnbeta(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_qnbeta(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_dnf(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_pnf(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_qnf(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_dnt(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pnt(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qnt(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_ptukey(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_qtukey(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e, SEXP f);

SEXP api_Rf_dwilcox(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_pwilcox(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_qwilcox(SEXP a, SEXP b, SEXP c, SEXP d, SEXP e);

SEXP api_Rf_rwilcox(SEXP a, SEXP b);

SEXP api_Rf_dsignrank(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_psignrank(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_qsignrank(SEXP a, SEXP b, SEXP c, SEXP d);

SEXP api_Rf_rsignrank(SEXP a);

SEXP api_Rf_gammafn(SEXP a);

SEXP api_Rf_lgammafn(SEXP a);

SEXP api_Rf_psigamma(SEXP a, SEXP b);

SEXP api_Rf_digamma(SEXP a);

SEXP api_Rf_trigamma(SEXP a);

SEXP api_Rf_tetragamma(SEXP a);

SEXP api_Rf_pentagamma(SEXP a);

SEXP api_Rf_beta(SEXP a, SEXP b);

SEXP api_Rf_lbeta(SEXP a, SEXP b);

SEXP api_Rf_choose(SEXP a, SEXP b);

SEXP api_Rf_lchoose(SEXP a, SEXP b);

SEXP api_Rf_bessel_i(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_bessel_j(SEXP a, SEXP b);

SEXP api_Rf_bessel_k(SEXP a, SEXP b, SEXP c);

SEXP api_Rf_bessel_y(SEXP a, SEXP b);

SEXP api_Rf_sign(SEXP a);

SEXP api_Rf_fprec(SEXP a, SEXP b);

SEXP api_Rf_ftrunc(SEXP a);

SEXP api_Rf_namesgets(SEXP vec, SEXP val);

SEXP api_Rf_copyMostAttrib(SEXP x, SEXP y);

SEXP api_Rf_VectorToPairList(SEXP x);

SEXP api_Rf_asCharacterFactor(SEXP x);

SEXP api_Rf_match(SEXP itables, SEXP ix, SEXP nmatch);

SEXP api_Rf_NonNullStringMatch(SEXP s, SEXP t);

SEXP api_R_has_slot(SEXP container, SEXP name);

SEXP api_Rf_PrintValue(SEXP value);

SEXP api_R_nchar(SEXP string, SEXP type, SEXP allowNA, SEXP keepNA, SEXP msgName);

SEXP api_Rf_isObject(SEXP x);

SEXP api_R_MakeActiveBinding(SEXP sym, SEXP fun, SEXP env);

