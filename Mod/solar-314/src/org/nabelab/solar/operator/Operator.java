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
import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.DebugTypes;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.OptionTypes;
import org.nabelab.solar.Options;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Subst;
import org.nabelab.solar.Tableau;
import org.nabelab.solar.Tags;
import org.nabelab.solar.Term;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.VarTable;
import org.nabelab.solar.constraint.Conjunction;
import org.nabelab.solar.constraint.Constraint;
import org.nabelab.solar.constraint.Disjunction;
import org.nabelab.solar.constraint.Equal;
import org.nabelab.solar.constraint.NotEqual;
import org.nabelab.solar.constraint.NotSubsuming;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ProofStep;
import org.nabelab.solar.util.Pair;

/**
 * @author nabesima
 *
 */
public abstract class Operator implements TermTypes, Tags, OptionTypes, DebugTypes {

  /**
   * Constructs a operator which is applied to the specified node.
   * @param env   the environment.
   * @param node  the specified node.
   * @param subst the substitution to be applied.
   */
  public Operator(Env env, Node node, Subst subst) {
    this.env      = env;
    this.node     = node;
    this.subst    = subst;
    this.tableau  = node.getTableau();
    this.stats    = tableau.stats();
    this.varTable = env.getVarTable();
    this.state    = varTable.state();
  }

  /**
   * Constructs a operator which is applied to the specified node.
   * @param env   the environment.
   * @param node  the specified node.
   * @param subst the substitution to be applied.
   */
  public Operator(Env env, Node node) {
    this(env, node, new Subst());
  }

  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    if (subst != null) {
      state = varTable.state();
      varTable.substitute(subst);
      
    }
 
