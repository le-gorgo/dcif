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
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.Options;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Term;
import org.nabelab.solar.constraint.GreaterThan;
import org.nabelab.solar.constraint.GreaterThanOrEqualTo;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ProofStep;
import org.nabelab.solar.proof.StartStep;

/**
 * @author nabesima
 *
 */
public class RootExtension extends Operator {

  /**
   * Constructs a extension operator which is applied to the root node.
   * @param env    the environment.
   * @param root   the root node.
   * @param clause the start clause.
   */
  public RootExtension(Env env, Node root, Clause clause) {
    this(env, root, clause, 0);
  }
  
  /**
   * Constructs a extension operator which is applied to the root node.
   * @param env    the environment.
   * @param root   the root node.
   * @param clause the start clause.
   * @param offset the variable offset.
   */
  public RootExtension(Env env, Node root, Clause clause, int offset) {
    super(env, root);
    this.topClause = clause;
    this.offset    = offset;
    stats.incProds(Stats.EXTENSION);
  }
 
  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    // Creates a tableau clause.
    topClause = Clause.newOffset(topClause, offset);
    // Applies the operator.
    varTable.addVars(topClause.getNumVars());
    super.apply();
    
    // Checks equality constraints in advance.
    Options opt = tableau.getOptions();
    if (opt.getEqConstraintType() == CFP.EQ_CONSTRAINTS_ADVANCE) {
      for (int i=0; i < topClause.size(); i++) {    
        Literal l = topClause.get(i);
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
              varTable.removeVars(topClause.getNumVars());
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
              varTable.removeVars(topClause.getNumVars());
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
    
    // Constructs constraints for satisfying the tautology-freeness.
    if (!checkTautologyFreeness(topClause)) {
      super.cancel();
      varTable.removeVars(topClause.getNumVars());
      stats.incSuccs(Stats.TAUTOLOGY_FREE_GEN);
      return false;
    }
    
    // Constructs constraints for satisfying the unit subsumption checking.
    if (!checkUnitSubsumption(topClause)) {
      super.cancel();
      varTable.removeVars(topClause.getNumVars());
      stats.incSuccs(Stats.UNIT_SUBSUMPTION_GEN);
      return false;  
    }
    
    node.addTopChildren(topClause);
    node.addTag(RESTARTTED);
    stats.incSuccs(Stats.EXTENSION);
    return true;
  }

  /**
   * Cancels this operator.
   * @Returns true if the cancellation succeeded.
   */
  public void cancel() {
    node.removeChildren();
    node.removeTag(RESTARTTED);
    super.cancel();
    varTable.removeVars(topClause.getNumVars());
  }

  /**
   * Returns the number of symbols in this operation.
   * @return the number of symbols in this operation.
   */
  public int getNumSyms() {
    //return topClause.getNumSyms(false);
    return 0;
  }
  
  /**
   * Returns the number of extendable clauses that increases by this operation (for Extension).
   * @return the number of extendable clauses that increases by this operation.
   */
  public int getNumExts() {
    //return topClause.getNumExts(false, null);
    return 0;
  }

  /**
   * Returns the top clause.
   * @return the top clause.
   */
  public Clause getClause() {
    return topClause;
  }
  
  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return new StartStep(env, topClause);
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    return "TOP " + topClause.toSimpString(offset) + "(" + getNumSyms() + "s/" + getNumExts() + "e)";
  }
  
  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[TOP]";
  }
  
  /** The tableau clause. */
  private Clause topClause = null;
  /** The variable offset. */
  private int offset = 0;
}
