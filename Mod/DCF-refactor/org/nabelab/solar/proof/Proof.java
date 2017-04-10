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

package org.nabelab.solar.proof;

import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Clause;
import org.nabelab.solar.Conseq;
import org.nabelab.solar.Env;
import org.nabelab.solar.Node;
import org.nabelab.solar.OptionTypes;
import org.nabelab.solar.Options;
import org.nabelab.solar.Stats;
import org.nabelab.solar.Tableau;
import org.nabelab.solar.VarTable;
import org.nabelab.solar.constraint.Constraint;
import org.nabelab.solar.operator.Operator;
import org.nabelab.solar.parser.ParseException;

/**
 * @author nabesima
 *
 */
public class Proof implements OptionTypes {

  /**
   * Constructs a proof.
   * @param env    the environment.
   * @param cfp    the consequence finding problem.
   * @param target the target consequence of this proof.
   * @param used   the list of used operations.
   */
  public Proof(Env env, CFP cfp, Conseq target, List<Operator> used) {
    this.env = env;
    this.cfp = cfp;
    this.opt = cfp.getOptions();
    this.target = target;
    for (Operator op : used)
      steps.add(op.convert());
  }

  /**
   * Constructs a copy of the specified proof.
   * @param proof  the proof to be copied.
   * @param target the target consequence of this proof.
   */
  public Proof(Proof proof, Conseq target) {
    this.env = proof.env;
    this.cfp = proof.cfp;
    this.opt = proof.opt;
    this.solved = proof.solved;
    this.usedClauses = proof.usedClauses;
    this.target = target;
    this.steps = new ArrayList<ProofStep>(proof.steps);
  }

  /**
   * Returns true if the proof steps are valid.
   * @return true if the proof steps are valid.
 * @throws Exception 
   */
  public boolean validate() throws Exception {
    // Turn off the skip-minimality.
    boolean skmin = opt.use(USE_SKIP_MINIMALITY);
    opt.set(USE_SKIP_MINIMALITY, false);
    // Resets the variable table.
    VarTable varTable = env.getVarTable();
    assert(varTable.state() == 0);

    CFP empty = new CFP(env, opt);
    empty.setPField(cfp.getPField());
    Tableau tableau = new Tableau(env, empty, new Stats());
    tableau.reset();

    if (!validate(tableau, steps))
      return false;

    // Generates a consequence.
    Conseq conseq = tableau.getConseq();

    // Records the solved tableau.
    if (tableau.isSolved()) {
      solved = tableau;
      solved.instantiate();
    }
    // Records the used clauses.
    if (opt.hasUsedClausesOp()) {
      usedClauses = new ArrayList<Clause>();
      for (Clause c : tableau.getUsedClauses())
        usedClauses.add(c.instantiate());
      target.setUsedClauses(usedClauses);
    }

    // Resets the variable table.
    varTable.backtrackTo(0);
    varTable.removeAllVars();

    // Validates the consequence.
    if (!(conseq.subsumes(target) && target.subsumes(conseq))) {
      System.out.println("Error: invalid consequence");
      System.out.println(" target = " + target);
      System.out.println(" conseq = " + conseq);

      solved = null;
      System.out.println(this);

      return false;
    }

    // Recovers the skip-minimality.
    opt.set(USE_SKIP_MINIMALITY, skmin);

    return true;
  }

  private boolean validate(Tableau tableau, ArrayList<ProofStep> steps) throws ParseException {
    for (ProofStep step : steps) {
      tableau.stats().incInf();
      // Expands the unit lemma matching.
      if (step instanceof UnitLemmaMatchingStep) {
        UnitLemmaMatchingStep ulm = (UnitLemmaMatchingStep)step;
        Proof proof = ulm.getProof();
        if (!validate(tableau, proof.steps))
          return false;
        continue;
      }
      Node node = tableau.getNextSubgoal();
      Operator op = step.convert(tableau, node);
      if (op == null) {
        System.out.println("Error: invalid operation " + step);
        return false;
      }
      if (!tableau.apply(op)) {
        System.out.println("Error: cannot apply the operation " + op);
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the solved tableau.
   * @return the solved tableau.
 * @throws Exception 
   */
  public Tableau getSolvedTableau() throws Exception {
    if (solved == null)
      validate();
    return solved;
  }

  /**
   * Returns the set of used clauses in this proof.
   * @return the set of used clauses in this proof.
 * @throws Exception 
   */
  public List<Clause> getUsedClauses() throws Exception {
    if (usedClauses == null)
      validate();
    return usedClauses;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();
    if (solved == null) {
      for (ProofStep step : steps)
        str.append(step.toString() + "\n");
      return str.toString();
    }
    str.append(solved.toString());
    List<Constraint> constraints = solved.getConstraints().getUnknowns();
    if (!constraints.isEmpty()) {
    	str.append("\nConstraints:\n");
    	for (Constraint c : constraints)
    		str.append(" " + c + "\n");
    }
    return str.toString();
  }

  /** The environment. */
  private Env env = null;
  /** The consequence finding problem. */
  private CFP cfp = null;
  /** The options. */
  private Options opt = null;
  /** The target of this proof. */
  private Conseq target = null;
  /** The solved tableau of this proof. */
  private Tableau solved = null;
  /** The set of used clauses for solving the tableau. */
  private ArrayList<Clause> usedClauses = null;
  /** The list of proof steps. */
  private ArrayList<ProofStep> steps = new ArrayList<ProofStep>();
}
