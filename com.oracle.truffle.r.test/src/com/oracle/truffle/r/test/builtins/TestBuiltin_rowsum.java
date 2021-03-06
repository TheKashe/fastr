/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_rowsum extends TestBase {

    @Test
    public void testrowsum1() {
        assertEval("argv <- list(structure(1:12, .Dim = 3:4), c('Y', 'X', 'Y'), c('X', 'Y'), FALSE, c('X', 'Y')); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testrowsum2() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2.83678930089809, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.807183221448213, 0, 0, 0, 0, 0, 0, 0, 2.28512598329689, 0, 2.63247834995855, 0, 2.20238351041917, 0, 0.769295244943351, 0, 0, 0, 0, 0, 2.39247868547682, 2.35872046276927, 0, 1.96863683476113, 1.77908247814048, 0, 0, 0, 1.30635392456315, 0, 1.90387659717817, 0, 0.869405556702986, 1.81335102661978, 2.03683243726846, 0, 0.594853387563489, 0, 0, 0.546043346519582, 2.43847447982989, 0, 0, 2.01202685781755, 0, 2.67064226802904, 2.9177625705488, 0.566296850563958, 0, 1.07001449051313, 1.02747153153177, 0, 1.14719850081019, 2.56380862975493, 0.760732567403466, 0.778199262684211, 0, 0.871444851625711, 2.75724254851229, 1.6366914055543, 0.606628090143204, 1.09201998403296, 0.729518678388558, 2.26406822516583, 2.41699231159873, 1.90058654651511), .Names = c('7', '8', '12', '13', '19', '22', '26', '31', '34', '36', '38', '40', '42', '43', '44', '46', '50', '51', '57', '59', '60', '61', '64', '67', '68', '72', '75', '76', '77', '78', '84', '87', '89', '94', '95', '100', '101', '102', '107', '110', '114', '115', '122', '123', '129', '130', '131', '134', '136', '137', '141', '144', '146', '150', '153', '154', '157', '160', '161', '162', '166', '167', '172', '174', '176', '178', '179', '180', '183', '184', '185', '186', '187', '197', '199', '201', '203', '204', '205', '207', '208', '210', '211', '214', '217', '219', '220', '222', '226', '228')), structure(c(310, 361, 654, 728, 61, 81, 520, 473, 107, 122, 965, 731, 153, 433, 145, 95, 765, 735, 5, 687, 345, 444, 60, 208, 821, 305, 226, 426, 705, 363, 167, 641, 740, 245, 588, 166, 559, 450, 529, 351, 201, 524, 199, 550, 551, 543, 293, 511, 511, 371, 201, 62, 356, 340, 315, 182, 364, 376, 384, 268, 266, 194, 348, 382, 296, 186, 145, 269, 350, 272, 292, 332, 285, 243, 276, 79, 240, 202, 235, 224, 239, 173, 252, 92, 192, 211, 175, 203, 105, 177), .Names = c('7', '8', '12', '13', '19', '22', '26', '31', '34', '36', '38', '40', '42', '43', '44', '46', '50', '51', '57', '59', '60', '61', '64', '67', '68', '72', '75', '76', '77', '78', '84', '87', '89', '94', '95', '100', '101', '102', '107', '110', '114', '115', '122', '123', '129', '130', '131', '134', '136', '137', '141', '144', '146', '150', '153', '154', '157', '160', '161', '162', '166', '167', '172', '174', '176', '178', '179', '180', '183', '184', '185', '186', '187', '197', '199', '201', '203', '204', '205', '207', '208', '210', '211', '214', '217', '219', '220', '222', '226', '228')), c(5, 60, 61, 62, 79, 81, 92, 95, 105, 107, 122, 145, 153, 166, 167, 173, 175, 177, 182, 186, 192, 194, 199, 201, 202, 203, 208, 211, 224, 226, 235, 239, 240, 243, 245, 252, 266, 268, 269, 272, 276, 285, 292, 293, 296, 305, 310, 315, 332, 340, 345, 348, 350, 351, 356, 361, 363, 364, 371, 376, 382, 384, 426, 433, 444, 450, 473, 511, 520, 524, 529, 543, 550, 551, 559, 588, 641, 654, 687, 705, 728, 731, 735, 740, 765, 821, 965), FALSE, c('5', '60', '61', '62', '79', '81', '92', '95', '105', '107', '122', '145', '153', '166', '167', '173', '175', '177', '182', '186', '192', '194', '199', '201', '202', '203', '208', '211', '224', '226', '235', '239', '240', '243', '245', '252', '266', '268', '269', '272', '276', '285', '292', '293', '296', '305', '310', '315', '332', '340', '345', '348', '350', '351', '356', '361', '363', '364', '371', '376', '382', '384', '426', '433', '444', '450', '473', '511', '520', '524', '529', '543', '550', '551', '559', '588', '641', '654', '687', '705', '728', '731', '735', '740', '765', '821', '965')); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testrowsum3() {
        assertEval("argv <- list(numeric(0), numeric(0), numeric(0), FALSE, character(0)); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testrowsum4() {
        assertEval("argv <- list(structure(c(0.432389384893196, 2.31273022636069, 0, 2.31273022636069, 0.432389384893196, 0), .Names = c('1', '3', '4', '5', '6', '7')), structure(c(9, 1, 1, 6, 6, 8), .Names = c('1', '3', '4', '5', '6', '7')), c(1, 6, 8, 9), FALSE, c('1', '6', '8', '9')); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testrowsum5() {
        assertEval("argv <- list(structure(c(0.102430555555556, 0.102430555555556, 0.102430555555556, 0.546875, -0.078125, 0.477430555555556, -0.0642361111111111, 0.102430555555556), .Names = c('1', '2', '3', '4', '5', '6', '7', '8')), structure(1:8, .Label = c('1', '2', '3', '4', '5', '6', '7', '8'), class = 'factor'), structure(1:8, .Label = c('1', '2', '3', '4', '5', '6', '7', '8'), class = 'factor'), FALSE, c('1', '2', '3', '4', '5', '6', '7', '8')); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testrowsum6() {
        // NA prints as NaN
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(c(0, 0.719170679378362, -0.280829320621638, 0, -0.43834135875385, 0, -0.0525040127116955, 0.783590877798991, -0.365543432545085, -0.0525040127116955, -0.31303941983339, 0, 0, NA), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14')), c(3, 3, 4, 5, 5, 6, 6, 6, 7, 1, 1, 1, 1, 2), c(1, 2, 3, 4, 5, 6, 7), FALSE, c('1', '2', '3', '4', '5', '6', '7')); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testrowsum7() {
        assertEval("argv <- list(structure(c(0.10270237599789, 2.6083215370191, 11.0196744330572, 2.6083215370191, 0.10270237599789, 0.10270237599789, 0.10270237599789, -0.441795512568419, 2.6083215370191, 0.10270237599789, -0.441795512568419), .Dim = c(11L, 1L), .Dimnames = list(c('11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21'), 'x')), structure(c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), .Names = c('11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21')), c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), FALSE, c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11')); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testrowsum8() {
        assertEval("argv <- list(structure(c(1.33333333333333, -1.33333333333333, 1, 1.33333333333333, -2, 0.666666666666667, -0.666666666666667, 0.666666666666667, -0.666666666666667), .Dim = c(9L, 1L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9'), 'x')), structure(c(1, 1, 2, 2, 2, 2, 3, 4, 5), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9')), c(1, 2, 3, 4, 5), FALSE, c('1', '2', '3', '4', '5')); .Internal(rowsum_matrix(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testRowsumArgCoverage() {
        assertEval("x <- matrix(1:10, ncol=2); g <- c(1,2,3,2,1); ug <-unique(g); .Internal(rowsum_matrix(x, g, ug, FALSE, as.character(ug)))");
        // 2nd parm function => GnuR Error: segfault
        assertEvalFastR("x <- matrix(1:10, ncol=2); g <- c(1,2,3,2,1); ug <-unique(g); .Internal(rowsum_matrix(x, print, ug, FALSE, as.character(ug)));",
                        "cat(\"Error: invalid 'g' argument\\n\")");
        // 3rd parm is function => GnuR Error: "Error: unimplemented type 'closure' in
        // 'HashTableSetup'"
        assertEval(Output.IgnoreErrorMessage, "x <- matrix(1:10, ncol=2); g <- c(1,2,3,2,1); ug <-unique(g); .Internal(rowsum_matrix(x, g, print, FALSE, as.character(ug)))");
        // 2nd parm NULL => GnuR Error: segfault\n" +
        assertEvalFastR("x <- matrix(1:10, ncol=2); g <- c(1,2,3,2,1); ug <-unique(g); .Internal(rowsum_matrix(x, NULL, ug, FALSE,as.character(ug)))",
                        "cat(\"Error: invalid 'g' argument\\n\")");
        // 3rd parm NULL => GnuR Error: Error: unimplemented type 'NULL' in 'HashTableSetup'
        assertEval(Output.IgnoreErrorMessage, "x <- matrix(1:10, ncol=2); g <- c(1,2,3,2,1); ug <-unique(g); .Internal(rowsum_matrix(x, g, NULL, FALSE,as.character(ug)))");
        assertEval(Output.IgnoreErrorMessage, "x <- matrix(1:10, ncol=2); g <- c(1,2,3,2,1); ug <-unique(g); .Internal(rowsum_matrix(x, , ug, FALSE,as.character(ug)))");
        assertEval(Output.IgnoreErrorMessage, "x <- matrix(1:10, ncol=2); g <- c(1,2,3,2,1); ug <-unique(g); .Internal(rowsum_matrix(x, g, , FALSE,as.character(ug)))");
    }
}
