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

import java.util.HashSet;
import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.ConseqSet;
import org.nabelab.solar.DebugTypes;
import org.nabelab.solar.Env;
import org.nabelab.solar.Node;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Subst;
import org.nabelab.solar.Unifiable;
import org.nabelab.solar.constraint.Constraint;
import org.nabelab.solar.constraint.Disjunction;
import org.nabelab.solar.constraint.NotEqual;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PFieldItem;
import org.nabelab.solar.proof.ProofStep;
import org.nabelab.solar.proof.SkipStep;
import org.nabelab.solar.util.Pair;

/**
 * @author nabesima
 *
 */
public class Skip extends Operator implements DebugTypes {

  /**
   * Constructs a skip operator which is applied to the specified node.
   * @param env the environment.
   * @param node the specified node.
   * @param unifiable the unifiable production field item.
   */
  public Skip(Env env, Node node, Unifiable<PFieldItem> unifiable) {
    super(env, node, unifiable.getSubst());
    this.pfieldItem = unifiable.getObject();
    stats.incProds(Stats.SKIP);
  }

  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    
    // Applies the operator.
    if (subst != null)
      varTable.addVars(pfieldItem.getNumVars());
    super.apply();
    node.addTag(SKIPPED);
    pfieldItem.skip();
    tableau.addSkippedNode(node);
    stats.incSuccs(Stats.SKIP);
    
    // check term Depth
    if(!checkTermDepth(node)){
    	tableau.removeSkippedNode(node);
        pfieldItem.unskip();
        node.removeTag(SKIPPED);
        super.cancel();
        if (subst != null)
          varTable.removeVars(pfieldItem.getNumVars());
        stats.incSuccs(Stats.SKIP_MINIMALITY);
        return false;
    }
    // Constructs constraints for satisfying the skip-regularity.
    if (tableau.getOptions().use(USE_SKIP_REGULARITY)) {

      // Adaptive use of skip-regularity (test1)
      boolean use = true;
      if (tableau.getOptions().use(USE_TEST1)) {
        stats.incTests(Stats.SKIP_REGULARITY_TRY);
        if (stats.getSuccs(Stats.SKIP_REGULARITY_GEN) != 0) {
          long interval = stats.getTests(Stats.SKIP_REGULARITY_GEN) / stats.getSuccs(Stats.SKIP_REGULARITY_GEN);
          interval >>= 2;
          if (stats.getTests(Stats.SKIP_REGULARITY_TRY) % interval != 0)
            use = false;
        }
      }
      
      if (use) {
        stats.incTests(Stats.SKIP_REGULARITY_GEN);
        List<Pair<Node,Subst>> unifiables = tableau.findCompUnifiable(node);
        if (unifiables != null) {
          for (int i=0; i < unifiables.size(); i++) {
            Pair<Node,Subst> pair = unifiables.get(i);
            Subst g = pair.get2nd();
            if (g.isEmpty()) {
              tableau.removeSkippedNode(node);
              pfieldItem.unskip();
              node.removeTag(SKIPPED);
              super.cancel();
              if (subst != null)
                varTable.removeVars(pfieldItem.getNumVars());
              stats.incSuccs(Stats.SKIP_REGULARITY_GEN);
              return false;
            }
            else if (g.size() == 1) {
              NotEqual neq = new NotEqual(env, tableau.getOptions(), node, Stats.SKIP_REGULARITY_CHK, g.getVar(0), g.getVal(0)); 
              if (tableau.addConstraint(neq)) {
                addGenerated(neq);
                stats.incProds(Stats.SKIP_REGULARITY_GEN);
              }
            }
            else {
              HashSet<Constraint> dis = new HashSet<Constraint>();
              for (int j=0; j < g.size(); j++)
                dis.add(new NotEqual(env, tableau.getOptions(), node, Stats.SKIP_REGULARITY_CHK, g.getVar(j), g.getVal(j)));
              Disjunction disjunct = new Disjunction(env, node, Stats.SKIP_REGULARITY_CHK, dis);
              if (tableau.addConstraint(disjunct)) {
                addGenerated(disjunct);
                stats.incProds(Stats.SKIP_REGULARITY_GEN, disjunct.size() + 1);
              }
            }          
          }
        }
      }
    }   
    
    // Skip-minimality checking
    if (tableau.getOptions().use(USE_SKIP_MINIMALITY)) {
      Clause skipped = tableau.getSkipped().convertToInstantiatedClause();
      stats.incTests(Stats.SKIP_MINIMALITY);
      ConseqSet conseqSet = tableau.getCFP().getConseqSet();
      Clause subsuming = conseqSet.findSubsuming(skipped.getFVec(true), skipped); 
      stats.setSuccs(Stats.SKIP_MIN_SUBSUMP_CHK, conseqSet.getNumSubsumChecks());
      stats.setTests(Stats.SKIP_MIN_SUBSUMP_CHK, conseqSet.getNumSubsumChecksWithoutFiltering());          
      if (subsuming != null) {
        if (env.dbg(DBG_SKIP_MINIMALITY)) {
          System.out.println(env.getTimeStep() + ": skip-minimality removed = " + skipped);
          System.out.println("  subsumed by " + subsuming.toSimpString());
        }
        ////tableau.markAs(Tags.SOLVABLE);
        tableau.removeSkippedNode(node);
        pfieldItem.unskip();
        node.removeTag(SKIPPED);
        super.cancel();
        if (subst != null)
          varTable.removeVars(pfieldItem.getNumVars());
        stats.incSuccs(Stats.SKIP_MINIMALITY);
        return false;
      }
    }
    
    return true;
  }

  private boolean checkTermDepth(Node node) {
	  Node n = node.getRight();
	  while (n != null){
		if (!checkTermDepth(n.getLiteral().getTerm()))
				return false;
		else n = n.getRight();
	  }
	  return true;
}

/**
   * Cancels this operator.
   */
  public void cancel() {
    tableau.removeSkippedNode(node);
    pfieldItem.unskip();
    node.removeTag(SKIPPED);
    super.cancel();
    if (subst != null)
      varTable.removeVars(pfieldItem.getNumVars());    
  }

  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return new SkipStep(env, node.getLiteral(), pfieldItem.getPLiteral());
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    return "SKP " + node + "/" + subst;
  }
  
  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[SKP]";
  }
  
  /** The production field item. */
  private PFieldItem pfieldItem = null;
  
}
