/************************************************************************
 Copyright 2003-2009, University of Yamanashi. All rights reserved. 
 By using this software the USER indicates that he or she has read,
 understood and will comply with the following:

 --- University of Yamanashi hereby grants USER non-exclusive permission
 to use, copy and/or modify this software for internal, non-commercial,
 research purposes only. Any distribution, including commercial sale or
 license, of this software, copies of the software, its associated
 documentation and/or modifications of either is strictly prohibited
 without the prior consent of University of Yamanashi. Title to
 copyright to this software and its associated documentation shall at
 all times remain with University of Yamanashi.  Appropriate copyright
 notice shall be placed on all software copies, and a complete copy of
 this notice shall be included in all copies of the associated
 documentation. No right is granted to use in advertising, publicity or
 otherwise any trademark, service mark, or the name of University of
 Yamanashi.

 --- This software and any associated documentation is provided "as is"

 UNIVERSITY OF YAMANASHI MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS
 OR IMPLIED, INCLUDING THOSE OF MERCHANTABILITY OR FITNESS FOR A
 PARTICULAR PURPOSE, OR THAT USE OF THE SOFTWARE, MODIFICATIONS, OR
 ASSOCIATED DOCUMENTATION WILL NOT INFRINGE ANY PATENTS, COPYRIGHTS,
 TRADEMARKS OR OTHER INTELLECTUAL PROPERTY RIGHTS OF A THIRD PARTY.

 University of Yamanashi shall not be liable under any circumstances for
 any direct, indirect, special, incidental, or consequential damages
 with respect to any claim by USER or any third party on account of or
 arising from the use, or inability to use, this software or its
 associated documentation, even if University of Yamanashi has been
 advised of the possibility of those damages.
************************************************************************/

package org.nabelab.solar.operator;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.Options;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Subst;
import org.nabelab.solar.Term;
import org.nabelab.solar.constraint.GreaterThan;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ProofStep;
import org.nabelab.solar.proof.ReductionStep;

/**
 * @author nabesima
 *
 */
public class Reduction extends Operator {

  /**
   * Constructs a reduction operator which is applied to the specified node.
   * @param env    the environment.
   * @param node   the specified node.
   * @param target the ancestor node that is a reduction target.
   * @param subst  the unification for the reduction. 
   */
  public Reduction(Env env, Node node, Node target, Subst subst) {
    super(env, node, subst);
    this.target = target;
    tableau.stats().incProds(Stats.REDUCTION);
  }
  
  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    super.apply();
    
    // Checks equality constraints.
    Options opt = tableau.getOptions();
    if (opt.useEqConstraint()) {
      if (opt.getEqConstraintType() != CFP.EQ_CONSTRAINTS_ADVANCE) {
        Literal lit = node.getLiteral();
        if (lit.isEqualPred()) {
          stats.incTests(Stats.EQ_CONSTRAINT_GEN);
          TermWeight weight1 = env.getTermWeight1();
          TermWeight weight2 = env.getTermWeight2();
          Term arg1 = lit.getArg(0);
          Term arg2 = lit.getArg(1);
          arg1.calcTermWegiht(weight1);
          arg2.calcTermWegiht(weight2);
          int ret = weight2.isGreaterThan(weight1); 
          if (ret == TermWeight.TRUE || ret == TermWeight.SAME) {
            super.cancel();
            stats.incSuccs(Stats.EQ_CONSTRAINT_GEN);
            if (env.dbgNow(DBG_TABLEAUX)) {
              System.out.println();
              System.out.println("FAILED by EQCN [" + arg1 + " > " + arg2 + "].");
            }
            return false;
          }
          else if (opt.getEqConstraintType() == CFP.EQ_CONSTRAINTS_FULL) {
            GreaterThan gt = new GreaterThan(env, node, Stats.EQ_CONSTRAINT_CHK, arg1, arg2);
            if (tableau.addConstraint(gt)) {
              addGenerated(gt);
              stats.incProds(Stats.EQ_CONSTRAINT_GEN);
            }
          }
        }
      }
      else {  // Checks equality constraints in advance
        
        Literal lit = node.getLiteral();
        if (lit.isNegEqualPred()) {   // changes the constraint >= to >.

          stats.incTests(Stats.EQ_CONSTRAINT_GEN);
          TermWeight weight1 = env.getTermWeight1();
          TermWeight weight2 = env.getTermWeight2();
          Term arg1 = lit.getArg(0);
          Term arg2 = lit.getArg(1);
          arg1.calcTermWegiht(weight1);
          arg2.calcTermWegiht(weight2);
          int ret = weight2.isGreaterThan(weight1);
          if (ret == TermWeight.TRUE || ret == TermWeight.SAME) {
            super.cancel();
            stats.incSuccs(Stats.EQ_CONSTRAINT_GEN);
            if (env.dbgNow(DBG_TABLEAUX)) {
              System.out.println();
              System.out.println("FAILED by EQCN [" + arg1 + " > " + arg2 + "].");
            }
            return false;
          }
          else {
            GreaterThan gt = new GreaterThan(env, node, Stats.EQ_CONSTRAINT_CHK, arg1, arg2);
            if (tableau.addConstraint(gt)) {
              addGenerated(gt);
              stats.incProds(Stats.EQ_CONSTRAINT_GEN);
            }
          }
        }
      }
    }
    
    node.setReductionTarget(target);
    node.addTag(REDUCED);
    tableau.stats().incSuccs(Stats.REDUCTION);
    return true;
  }

  /**
   * Cancels this operator.
   */
  public void cancel() {
    node.clearReductionTarget();
    node.removeTag(REDUCED);
    super.cancel();
  }

  /**
   * Returns the number of symbols in this operation.
   * @return the number of symbols in this operation.
   */
  public int getNumSyms() {
    return subst.getNumSyms();
  }

  /**
   * Returns the target node of the reduction.
   * @return the target node of the reduction.
   */
  public Node getTargetNode() {
    return target;
  }
 
  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return new ReductionStep(env, node.getDepth() - target.getDepth());
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    return "RED " + target + "/" + subst + "(" + getNumSyms() + "s)";
  }
  
  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[RED]";
  }
  
  /** The ancestor node that is a reduction target. */
  private Node target = null;
}