    node.setInfStep(stats.inf());
    return true;
  }

  /**
   * Cancels this operator.
   */
  public void cancel() {
    node.clearInfStep();
    if (foldingUps != null) {
      for (Pair<Node,Node> pair : foldingUps) {
        Node subgoal = pair.get1st();
        Node deepest = pair.get2nd();
        //deepest.removeFoldingUp(subgoal);
        boolean ret = deepest.removeFoldingUp(subgoal);
        assert(ret == true);
      }
    }
    if (subst != null)
      varTable.backtrackTo(state);
    if (satisfied != null)
      tableau.addConstraints(satisfied);
    if (generated != null)
      tableau.removeConstraints(generated);
  }

  /**
   * Returns the node to which this operation is applied.
   * @return the node to which this operation is applied.
   */
  public Node getNode() {
    return node;
  }

  /**
   * Returns the substitution of this operator.
   * @return the substitution of this operator.
   */
  public Subst getSubst() {
    return subst;
  }

  /**
   * Returns the true if this is a mandatory operator.
   * @return the true if this is a mandatory operator.
   */
  public boolean isMandatory() {
    return mandatory;
  }

  /**
   * Adds the satisfied constraints to this node.
   * @param cs the set of satisfied constraints to add.
   */
  public void addSatisfied(List<Constraint> cs) {
    if (satisfied == null)
      satisfied = new ArrayList<Constraint>();
    satisfied.addAll(cs);
  }

  /**
   * Adds the specified constraint to this node.
   * @param c the constraint to add.
   */
  public void addGenerated(Constraint c) {
    if (generated == null)
      generated = new ArrayList<Constraint>();
    generated.add(c);
  }

  /**
   * Adds a folding-up lemma generated by the application of this operator.
   * @param subgoal the source node of a folding-up lemma.
   * @param deepest the attached node of a folding-up lemma.
   */
  public void addFoldingUp(Node subgoal, Node deepest) {
    if (foldingUps == null)
      foldingUps = new ArrayList<Pair<Node,Node>>();
    foldingUps.add(new Pair<Node,Node>(subgoal, deepest));
  }

  /**
   * Constructs constraints for satisfying the tautology-freeness.
   * @param clause the clause to be checked.
   * @return true if the tableau is not redundant.
   */
  public boolean checkTautologyFreeness(Clause clause) {
    Options opt = tableau.getOptions();
    if (!opt.use(USE_TAUTOLOGY_FREE))
      return true;

    List<Pair<Literal,Literal>> compUnifPairs = clause.getCompUnifiableLiterals();
    if (compUnifPairs != null) {
      for (Pair<Literal,Literal> pair : compUnifPairs) {
        Subst g = pair.get1st().isCompUnifiable(pair.get2nd());
        stats.incTests(Stats.TAUTOLOGY_FREE_GEN);
        if (g == null)
          continue;
        if (g.isEmpty()) {
          if (env.dbgNow(DBG_TABLEAUX)) {
            System.out.println();
            System.out.println("FAILED by TAUT [" + pair.get1st() + " != " + pair.get2nd() + "].");
          }
          return false;
        } else if (g.size() == 1) {
          NotEqual neq = new NotEqual(env, opt, node,
              Stats.TAUTOLOGY_FREE_CHK, g.getVar(0), g.getVal(0));
          if (tableau.addConstraint(neq)) {
            addGenerated(neq);
            stats.incProds(Stats.TAUTOLOGY_FREE_GEN);
          }
        } else {
          HashSet<Constraint> dis = new HashSet<Constraint>();
          for (int j = 0; j < g.size(); j++)
            dis.add(new NotEqual(env, opt, node,
                Stats.TAUTOLOGY_FREE_CHK, g.getVar(j), g.getVal(j)));
          Disjunction disjunct = new Disjunction(env, node,
              Stats.TAUTOLOGY_FREE_CHK, dis);
          if (tableau.addConstraint(disjunct)) {
            addGenerated(disjunct);
            stats.incProds(Stats.TAUTOLOGY_FREE_GEN, disjunct.size() + 1);
          }
        }
      }
    }

    /*
    if (CFP.EQ_SNMT <= opt.getEqType() && opt.getEqType() <= CFP.EQ_MSNT2 &&
        !node.isRoot() && node.getLiteral().isConnPred()) {
      // When a positive symmetry connector predicate.
      //   L1, L2, +&(...)
      //           -&(...), -eq1, -eq2, +eq3
      if (node.getLiteral().isPositive()) {
        // Finds the positive equality literal in this clause.
        Literal pos = null;
        for (int i=1; i < clause.size(); i++) {
          if (clause.get(i).isPosEqualPred()) {
            pos = clause.get(i);
            break;
          }
        }
        assert(pos != null);

        Node uncle = node.getParent().getFirstChild();
        while (uncle != null) {
          if (uncle.getLiteral().isNegEqualPred()) {
            Literal neg = uncle.getLiteral();
            Subst g = pos.isCompUnifiable(neg);
            stats.incTests(Stats.TAUTOLOGY_FREE_GEN);
            if (g == null) {
              uncle = uncle.getRight();
              continue;
            }
            if (g.isEmpty()) {
              if (env.dbgNow(DBG_TABLEAUX)) {
                System.out.println();
                System.out.println("FAILED by TAUT [" + pos + " != " + neg + "].");
              }
              return false;
            } else if (g.size() == 1) {
              NotEqual neq = new NotEqual(env, opt, node,
                  Stats.TAUTOLOGY_FREE_CHK, g.getVar(0), g.getVal(0));
              if (tableau.addConstraint(neq)) {
                addGenerated(neq);
                stats.incProds(Stats.TAUTOLOGY_FREE_GEN);
              }
            } else {
              HashSet<Constraint> dis = new HashSet<Constraint>();
              for (int j = 0; j < g.size(); j++)
                dis.add(new NotEqual(env, opt, node,
                    Stats.TAUTOLOGY_FREE_CHK, g.getVar(j), g.getVal(j)));
              Disjunction disjunct = new Disjunction(env, node,
                  Stats.TAUTOLOGY_FREE_CHK, dis);
              if (tableau.addConstraint(disjunct)) {
                addGenerated(disjunct);
                stats.incProds(Stats.TAUTOLOGY_FREE_GEN, disjunct.size() + 1);
              }
            }
          }
          uncle = uncle.getRight();
        }
      }
      // -eq1, -eq2, +eq3, -&(...)
      //                   +&(...), L1, L2
      else {
        // Finds the positive equality literal in the parent clause.
        Literal pos = null;
        if (node.getParent().getExtChild().isPosEqualPred()) {
          pos = node.getParent().getExtChild();
        }
        else {
          Node uncle = node.getParent().getFirstChild();
          while (uncle != null) {
            if (uncle.getLiteral().isPosEqualPred()) {
              pos = uncle.getLiteral();
              break;
            }
            uncle = uncle.getRight();
          }
        }
        assert(pos != null);

        for (int i=0; i < clause.size(); i++) {
          if (clause.get(i).isNegEqualPred()) {
            Literal neg = clause.get(i);
            Subst g = pos.isCompUnifiable(neg);
            stats.incTests(Stats.TAUTOLOGY_FREE_GEN);
            if (g == null)
              continue;
            if (g.isEmpty()) {
              if (env.dbgNow(DBG_TABLEAUX)) {
                System.out.println();
                System.out.println("FAILED by TAUT [" + pos + " != " + neg + "].");
              }
              return false;
            } else if (g.size() == 1) {
              NotEqual neq = new NotEqual(env, opt, node,
                  Stats.TAUTOLOGY_FREE_CHK, g.getVar(0), g.getVal(0));
              if (tableau.addConstraint(neq)) {
                addGenerated(neq);
                stats.incProds(Stats.TAUTOLOGY_FREE_GEN);
              }
            } else {
              HashSet<Constraint> dis = new HashSet<Constraint>();
              for (int j = 0; j < g.size(); j++)
                dis.add(new NotEqual(env, opt, node,
                    Stats.TAUTOLOGY_FREE_CHK, g.getVar(j), g.getVal(j)));
              Disjunction disjunct = new Disjunction(env, node,
                  Stats.TAUTOLOGY_FREE_CHK, dis);
              if (tableau.addConstraint(disjunct)) {
                addGenerated(disjunct);
                stats.incProds(Stats.TAUTOLOGY_FREE_GEN, disjunct.size() + 1);
              }
            }
          }
        }
      }
    }
    */
    return true;
  }

  /**
   * Constructs constraints for satisfying the unit subsumption checking.
   * @param clause  the clause to be checked.
   * @return true if the tableau is not redundant.
   */
  public boolean checkUnitSubsumption(Clause clause) {
    Options opt = tableau.getOptions();
    if (!opt.use(USE_UNIT_SUBSUMPTION))
      return true;

    if (opt.use(USE_UNIQUE_NAME_AXIOMS)) {
    	for (Literal lit : clause) {
  			// if lit is -equal(s, t) and s is NOT unifiable with t, then the literal is subsumed by the unique name axiom.
    		if (lit.isNegEqualPred()) {
    			Term arg1 = lit.getArg(0);
    			Term arg2 = lit.getArg(1);
    			Subst g = arg1.isUnifiable(arg2);
    			if (g == null)
    				return false;
    			if (g.size() == 1) {
    				Equal eq = new Equal(env, opt, node, Stats.UNIT_SUBSUMPTION_CHK, g.getVar(0), g.getVal(0));
    				if (tableau.addConstraint(eq)) {
    					addGenerated(eq);
    					stats.incProds(Stats.UNIT_SUBSUMPTION_GEN);
    				}
    			}
    			else {
    				HashSet<Constraint> con = new HashSet<Constraint>();
    				for (int j=0; j < g.size(); j++) {
    					con.add(new Equal(env, opt, node, Stats.UNIT_SUBSUMPTION_CHK, g.getVar(j), g.getVal(j)));
    					stats.incProds(Stats.UNIT_SUBSUMPTION_GEN);
    				}
    				Conjunction conjunct = new Conjunction(env, node, Stats.UNIT_SUBSUMPTION_CHK, con);
    				if (tableau.addConstraint(conjunct)) {
    					addGenerated(conjunct);
    					stats.incProds(Stats.UNIT_SUBSUMPTION_GEN, conjunct.size() + 1);
    				}
    			}
    		}
    	}
    }

    List<Pair<Literal,Literal>> cands = clause.getUnitSubsumptionCandidates();
    if (cands != null) {
      for (Pair<Literal,Literal> pair : cands) {
        Literal lit  = pair.get1st();
        Literal unit = pair.get2nd();
        if (unit.isSubsuming(lit) != null)
          return false;
        if (unit.isUnifiable(lit) == null)
          continue;
        NotSubsuming nsub = new NotSubsuming(env, opt, node, Stats.UNIT_SUBSUMPTION_CHK, unit.getTerm(), lit.getTerm());
        if (tableau.addConstraint(nsub)) {
          addGenerated(nsub);
          stats.incProds(Stats.UNIT_SUBSUMPTION_GEN);
        }

//        Subst g = pair.get1st().isSubsuming(pair.get2nd());
//        stats.incTests(Stats.UNIT_SUBSUMPTION_GEN);
//        if (g == null)
//          continue;
//        if (g.isEmpty()) {
//          if (env.dbgNow(DBG_TABLEAUX)) {
//            System.out.println();
//            System.out.println("FAILED by UNIS [" + pair.get1st() + " != " + pair.get2nd() + "].");
//          }
//          return false;
//        }
//        else if (g.size() == 1) {
//          NotEqual neq = new NotEqual(env, opt, node, Stats.UNIT_SUBSUMPTION_CHK, g.getVar(0), g.getVal(0));
//          if (tableau.addConstraint(neq)) {
//            addGenerated(neq);
//            stats.incProds(Stats.UNIT_SUBSUMPTION_GEN);
//          }
//        }
//        else {
//          HashSet<Constraint> dis = new HashSet<Constraint>();
//          for (int i=0; i < g.size(); i++)
//            dis.add(new NotEqual(env, opt, node, Stats.UNIT_SUBSUMPTION_CHK, g.getVar(i), g.getVal(i)));
//          Disjunction disjunct = new Disjunction(env, node, Stats.UNIT_SUBSUMPTION_CHK, dis);
//          if (tableau.addConstraint(disjunct)) {
//            addGenerated(disjunct);
//            stats.incProds(Stats.UNIT_SUBSUMPTION_GEN, disjunct.size() + 1);
//          }
//        }
      }
    }
    return true;
  }

  public boolean checkTermDepth(Clause c) throws ParseException{
	  if(tableau.getOptions().getDepthLimit()==0)
		  return true;
	  Clause p = c.instantiate();
	   
	  for(Literal l : p.getLiterals()){
		  if(!checkTermDepth(l.getTerm()))
			  return false;

	  }
	  return true;

  }
  protected boolean checkTermDepth(Term term) {
	  int depth = tableau.getOptions().getDepthLimit();
	  return (term.getDepth() <= depth);
  }

