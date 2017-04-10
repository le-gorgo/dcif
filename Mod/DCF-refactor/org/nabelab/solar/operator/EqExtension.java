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
import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.LitOrder;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.Options;
import org.nabelab.solar.PClause;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Term;
import org.nabelab.solar.Unifiable;
import org.nabelab.solar.constraint.GreaterThan;
import org.nabelab.solar.constraint.GreaterThanOrEqualTo;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ProofStep;

/**
 * @author nabesima
 *
 */
public class EqExtension extends Operator {

  /**
   * Constructs a equal extension operator which is applied to the specified node.
   * @param env        the environment.
   * @param node       the specified node.
   * @param unifiable  the unifiable clause.
   * @param arg1       the 1st argument of the extended predicate.  (must be instantiated)
   * @param arg2       the 2nd argument of the extended predicate.  (must be instantiated)
   * @param exarg      the argument of the extending predicate, which is used in the grand child node. 
   * @param gchild     the grand child equality predicate.
   */   
  public EqExtension(Env env, Node node, Unifiable<PClause> unifiable, Term arg1, Term arg2, Term exarg, Literal gchild) {
    super(env, node, unifiable.getSubst());
    this.unifiable = unifiable;
    this.arg1      = arg1;
    this.arg2      = arg2;
    this.exarg     = exarg;
    this.gchild    = gchild;
    this.stats.incProds(Stats.EQ_EXTENSION);
  }

  /**
   * Constructs a equal extension operator which is applied to the specified node.
   * @param env        the environment.
   * @param node       the specified node.
   * @param unifiable  the unifiable clause.
   * @param arg1       the 1st argument of the extended predicate.  (must be instantiated)
   * @param arg2       the 2nd argument of the extended predicate.  (must be instantiated)
   */   
  public EqExtension(Env env, Node node, Unifiable<PClause> unifiable, Term arg1, Term arg2) {
    super(env, node, unifiable.getSubst());
    this.unifiable = unifiable;
    this.arg1      = arg1;
    this.arg2      = arg2;
    this.stats.incProds(Stats.EQ_EXTENSION);
  }

  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    // Creates a tableau clause.
    PClause pclause = unifiable.getObject();
    clause = Clause.newOffset(pclause.getClause(), unifiable.getOffset());   
    
    // Applies the operator.
    varTable.addVars(clause.getNumVars());
    super.apply();
    
    // Moves the extension target literal to the front and Sorts literal ordering.
    if (!clause.isUnit()) {
      clause.swap(0, pclause.getPos());
      LitOrder order = tableau.getLitOrder();
      if (order.isDyn()) {
        if (order.useDynSyms()) 
          clause.getNumSyms(true);
        if (order.useDynExts()) 
          clause.getNumExts(true, tableau.getClauseDB());
        clause.sort(order, 1);
      }
    }

