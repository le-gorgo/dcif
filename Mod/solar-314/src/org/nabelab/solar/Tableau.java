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

package org.nabelab.solar;

import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.constraint.Constraint;
import org.nabelab.solar.constraint.Constraints;
import org.nabelab.solar.operator.EqResolutionChecker;
import org.nabelab.solar.operator.ExtensionChecker;
import org.nabelab.solar.operator.NegEqFlatAllChecker;
import org.nabelab.solar.operator.NegEqFlatChecker;
import org.nabelab.solar.operator.NegationAsFailureChecker;
import org.nabelab.solar.operator.OpChecker;
import org.nabelab.solar.operator.Operator;
import org.nabelab.solar.operator.ReductionChecker;
import org.nabelab.solar.operator.RestartExtensionChecker;
import org.nabelab.solar.operator.SkipChecker;
import org.nabelab.solar.operator.StrongContractionChecker;
import org.nabelab.solar.operator.SymSplitChecker;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.pfield.PFieldChecker;
import org.nabelab.solar.pfield.PFieldCheckerCardConstraints;
import org.nabelab.solar.proof.Proof;
import org.nabelab.solar.util.ArrayQueue;
import org.nabelab.solar.util.Pair;

/**
 * @author nabesima
 *
 */
public class Tableau implements Tags, OptionTypes, DebugTypes {

  /**
   * Constructs a tableau using the specified top clause.
   * @param env   the environment.
   * @param cfp   the consequence finding problem to which this belongs.
   * @param stats the statistics information.
 * @throws Exception 
   */
  public Tableau(Env env, CFP cfp, Stats stats) throws Exception {
    this.env   = env;
    this.cfp   = cfp;
    this.opt   = cfp.getOptions();
    this.stats = stats;
    clauseDB   = new ClauseDB(env, opt, cfp.getClauses());
    clauseDB.addUnitAxiomsForMatching(cfp.getUnitsForMatching());
    varTable   = env.getVarTable();
    litOrder   = cfp.getLitOrder();
    opOrder    = cfp.getOpOrder();
    skipped    = new Skipped(env);
    opCheckers = new ArrayList<OpChecker>();
//    if (opt.divide())
//      opCheckers.add(new ConquerorChecker(env, this));
    if (opt.use(USE_NEGATION_AS_FAILURE))
      opCheckers.add(new NegationAsFailureChecker(env, this));
    if (opt.use(USE_STRONG_CONTRACTION))
      opCheckers.add(new StrongContractionChecker(env, this));
    //if (opt.getEqType() >= CFP.EQ_SMT)
    if (opt.getEqType() != CFP.EQ_AXIOMS_REQUIRED)
      opCheckers.add(new EqResolutionChecker(env, this));
    if (opt.getEqType() == CFP.EQ_SNMTN || opt.getEqType() == CFP.EQ_SNM)
      opCheckers.add(new SymSplitChecker(env, this));
    if (opt.getEqType() == CFP.EQ_SNMT2 || opt.getEqType() == CFP.EQ_MSNT2)
      opCheckers.add(new NegEqFlatChecker(env, this));
    if (opt.getEqType() == CFP.EQ_SNMT2A)
      opCheckers.add(new NegEqFlatAllChecker(env, this));
    opCheckers.add(new ReductionChecker(env, this));
    if (opt.getCalcType() == CALC_ME)
      opCheckers.add(new ExtensionChecker(env, this));
    else
      opCheckers.add(new RestartExtensionChecker(env, this));
    if (!cfp.getPField().isEmpty()) {
      //pfChecker = PFieldChecker.create(env, opt, cfp.getPField());
    	pfChecker = PFieldCheckerCardConstraints.createChecker(env, cfp.getPField());
    	opCheckers.add(new SkipChecker(env, this, pfChecker));
    }
    constraints = new Constraints(env, this);
    appOps      = new AppOps(env, cfp);

    stats.setProds(Stats.UNIT_AXIOM_MATCHING, clauseDB.getNumUnitAxioms());
  }