/**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public abstract ProofStep convert();

  /**
   * Returns the used clause in this operator.
   * @return the used clause in this operator.
   */
  public Clause getClause() {
    return null;
  }

  /**
   * Returns the number of symbols in this operation.
   * @return the number of symbols in this operation.
   */
  public int getNumSyms() {
    return 0;
  }

  /**
   * Returns the number of extendable clauses that increases by this operation (for Extension).
   * @return the number of extendable clauses that increases by this operation.
   */
  public int getNumExts() {
    return 0;
  }

  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  abstract public String toSimpleString();

  /** The environment. */
  protected Env env = null;
  /** The node to be operated by this operator. */
  protected Node node = null;
  /** The substitution to be applied. */
  protected Subst subst = null;
  /** The original state of the variable table. */
  protected int state = 0;
  /** The tableau. */
  protected Tableau tableau = null;
  /** The statistics information. */
  protected Stats stats = null;
  /** The variable table. */
  protected VarTable varTable = null;
  /** If true, then it means this is a mandatory operator. */
  protected boolean mandatory = false;
  /** The set of generated constraints when this operator is applied. */
  protected ArrayList<Constraint> generated = null;
  /** The set of satisfied constraints when this operator is applied. */
  protected ArrayList<Constraint> satisfied = null;
  /** The folding-up lemmas generated by the application of this operator. */
  protected ArrayList<Pair<Node,Node>> foldingUps = null;

}
