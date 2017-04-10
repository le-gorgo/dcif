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

import java.awt.List;
import java.util.HashSet;
import java.util.*;
import org.nabelab.solar.CFP;
import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.LitOrder;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.Options;
import org.nabelab.solar.PClause;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Subst;
import org.nabelab.solar.Term;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.Unifiable;
import org.nabelab.solar.constraint.Constraint;
import org.nabelab.solar.constraint.Disjunction;
import org.nabelab.solar.constraint.GreaterThan;
import org.nabelab.solar.constraint.GreaterThanOrEqualTo;
import org.nabelab.solar.constraint.NotEqual;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ExtensionStep;
import org.nabelab.solar.proof.ProofStep;

/**
 * @author nabesima
 *
 */
public class Extension extends Operator implements TermTypes {

  /**
   * Constructs a extension operator which is applied to the specified node.
   * @param env     the environment.
   * @param node    the specified node.
   * @param pclause the extendable clause with the position.
   */
  public Extension(Env env, Node node, Unifiable<PClause> unifiable) {
    super(env, node, unifiable.getSubst());
    this.unifiable = unifiable;
    this.stats.incProds(Stats.EXTENSION);
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
      for (int i=pclause.getPos(); i > 0; i--)
        clause.swap(i-1, i);
      LitOrder order = tableau.getLitOrder();
      if (order.isDyn()) {
        if (order.useDynSyms()) 
          clause.getNumSyms(true);
        if (order.useDynExts()) 
          clause.getNumExts(true, tableau.getClauseDB());
        
//        // DEBUG
//        for (int i=1; i < clause.size(); i++) {
//          Literal lit = clause.get(i);
//          System.out.println(lit + ": " + lit.getNumSyms() + "syms, " + lit.getNumExts() + "exts, " + ((double)lit.getNumSyms() / lit.getNumExts()) + "s/e");
//        }
        
        clause.sort(order, 1);
      }
    }

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
            varTable.removeVars(clause.getNumVars());
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

        for (int i=1; i < clause.size(); i++) {    // Skip the extension target.
          Literal l = clause.get(i);
          if (l.isEqualPred()) {

            stats.incTests(Stats.EQ_CONSTRAINT_GEN);
            TermWeight weight1 = env.getTermWeight1();
            TermWeight weight2 = env.getTermWeight2();
            Term arg1 = l.getArg(0);
            Term arg2 = l.getArg(1);

            if (l.isPositive() || arg1.isUnifiable(arg2) == null) {

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
              else {
                GreaterThan gt = new GreaterThan(env, node, Stats.EQ_CONSTRAINT_CHK, arg1, arg2);
                if (tableau.addConstraint(gt)) {
                  addGenerated(gt);
                  stats.incProds(Stats.EQ_CONSTRAINT_GEN);
                }
              }           
            }
            else {  // ex. -eq(_,_).   We may apply the eq resolution to the node.  

              arg1.calcTermWegiht(weight1);
              arg2.calcTermWegiht(weight2);
              int ret = weight2.isGreaterThan(weight1); 
              if (ret == TermWeight.TRUE) {
                super.cancel();
                varTable.removeVars(clause.getNumVars());
                stats.incSuccs(Stats.EQ_CONSTRAINT_GEN);
                if (env.dbgNow(DBG_TABLEAUX)) {
                  System.out.println();
                  System.out.println("FAILED by EQCN [" + arg1 + " > " + arg2 + "].");
                }
                return false;
              }
              else if (ret == TermWeight.UNDECIDABLE) {
                GreaterThanOrEqualTo ge = new GreaterThanOrEqualTo(env, node, Stats.EQ_CONSTRAINT_CHK, arg1, arg2);
                if (tableau.addConstraint(ge)) {
                  addGenerated(ge);
                  stats.incProds(Stats.EQ_CONSTRAINT_GEN);
                }
              }
            }
          }
        }
      }
    }