    Options opt = tableau.getOptions();
    if (opt.useEqConstraint()) {

      // Checks equality constraints of the missing child node.
      stats.incTests(Stats.EQ_CONSTRAINT_GEN);
      TermWeight weight1 = env.getTermWeight1();
      TermWeight weight2 = env.getTermWeight2();
      arg1.calcTermWegiht(weight1);
      arg2.calcTermWegiht(weight2);
      int ret = weight2.isGreaterThan(weight1); 
      if (ret == TermWeight.TRUE || ret == TermWeight.SAME) {
        super.cancel();
        varTable.removeVars(clause.getNumVars());
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
    
      // Checks equality constraints of the grand child node.
      if (gchild != null) {
        if (gchild.isNegative()) {
          stats.incTests(Stats.EQ_CONSTRAINT_GEN);
          exarg.calcTermWegiht(weight1);
          arg2.calcTermWegiht(weight2);
          ret = weight2.isGreaterThan(weight1); 
          if (ret == TermWeight.TRUE) {
            super.cancel();
            varTable.removeVars(clause.getNumVars());
            stats.incSuccs(Stats.EQ_CONSTRAINT_GEN);
            if (env.dbgNow(DBG_TABLEAUX)) {
              System.out.println();
              System.out.println("FAILED by EQCN [" + exarg + " >= " + arg2 + "].");
            }
            return false;
          }
          else if (ret == TermWeight.UNDECIDABLE && opt.getEqConstraintType() == CFP.EQ_CONSTRAINTS_FULL) {
            GreaterThanOrEqualTo geq = new GreaterThanOrEqualTo(env, node, Stats.EQ_CONSTRAINT_CHK, exarg, arg2);
            if (tableau.addConstraint(geq)) {
              addGenerated(geq);
              stats.incProds(Stats.EQ_CONSTRAINT_GEN);
            }
          }
        }
        // The grand child is positive.
        else {
          stats.incTests(Stats.EQ_CONSTRAINT_GEN);
          exarg.calcTermWegiht(weight1);
          arg2.calcTermWegiht(weight2);
          ret = weight2.isGreaterThan(weight1); 
          if (ret == TermWeight.TRUE || ret == TermWeight.SAME) {
            super.cancel();
            varTable.removeVars(clause.getNumVars());
            stats.incSuccs(Stats.EQ_CONSTRAINT_GEN);
            if (env.dbgNow(DBG_TABLEAUX)) {
              System.out.println();
              System.out.println("FAILED by EQCN [" + exarg + " > " + arg2 + "].");
            }
            return false;
          }
          else if (opt.getEqConstraintType() == CFP.EQ_CONSTRAINTS_FULL) {
            GreaterThan gt = new GreaterThan(env, node, Stats.EQ_CONSTRAINT_CHK, exarg, arg2);
            if (tableau.addConstraint(gt)) {
              addGenerated(gt);
              stats.incProds(Stats.EQ_CONSTRAINT_GEN);
            }          
          }
        }
      }
    }
    
    // Constructs constraints for satisfying the tautology-freeness.
    if (!checkTautologyFreeness(clause)) {
      super.cancel();
      varTable.removeVars(clause.getNumVars());
      stats.incSuccs(Stats.TAUTOLOGY_FREE_GEN);
      return false;
    }
    
    // Constructs constraints for satisfying the unit subsumption checking.
    if (!checkUnitSubsumption(clause)) {
      super.cancel();
      varTable.removeVars(clause.getNumVars());
      stats.incSuccs(Stats.UNIT_SUBSUMPTION_GEN);
      return false;  
    }
    
    if (gchild != null) {
      node.addEqExtendedNodes(clause, gchild);
      node.getFirstChild().setReductionTarget(node);  // Reduction target of the bottom node 
    }
    else
      node.addChildren(clause);
    node.addTag(EQ_EXTENDED);
    stats.incSuccs(Stats.EQ_EXTENSION);
    return true;
  }

  /**
   * Cancels this operator.
   */
  public void cancel() {
    if (gchild != null) {
      node.getFirstChild().clearReductionTarget();
      node.removeEqExtendedNodes();
    }
    else
      node.removeChildren();
    node.removeTag(EQ_EXTENDED);
    super.cancel();
    varTable.removeVars(clause.getNumVars());
  }

  /**
   * Returns the number of symbols in this operation.
   * @return the number of symbols in this operation.
   */
  public int getNumSyms() {
    return subst.getNumSyms();
  }
  
  /**
   * Returns the number of extendable clauses that increases by this operation (for Extension).
   * @return the number of extendable clauses that increases by this operation.
   */
  public int getNumExts() {
    return unifiable.getObject().getClause().getNumExts(false, null);
  }

  /**
   * Returns the clause.
   * @return the clause.
   */
  public Clause getClause() {
    return clause;
  }
  
  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return null; // TODO new EqExtensionStep(env, clause);
  }
  
  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    if (gchild == null)
      return "EQE " + unifiable.toString() + "(" + getNumSyms() + "s/" + getNumExts() + "e)";
    else
      return "EQE " + unifiable.toString() + "(" + getNumSyms() + "s/" + getNumExts() + "e)" + " + " + gchild;
  }

  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[NEX]";
  }  

  /** The extendable clause with the position. */
  private Unifiable<PClause> unifiable = null;
  /** The tableau clause. */
  private Clause clause = null;
  /** The 1st argument of the extended predicate. */
  private Term arg1 = null;
  /** The 2nd argument of the extended predicate. */
  private Term arg2 = null;
  /** The argument of the extending predicate, which is used in the grand child node. */
  private Term exarg = null;
  /** The grand child equality predicate. */
  private Literal gchild = null;
}