  /**
   * Resets this tableau.
   */
  public void reset() {
    while (cancel())
      ;
    root = subgoal = new Node(env, cfp.getOptions(), this, cfp.getTopClauses());
    maxNumSkipped = 0;
  }

  /**
   * Returns the root node.
   * @return the root node.
   */
  public Node getRoot() {
    return root;
  }

  /**
   * Sets the top clause.
   * @param topClause  the top clause of this tableau.
   */
  public void setTopClause(Clause topClause) {
    this.topClause = topClause;
  }

  /**
   * Returns the top clause.
   * @param the top clause.
   */
  public Clause getTopClause() {
    return topClause;
  }

  /**
   * Returns the next subgoal of this tableau.
   * @return the next subgoal of this tableau.
   */
  public Node getNextSubgoal() {
    if (!subgoal.isSolved()) // && !subgoal.hasTag(DIVISION_COMPLETED))
      return subgoal;

    while (subgoal.hasChildren()) {
      subgoal = subgoal.getFirstChild();
      if (!subgoal.isSolved())
        return subgoal;
    }

    boolean hasDividedChild = false;
    while (true) {

      if (subgoal.isDividedAndNotCompleted()) {
        // If the subgoal has a divided child, then the subgoal do not need to record a local success.
        if (!hasDividedChild) {
          // Records the substitution to the sub-tableau below the subgoal.
          LSucc g = subgoal.addLocalSuccess();
          if (g != null) {
            if (env.dbgNow(DBG_TABLEAUX) || env.dbgNow(DBG_DIV_CONQ_DETAIL))
              System.out.println(env.getTimeStep() + ": added local success " + g + " to " + subgoal);

            // In the single top clause problem, allows the quick enumeration of found consequences.
            if (root.getNumChildren() == 1 && subgoal.getDepth() == 1) {
              LSuccCache curCache = subgoal.getLSuccCache();
              cfp.addConseqs(curCache.getConseqs());
              stats.setProds(Stats.CONSEQUENCES, cfp.getConseqSet().size());
              stats.setProds(Stats.CONSEQ_LITS , cfp.getConseqSet().getNumLiterals());
              curCache.clear();
            }
          }
        }
        // If the substitution is the most general, then we can complete the search to the subgoal.
        if (subgoal.getLSuccCache().hasEmptySucc()) {
          while (true){
            Operator op = appOps.pop();
            op.cancel();
            if (subgoal == op.getNode())
              break;
          }
          return subgoal;
        }
        else if (subgoal.getLSuccCache().isFull()) {
          getSearchParam().setExhaustiveness(false);
          subgoal.markAsNotExhausted();
          while (true){
            Operator op = appOps.pop();
            op.cancel();
            if (subgoal == op.getNode())
              break;
          }
          return subgoal;
        }

        // Cancel the last operation
        Operator op = appOps.pop();
        op.cancel();
        subgoal = op.getNode();

        return subgoal;
      }

      if (subgoal.getRight() != null)
        return subgoal = subgoal.getRight();
      hasDividedChild = subgoal.isDivided();
      subgoal = subgoal.getParent();
      if (subgoal.isRoot())
        return subgoal;

      if (opt.use(USE_UNIT_LEMMA_MATCHING) && subgoal.getDepth() == subgoal.getShallowestTargetDepth()) {
        Literal ulemma = clauseDB.addUnitLemma(subgoal);
        if (ulemma != null) {
          stats.setProds(Stats.UNIT_LEMMA_MATCHING, clauseDB.getNumUnitLemmas());
          if (env.dbgNow(DBG_UNIT_LEMMA_DETAIL)) {
            System.out.println();
            System.out.println(env.getTimeStep() + " " + this);
            System.out.println("UNIT LEMMA:");
            System.out.println(" " + ulemma.toSimpString());
          }
          else if (env.dbgNow(DBG_UNIT_LEMMA))
            System.out.println(env.getTimeStep() + " UNIT LEMMA: " + ulemma.toSimpString());

          if (opt.use(USE_STRONG_CONTRACTION)) {
            stats.incTests(Stats.STRONG_CONTRACTION);
            // Checking for strong contraction with units.
            Clause ua = clauseDB.hasCompSubsumingUnitAxiom(ulemma);
            if (ua != null) {
              if (env.dbgNow(DBG_STRONG_CONTRACTION))
                System.out.println(env.getTimeStep() + " S-CONTRACT: unit axiom " + ua + " subsumes new unit lemma " + ulemma);
              markAsContractible(root);
            }
            else {
              Clause ul = clauseDB.hasCompSubsumingUnitLemma(ulemma);
              if (ul != null) {
                if (env.dbgNow(DBG_STRONG_CONTRACTION))
                  System.out.println(env.getTimeStep() + " S-CONTRACT: unit lemma " + ul + " subsumes new unit lemma  " + ulemma);
                markAsContractible(root);
              }
            }
          }
        }
      }
      else if (opt.use(USE_IDENTICAL_C_REDUCTION)) {
        Node deepest = subgoal.getDeepestTarget();
        if (deepest != null) {
          assert(!deepest.isRoot());
          assert(deepest.getDepth() < subgoal.getDepth());
          Literal lit = deepest.addFoldingUp(subgoal);
          if (lit != null) {
            stats.incProds(Stats.IDENTICAL_C_REDUCTION);
            getLastOperator().addFoldingUp(subgoal, deepest);
            if (env.dbgNow(DBG_FOLDING_UP_DETAIL)) {
              System.out.println();
              System.out.println(env.getTimeStep() + " " + this);
              System.out.println("FOLDING-UP: added to " + deepest);
              System.out.println(" " + lit);
            }
            else if (env.dbgNow(DBG_FOLDING_UP))
              System.out.println(env.getTimeStep() + " FOLDING-UP: " + lit);


            if (opt.use(USE_STRONG_CONTRACTION)) {
              stats.incTests(Stats.STRONG_CONTRACTION);
              // Checking for strong contraction with units.
              Clause ua = clauseDB.hasSubsumingUnitAxiom(lit);
              if (ua != null) {
                if (env.dbgNow(DBG_STRONG_CONTRACTION))
                  System.out.println(env.getTimeStep() + " S-CONTRACT: unit axiom " + ua + " subsumes new folding-up " + lit);
                markAsContractible(deepest);
              }
              else {
                Clause ul = clauseDB.hasSubsumingUnitLemma(lit);
                if (ul != null) {
                  if (env.dbgNow(DBG_STRONG_CONTRACTION))
                    System.out.println(env.getTimeStep() + " S-CONTRACT: unit lemma " + ul + " subsumes new folding-up " + lit);
                  markAsContractible(deepest);
                }
                // Checking for strong contraction with c-literals.
                else {
                  Node node = deepest.getParent();
                  while (!node.isRoot()) {
                    Node solved = node.compContainsFoldingUp(subgoal);
                    if (solved != null) {
                      if (env.dbgNow(DBG_STRONG_CONTRACTION))
                        System.out.println(env.getTimeStep() + " S-CONTRACT: folding-up " + solved + " comp-equals new folding-up " + lit);
                      markAsContractible(deepest);
                    }
                    node = node.getParent();
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Returns the operator checkers.
   * @return the operator checkers.
   */
  public ArrayList<OpChecker> getOpCheckers() {
    return opCheckers;
  }

  /**
   * Returns the consequence finding problem.
   * @return the consequence finding problem.
   */
  public CFP getCFP() {
    return cfp;
  }

  /**
   * Returns the input clause database.
   * @return the input clause database.
   */
  public ClauseDB getClauseDB() {
    return clauseDB;
  }

  /**
   * Returns the literal ordering.
   * @return the literal ordering.
   */
  public LitOrder getLitOrder() {
    return litOrder;
  }

  /**
   * Returns the literal ordering.
   * @return the literal ordering.
   */
  public OpOrder getOpOrder() {
    return opOrder;
  }

  /**
   * Applies the specific operator to this tableau.
   * @param op the operator to be applied.
   * @return true if the application of the operator succeeds.
 * @throws ParseException 
   */
  public boolean apply(Operator op) throws ParseException {
    if (op.apply() == false)
      return false;
    appOps.push(op);
    return true;
  }

  /**
   * Cancels the last applied operator.
   * @Returns true if the cancellation succeeded.
   */
  public boolean cancel() {
    if (appOps.isEmpty())
      return false;

    Operator op = appOps.pop();

//  // Records the local failure to the cache.
//  if (opt.use(USE_LOCAL_FAILURE_CACHE)) {
//    Node prev = op.getNode();
//    if (prev.getRight() == subgoal && prev.hasNextOperator()) {
//
//      LFail fail = prev.addLocalFailure();
//
//      if (opt.dbgNow(DBG_LOCAL_FAIL_DETAIL)) {
//        System.out.println();
//        System.out.format("LFAIL: added %s to %s\n", fail, prev);
//        System.out.println("LFAIL: cache " + prev.getLFailCache());
//      }
//      else if (opt.dbgNow(DBG_LOCAL_FAIL))
//        System.out.format("%d LFAIL: %s(%d)\n", inf(), fail.toString(), fail.getNumSyms());
//    }
//  }

    if (subgoal != op.getNode()) {
      subgoal.reset();

      if (opt.use(USE_LOCAL_FAILURE_CACHE) && !subgoal.isDivided()) {
        Node left = subgoal.getLeft();
        if (!subgoal.hasNAF() && left != null && !left.isSolvable() &&
            (param.getDepthLimit() == 0 || left.getExtDepth() < param.getDepthLimit())) {
          LFail fail = left.addLocalFailure();
          if (fail != null) {
            if (env.dbgNow(DBG_LOCAL_FAIL_DETAIL)) {
              System.out.println();
              System.out.format("LFAIL: added %s to %s\n", fail, left);
              System.out.println("LFAIL: cache " + left.getLFailCache());
            }
            else if (env.dbgNow(DBG_LOCAL_FAIL))
              System.out.format("%d LFAIL: %s(%d)\n", env.getTimeStep(), fail.toString(), fail.getNumSyms());
          }
        }
      }
    }

    // Cancel the last operation.
    op.cancel();

    // Restores the next subgoal.
    subgoal = op.getNode();

//    // Records the local failure to the cache.
//    if (opt.use(USE_LOCAL_FAILURE_CACHE) && !subgoal.hasNextOperator()) {
//      Node prev = subgoal.getLeft();
//      if (prev != null && prev.hasNextOperator()) {
//
//        LFail fail = prev.addLocalFailure();
//
//        if (opt.dbgNow(DBG_LOCAL_FAIL_DETAIL)) {
//          System.out.println();
//          System.out.println(inf() + " " + this);
//          System.out.println("LFAIL: added to " + prev);
//          System.out.format(" %s(%d)\n", fail.toString(), fail.getNumSyms());
//        }
//        else if (opt.dbgNow(DBG_LOCAL_FAIL))
//          System.out.format("%d LFAIL: %s(%d)\n", inf(), fail.toString(), fail.getNumSyms());
//     }
//    }

    return true;
  }

  /**
   * Returns the proof of the tableau.
   * @param target the target clause of proof.
   * @return the proof of the tableau.
   */
  public Proof getProof(Conseq target) {
    return getProof(target, null);
  }

  /**
   * Returns the proof of the specified node.
   * @param target the target clause of proof.
   * @param node the node to prove.
   * @return the proof of the specified node.
   */
  public Proof getProof(Conseq target, Node node) {
    return appOps.getProof(target, node);
  }

  /**
   * Returns the set of used clauses in this proof.
   * @return the set of used clauses in this proof.
   */
  public List<Clause> getUsedClauses() {
    return appOps.getUsedClauses();
  }

  /**
   * Returns true if this tableau is solved.
   * @return true if this tableau is solved.
   */
  public boolean isSolved() {
    return numOpenNodes == 0 && constraints.backtrackUntilSatisfiable();
  }

  /**
   * Returns the number of open nodes in this tableau.
   * @return the number of open nodes in this tableau.
   */
  public int getNumOpenNodes() {
    return numOpenNodes;
  }

  /**
   * Returns true if the constraints of this tableau is satisfiable.
   * @return true if the constraints of this tableau is satisfiable.
   */
  public boolean isSatisfiable() {
    return false;
  }

  /**
   * Removes the closed sub-tableau whose root node has NAF.
   * @return true if the removal of the closed NAF sub-tableau is succeed.
   */
  public boolean removeClosedNAFSubTableau() {
    if (subgoal.isRoot())
      return true;
    if (subgoal.hasChildren())
      return true;

    if (subgoal.hasNAF() && subgoal.isClosed() && !subgoal.hasTag(CLOSED_BY_NAF)) {
      // Backtracks to the previous node.
      cancel();         // cancel this NAF node.
      return cancel();  // cancel the previous node.
    }

    // If there is a closed NAF sub-tableau, then removes it.
    Node node = subgoal;
    while (node.getRight() == null) {
      node = node.getParent();
      if (node.isRoot())
        break;
      if (node.hasNAF()) {
        while (subgoal != node)
          cancel();
        return cancel();  // cancel the previous node.
      }
    }

    // If there is an ancestor which has NAF and NOT_EXHAUSTED, then removes it.
    node = subgoal;
    while (node.hasTag(NOT_EXHAUSTED)) {
      if (node.isRoot())
        return true;
      if (node.hasNAF()) {
        while (subgoal != node)
          cancel();
        return cancel();  // cancel the previous node.
      }
      node = node.getParent();
    }

    return true;
  }

  /**
   * Returns true if the removal of redundancy succeeds.
   * @return true if the removal of redundancy is succeed.
   */
  public boolean removeRedundancy() {

    if (!constraints.backtrackUntilSatisfiable())
      return false;

    if (opt.use(USE_LOCAL_FAILURE_CACHE)) {
      Node node = subgoal;
      Clause curSkipped = skipped.convertToInstantiatedClause();
      while (!node.isRoot()) {
        stats.incTests(Stats.LOCAL_FAILURE_CACHE);
        LFail fail = node.hasMoreGeneralFailure(curSkipped);
        if (fail != null) {
          stats.incSuccs(Stats.LOCAL_FAILURE_CACHE);
          if (env.dbgNow(DBG_TABLEAUX)) {
            System.out.println();
            System.out.println("FAILED by local failure " + fail + " in " + node + ".");
            System.out.println(" VarTable:");
//            for (int i=0; i < fail.size(); i++) {
//              Term var = varTable.getTailVar(fail.getVar(i));
//              int varname = var.getStartName() + var.getOffset();
//              Term val = varTable.getValue(varname);
//              if (val != null)
//                System.out.format("  _%d: %s\n", fail.getVar(i), val);
//              else
//                System.out.format("  _%d: %s\n", fail.getVar(i), var);
//            }
            System.out.println(varTable);
          }
          if (!cancel())
            return false;
          break;
        }
        node = node.getParent();
        // Checks local failure caches from subgoal to the ancestor at the half depth.
        if (node.getDepth() <= (subgoal.getDepth() >> 1))  // Heuristics
          break;
      }
    }

//    // Skip-minimality checking
//    if (opt.use(USE_SKIP_MINIMALITY)) {
//      curConseq = skipped.convertToConseq();
//      if (cfp.getConseqSet().isSubsuming(curConseq.getFVec(true), curConseq)) {
//
////        // TEST
////        Conseq c = skipped.convertToConseq();
////        if (!cfp.getConseqSet().isSubsuming(c.getFVec(true), c)) {
////          System.out.println("cur = " + curConseq);
////          System.out.println("c   = " + c);
////        }
//
//        stats().incSuccs(Stats.SKIP_MINIMALITY);
//        if (!cancel())
//          return false;
//      }
//    }

    return true;
  }

  /**
   * Adds the specified constraint to this tableau.
   * @param c a constraint to be added.
   * @return true if the tableau did not already contain the specified constraint.
   */
  public boolean addConstraint(Constraint c) {
    return constraints.add(c);
  }

  /**
   * Adds the specified set of constraints to this tableau.
   * @param cs a set of constraints to be added.
   */
  public void addConstraints(List<Constraint> cs) {
    constraints.addAll(cs);
  }

  /**
   * Removes the specified set of constraints from this tableau.
   * @param cs the set of constraint to be removed.
   */
  public void removeConstraints(ArrayList<Constraint> cs) {
    constraints.removeAll(cs);
  }

  /**
   * Return the set of constraints of this tableau.
   * @return the set of constraints of this tableau.
   */
  public Constraints getConstraints() {
  	return constraints;
  }

  /**
   * Returns the applied operators to this tableau.
   * @return the applied operators to this tableau.
   */
  public AppOps getAppOps() {
    return appOps;
  }

  /**
   * Returns the last applied operator.
   * @return the last applied operator.
   */
  public Operator getLastOperator() {
    if (appOps.isEmpty())
      return null;
    return appOps.last();
  }

  /**
   * Adds the specified node as skipped nodes.
   * @param node the node to be skipped.
   */
  public void addSkippedNode(Node node) {
    skipped.add(node);
    if (skipped.size() > maxNumSkipped)
      maxNumSkipped = skipped.size();
  }

  /**
   * Adds the specified node as skipped nodes.
   * @param node the node to be skipped.
   */
  public void removeSkippedNode(Node node) {
    skipped.remove(node);
  }

  /**
   * Returns the skipped nodes.
   * @return the skipped nodes.
   */
  public Skipped getSkipped() {
    return skipped;
  }

  /**
   * Returns the production field checker.
   * @return the production field checker.
   */
  public PFieldChecker getPFChecker() {
    return pfChecker;
  }
  /**
   * Returns the maximum number of skipped nodes.
   * @return the maximum number of skipped nodes.
   */
  public int getMaxNumSkipped() {
    return maxNumSkipped;
  }

  /**
   * Returns the consequence from the solved tableau.
   * @return the consequence from the solved tableau.
   */
  public Conseq getConseq() {
    Conseq conseq = skipped.convertToConseq();
    conseq.rename(env.getNegVarRenameMap());
    return conseq;
  }

  /**
   * Instantiates all variables in this tableau are replaced with the values.
   */
  public void instantiate() {
    ArrayQueue<Node> queue = env.getNodeQueue();
    queue.add(root);
    while (!queue.isEmpty()) {
      Node n = queue.remove();
      n.instantiate();
      Node c = n.getFirstChild();
      while (c != null) {
        queue.add(c);
        c = c.getRight();
      }
    }
    constraints.instantiate();
  }

  /**
   * Adds the specified tag to all the nodes in this tableau.
   * @param tag the tag to add.
   */
  public void markAs(int tag) {
    markAs(tag, root);
  }

  /**
   * Adds the specified tag to the nodes below the specified start node.
   * @param tag    the tag to add.
   * @param start  the start node.
   */
  public void markAs(int tag, Node start) {
    ArrayQueue<Node> queue = env.getNodeQueue();
    queue.add(start);
    while (!queue.isEmpty()) {
      Node n = queue.remove();
      n.addTag(tag);
      Node c = n.getFirstChild();
      while (c != null) {
        queue.add(c);
        c = c.getRight();
      }
    }
  }

  /**
   * Adds the tag CONTRACTIBLE to nodes between from and to.
   * @param start  the start node.
   */
  public void markAsContractible(Node start) {
    ArrayQueue<Node> queue = env.getNodeQueue();
    queue.add(start);
    while (!queue.isEmpty()) {
      Node n = queue.remove();
      if (!n.isSolved())
        n.addTag(CONTRACTIBLE);
      Node c = n.getFirstChild();
      while (c != null) {
        queue.add(c);
        c = c.getRight();
      }
    }
  }

  /**
   * Returns the complementary unifiable nodes with the specified nodes.
   * @param lit  the specified literal to check.
   * @return the unifiable literals with the specified literal.
   */
  public List<Pair<Node,Subst>> findCompUnifiable(Node node) {
    Literal lit = node.getLiteral();
    ArrayList<Pair<Node,Subst>> unifiable = null;

    ArrayQueue<Node> queue = env.getNodeQueue();
    queue.add(root);
    while (!queue.isEmpty()) {
      Node  n = queue.remove();
      if (n.getLiteral() != null) {
        Subst g = n.getLiteral().isCompUnifiable(lit);
        if (g != null) {
          if (unifiable == null)
            unifiable = new ArrayList<Pair<Node,Subst>>();
          unifiable.add(new Pair<Node,Subst>(n, g));
        }
      }
      Node c = n.getFirstChild();
      while (c != null) {
        queue.add(c);
        c = c.getRight();
      }
    }
    return unifiable;
  }

  /**
   * Adds the specified number to the number of nodes.
   * @param numNodes the number of nodes to add.
   * @param numOpenNodes the number of open nodes to add.
   */
  public void incNodes(int numNodes) {
    this.numNodes += numNodes;
    this.numOpenNodes += numNodes;
  }

  /**
   * Increments the number of open nodes.
   */
  public void incOpenNodes() {
    this.numOpenNodes++;
  }

  /**
   * Subtracts the specified number to the number of open nodes.
   * @param numNodes the number of nodes to be subtracted.
   * @param numOpenNodes the number of open nodes to be subtracted.
   */
  public void decNodes(int numNodes) {
    this.numNodes -= numNodes;
    this.numOpenNodes -= numNodes;
  }

  /**
   * Decrements the number of open nodes.
   */
  public void decOpenNodes() {
    this.numOpenNodes--;
  }

  /**
   * Sets the search parameter.
   * @param param the search parameter.
   */
  public void setSearchParam(SearchParam param) {
    this.param = param;
    for (OpChecker opChecker : opCheckers)
      opChecker.setSearchParam(param);
    if (pfChecker != null)
      pfChecker.setMaxLength(param.getMaxLenConseqs());
  }

  /**
   * Returns the search parameter.
   * @return a search parameter.
   */
  public SearchParam getSearchParam() {
    return param;
  }

  /**
   * Returns the statistics information object.
   * @param the statistics information object.
   */
  public Stats stats() {
    return stats;
  }

  /**
   * Returns the variable table.
   * @return the variable table of this problem.
   */
  public VarTable getVarTable() {
    return varTable;
  }

  /**
   * Returns the options.
   * @return the options.
   */
  public Options getOptions() {
    return opt;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append(String.format("(%d/%d nodes)\n", numOpenNodes, numNodes));
    str.append(String.format("root @%d\n", root.getInfStep()));

    Node node = root.getFirstChild();
    while (node != null) {

      // Makes indents in proportion to the number of ancestors.
      StringBuilder indent = new StringBuilder();
      Node n = node;
      while (!n.getParent().isRoot()) {
        n = n.getParent();
        if (n.getRight() != null)
          indent.insert(0, "| ");
        else
          indent.insert(0, "  ");
      }
      indent.insert(0, " ");

      // Prints the node.
      str.append(indent);
      str.append(node.isDivided() ? "* " : "+ ");
      str.append(node.toString());

      // Prints the tags of the node.
      str.append(" ");
      if (node.hasTag(SKIPPED))
        str.append("(skip)");
      if (node.hasTag(FACTORED))
        str.append("(factoring)");
      if (node.hasTag(MERGED))
        str.append("(merged)");
      if (node.hasTag(EXTENDED))
        str.append("(ext)");
      if (node.hasTag(RESTARTTED))
        str.append("(restart)");
      if (node.hasTag(EQ_RESOLVED))
        str.append("(eq resolve)");
      if (node.hasTag(SYMMETRY_SPLITTED))
        str.append("(sym split)");
      if (node.hasTag(NEG_EQ_FLATTENED))
        str.append("(neg eq flat)");
      if (node.hasTag(EQ_EXTENDED))
        str.append("(neg eq ext)");
      if (node.hasTag(REDUCED))
        str.append("(reduce)");
      if (node.hasTag(IDENTICAL_REDUCED))
        str.append("(id reduce)");
      if (node.hasTag(IDENTICAL_FOLDING_DOWN))
        str.append("(id folding-down)");
      if (node.hasTag(UNIT_AXIOM_MATCHED))
        str.append("(unit axiom)");
      if (node.hasTag(UNIT_LEMMA_MATCHED))
        str.append("(unit lemma)");
      if (node.hasTag(UNIT_LEMMA_EXTENSION))
        str.append("(unit lemma ext)");
      if (node.hasTag(STRONG_CONTRACTION))
        str.append("(st contract)");
      if (node.hasTag(IDENTICAL_C_REDUCED))
        str.append("(id c-reduce)");
      if (node.hasTag(EQ_RAW))
        str.append(" [raw]");
      if (node.hasTag(EQ_MATURE))
        str.append(" [mature]");
      if (node.hasTag(CONTRACTIBLE))
        str.append(" [contractible]");
      if (node.hasTag(DIVISION_COMPLETED))
        str.append(" (cqr)");
      if (node.hasTag(CLOSED_BY_NAF))
        str.append("(naf)");
      if (node.hasTag(SOLVABLE))
        str.append(" [solvable]");

      // MEMO test
//      if (node.getProof() != null)
//        str.append(" [proof]");

      // Subgoal?
      if (node == subgoal)
        str.append(" <subgoal>");

      // test
      //str.append(" exdep=" + node.getExtDepth());
      if (node.isDivided() && node.getLSuccCache() != null)
        str.append(" max=" + node.getLSuccCache().getMaxSize());

      if (node.getInfStep() != 0)
        str.append(" @" + node.getInfStep());

      if (node.hasTag(NOT_EXHAUSTED))
        str.append(" +");

      str.append("\n");

      // Is the node extended?
      if (node.getExtChild() != null) {
        str.append(indent);
        if (node.getRight() != null)
          str.append("| + ");
        else
          str.append("  + ");
        str.append(node.getExtChild());
        str.append(" (ext target)\n");
      }

      node = node.getNext();
    }

    if (env.dbg(DBG_CONSTRAINT) && !constraints.isEmpty()) {
      str.append("\n");
      str.append("Constraints:\n");
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
  /** The search parameter. */
  private SearchParam param = null;
  /** The statistics information. */
  private Stats stats = null;
  /** The variable table. */
  private VarTable varTable = null;
  /** The literal ordering in tableau clauses. */
  private LitOrder litOrder = null;
  /** The operator ordering in tableau clauses. */
  private OpOrder opOrder = null;
  /** The input clause database. */
  private ClauseDB clauseDB = null;
  /** The root node. */
  private Node root = null;
  /** The top clause. */
  private Clause topClause = null;
  /** The current subgoal. */
  private Node subgoal = null;
  /** The number of open nodes in this tableau. */
  private int numOpenNodes = 1;
  /** The number of nodes in this tableau. */
  private int numNodes = 1;
  /** The production field checker. */
  private PFieldChecker pfChecker = null;
  /** The set of operation checkers. */
  private ArrayList<OpChecker> opCheckers = null;
  /** The set of applied operators. */
  private AppOps appOps = null;
  /** The cutoff rule checker. */
  //private CutRuleChecker cutRuleChecker = null;
  /** The set of constraints. */
  private Constraints constraints = null;
  /** The set of skipped nodes. */
  private Skipped skipped = null;
  /** The maximum number of skipped nodes. */
  private int maxNumSkipped = 0;

}