//    // Checks equality constraints.
//    if (opt.useEqConstraint()) {
//      Literal lit = node.getLiteral();
//      if (lit.isNegEqualPred()) {
//        
//        stats.incTests(Stats.EQ_CONSTRAINT_GEN);
//        TermWeight weight1 = env.getTermWeight1();
//        TermWeight weight2 = env.getTermWeight2();
//        Term arg1 = lit.getArg(0);
//        Term arg2 = lit.getArg(1);
//        arg1.calcTermWegiht(weight1);
//        arg2.calcTermWegiht(weight2);
//        int ret = weight2.isGreaterThan(weight1); 
//        if (ret == TermWeight.TRUE || ret == TermWeight.SAME) {
//          super.cancel();
//          varTable.removeVars(clause.getNumVars());
//          stats.incSuccs(Stats.EQ_CONSTRAINT_GEN);
//          if (env.dbgNow(DBG_TABLEAUX)) {
//            System.out.println();
//            System.out.println("FAILED by EQCN [" + arg1 + " > " + arg2 + "].");
//          }
//          return false;
//        }
//        else if (opt.getEqConstraintType() == CFP.EQ_CONSTRAINTS_FULL) {
//          GreaterThan gt = new GreaterThan(env, node, Stats.EQ_CONSTRAINT_CHK, arg1, arg2);
//          if (tableau.addConstraint(gt)) {
//            addGenerated(gt);
//            stats.incProds(Stats.EQ_CONSTRAINT_GEN);
//          }
//        }
//      }
//    }

    // Constructs constraints for satisfying the tautology-freeness.
    if (!checkTautologyFreeness(clause)) {
      super.cancel();
      varTable.removeVars(clause.getNumVars());
      stats.incSuccs(Stats.TAUTOLOGY_FREE_GEN);
      if (env.dbgNow(DBG_TABLEAUX)) {
        System.out.println();
        System.out.println("FAILED by tautology free.");
      }
      return false;
    }
    
    // Constructs constraints for satisfying the unit subsumption checking.
    if (!checkUnitSubsumption(clause)) {
      super.cancel();
      varTable.removeVars(clause.getNumVars());
      stats.incSuccs(Stats.UNIT_SUBSUMPTION_GEN);
      if (env.dbgNow(DBG_TABLEAUX)) {
        System.out.println();
        System.out.println("FAILED by unit subsumption.");
      }
      return false;  
    }
    
    // check term depth
    if(env.isDepthTerm()){
    	if (!checkTermDepth(clause)){
    		super.cancel();
    		varTable.removeVars(clause.getNumVars());
    		stats.incSuccs(stats.FAIL);
    		if (env.dbgNow(DBG_TABLEAUX)) {
    			System.out.println();
    			System.out.println("FAILED by TermDepth.");
    		}
    		return false;
    	}
    }
    // Regularity checking.
    if (opt.use(USE_REGULARITY) && !clause.isUnit()) {
      boolean sameBlock = true;
      Node anode = node;
      while (!anode.isRoot()) {
        if (opt.getCalcType() == CALC_RME && sameBlock && anode.hasTag(RESTARTTED))
          sameBlock = false;        
        for (int i=1; i < clause.size(); i++) {    // Skip the extension target.
          
          // If RME, then use blockwise regularity:
          // - For positive literals, use normal regularity.
          // - For negative literals, use normal regularity in the same block.
          if (opt.getCalcType() == CALC_RME && clause.get(i).isNegative() && !sameBlock) 
            continue;

          stats.incTests(Stats.REGULARITY_GEN);
          Subst g = clause.get(i).isUnifiable(anode.getLiteral());
          if (g == null) continue;
          
          if (g.isEmpty()) {
            if (env.dbgNow(DBG_TABLEAUX)) {
              System.out.println();
              System.out.println("FAILED by regularity.");
            }
            super.cancel();
            varTable.removeVars(clause.getNumVars());
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
            varTable.removeVars(clause.getNumVars());
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
    
    node.addChildren(clause);
    node.addTag(EXTENDED);
    stats.incSuccs(Stats.EXTENSION);
    return true;
  }

  /**
   * Cancels this operator.
   */
  public void cancel() {
    node.removeChildren();
    node.removeTag(EXTENDED);
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
    return new ExtensionStep(env, clause);
  }
  
  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    return "EXT " + unifiable.toString() + "(" + getNumSyms() + "s/" + getNumExts() + "e)";
  }

  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[EXT]";
  }
  
  /** The extendable clause with the position. */
  private Unifiable<PClause> unifiable = null;
  /** The tableau clause. */
  private Clause clause = null;
  
}
