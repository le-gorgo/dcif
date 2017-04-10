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

import java.util.ArrayList;
import java.util.HashSet;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.Options;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Subst;
import org.nabelab.solar.Term;
import org.nabelab.solar.constraint.Constraint;
import org.nabelab.solar.constraint.Disjunction;
import org.nabelab.solar.constraint.GreaterThan;
import org.nabelab.solar.constraint.GreaterThanOrEqualTo;
import org.nabelab.solar.constraint.NotEqual;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ProofStep;

public class NegEqFlatAll extends Operator {

  /**
   * Constructs a negative equality flattening operator which is applied to the specified node.
   * @param env       the environment.
   * @param node      the specified node.
   * @param literals  the set of negative equalities.
   */
  public NegEqFlatAll(Env env, Node node, ArrayList<Literal> literals) {
    super(env, node);
    this.literals = literals;
    for (Literal lit : literals)
      numExts = lit.getNumExts();
    this.stats.incProds(Stats.NEG_EQ_FLATTENING);
  }

  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    varTable.addVars(literals.size() - 1);
    super.apply();
    
    // Checks equality constraints.
    Options opt = tableau.getOptions();
    if (opt.useEqConstraint()) {
      if (opt.getEqConstraintType() != CFP.EQ_CONSTRAINTS_ADVANCE) {
        Literal lit = node.getLiteral();

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
          varTable.removeVars(1);
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
      else {  // Checks equality constraints in advance 

        // changes the constraint >= to >.
        Literal lit = node.getLiteral();

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
          varTable.removeVars(1);
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

      for (int i=0; i < literals.size(); i++) {
        Literal lit = literals.get(i);
        GreaterThanOrEqualTo ge = new GreaterThanOrEqualTo(env, node, Stats.EQ_CONSTRAINT_CHK, lit.getArg(0), lit.getArg(1));
        if (tableau.addConstraint(ge)) {
          addGenerated(ge);
          stats.incProds(Stats.EQ_CONSTRAINT_GEN);
        }
      }
    }

    // Regularity checking.
    if (opt.use(USE_REGULARITY)) {
      boolean sameBlock = true;
      Node anode = node;
      while (!anode.isRoot()) {
        if (opt.getCalcType() == CALC_RME && sameBlock && anode.hasTag(RESTARTTED))
          sameBlock = false;        
        for (int i=0; i < literals.size(); i++) {
          
          // If RME, then use blockwise regularity:
          // - For negative literals, use normal regularity in the same block.
          if (opt.getCalcType() == CALC_RME && !sameBlock) 
            continue;

          Literal lit = literals.get(i);
          
          stats.incTests(Stats.REGULARITY_GEN);
          Subst g = lit.isUnifiable(anode.getLiteral());
          if (g == null) continue;
          
          if (g.isEmpty()) {
            if (env.dbgNow(DBG_TABLEAUX)) {
              System.out.println();
              System.out.println("FAILED by regularity.");
            }
            super.cancel();
            varTable.removeVars(1);
            stats.incSuccs(Stats.REGULARITY_GEN);
            return false;
          }
          else {
            if (g.size() == 1) {
              NotEqual neq = new NotEqual(env, opt, node, Stats.REGULARITY_CHK, g.getVar(0), g.getVal(0)); 
              if (tableau.addConstraint(neq)) {
                addGenerated(neq);
                stats.incProds(Stats.REGULARITY_GEN);
              }
            }
            else {
              HashSet<Constraint> dis = new HashSet<Constraint>();
              for (int j=0; j < g.size(); j++) {
                dis.add(new NotEqual(env, opt, node, Stats.REGULARITY_CHK, g.getVar(j), g.getVal(j)));
                stats.incProds(Stats.REGULARITY_GEN);
              }
              Disjunction disjunct = new Disjunction(env, node, Stats.REGULARITY_CHK, dis);
              if (tableau.addConstraint(disjunct)) {
                addGenerated(disjunct);
                stats.incProds(Stats.REGULARITY_GEN, disjunct.size() + 1);
              }
            }
          }
        }
        anode = anode.getParent();
      }
    }    
 
    // Complement-free checking.
    if (opt.use(USE_COMPLEMENT_FREE) && !node.getParent().isRoot()) {
      
      Node pnode = node;
      Node anode = pnode.getParent();
      Literal plit = pnode.getLiteral();
      Literal alit = anode.getLiteral();      
      
      // Use order preserving reduction for complement freeness?
      boolean useOPRC = opt.use(USE_ORDER_PRESERVING_REDUCTION);
      if (opt.getEqConstraintType() != CFP.EQ_CONSTRAINTS_NONE && plit.isEqualPred())
        useOPRC = false;

      while (!anode.isRoot()) {
        
        if (opt.getCalcType() == CALC_RME && anode.hasTag(RESTARTTED))
          break;

        // MEMO In some problems (PUZ035-1.p, SYN094-1.005.p), the order
        // preserving reduction conflicts with the complement-freeness.
        // Therefore, we restrict the application of the
        // complement-freeness according to the reduction-order.
        if (!useOPRC  || plit.getReductionOrder() < alit.getReductionOrder()) {

          stats.incTests(Stats.COMPLEMENT_FREE_GEN);
          Subst g = plit.isCompUnifiable(alit);
          if (g == null) {
            // Next loop.
          }
          else if (g.isEmpty()) {
            if (env.dbgNow(DBG_TABLEAUX)) {
              System.out.println();
              System.out.println("FAILED by complement freeness.");
            }
            super.cancel();
            varTable.removeVars(1);
            stats.incSuccs(Stats.COMPLEMENT_FREE_GEN);
            return false;
          }
          else {
            if (g.size() == 1) {
              NotEqual neq = new NotEqual(env, opt, node, Stats.COMPLEMENT_FREE_CHK, g.getVar(0), g.getVal(0)); 
              if (tableau.addConstraint(neq)) {
                addGenerated(neq);
                stats.incProds(Stats.COMPLEMENT_FREE_GEN);
              }
            }
            else {
              HashSet<Constraint> dis = new HashSet<Constraint>();
              for (int i=0; i < g.size(); i++)
                dis.add(new NotEqual(env, opt, node, Stats.COMPLEMENT_FREE_CHK, g.getVar(i), g.getVal(i)));
              Disjunction disjunct = new Disjunction(env, node, Stats.COMPLEMENT_FREE_CHK, dis);
              if (tableau.addConstraint(disjunct)) {
                addGenerated(disjunct);
                stats.incProds(Stats.COMPLEMENT_FREE_GEN, disjunct.size() + 1);
              }
            }
          }
        }
        anode = anode.getParent();
        alit  = anode.getLiteral();
      }
    }
           
    node.addNegEqFlatNodes(literals);
    node.addTag(NEG_EQ_FLATTENED);
    stats.incSuccs(Stats.NEG_EQ_FLATTENING);
    return true;
  }  
  
  /**
   * Cancels this operator.
   */
  public void cancel() {
    node.removeTag(NEG_EQ_FLATTENED);
    node.removeNegEqFlatNodes();
    super.cancel();
    varTable.removeVars(literals.size() - 1);
  }
  
  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return null; // new NegEqFlatStep(env, subNeq, orgNeq);
  }
  
  /**
   * Returns the number of extendable clauses that increases by this operation (for Extension).
   * @return the number of extendable clauses that increases by this operation.
   */
  public int getNumExts() {
    return numExts;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    return "NEF " + literals + "(" + getNumExts() + "e)";
  }

  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[NEF]";
  }

  /** The set of negative equalities. */
  private ArrayList<Literal> literals = null;
  /** The number of extendable clauses for the literals. */
  private int numExts = 0;
 
}
