package org.nabelab.solar.operator;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Env;
import org.nabelab.solar.LSuccCache;
import org.nabelab.solar.Node;
import org.nabelab.solar.Stats;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ProofStep;

public class Conqueror extends Operator {

  /**
   * Constructs a negation as failure operator which is applied to the specified node.
   * @param env    the environment.
   * @param node   the specified node.
   */
  public Conqueror(Env env, Node node) {
    super(env, node);
    //tableau.stats().incProds(Stats.NEGATION_AS_FAILURE);
  }

  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {

    LSuccCache curCache = node.getLSuccCache();
    curCache.setEndInfStep(stats.inf());

    if (env.dbgNow(DBG_DIV_CONQ_DETAIL) && !curCache.isEmpty()) {
      if (!env.dbgNow(DBG_TABLEAUX)) {
        System.out.println(stats.inf());
        System.out.println(tableau);
      }
      System.out.println("Current succ cache of subgoal (" + curCache.size() + " succs, " + curCache.getInfSteps() + "infs)");
      System.out.println(curCache);
    }

    if (node.getLeft() != null && !curCache.isEmpty()) {
      Node prenode = node.getLeft();
      LSuccCache preCache = prenode.getLSuccCache();
      if (env.dbgNow(DBG_DIV_CONQ_DETAIL)) {
        System.out.println("Previous succ cache of prev node (" + preCache.size() + " succs, " + preCache.getInfSteps() + "infs)");
        System.out.println(preCache);
      }
      if (env.dbg(DBG_DIV_CONQ)) { //
        if (preCache.size() > 100 || curCache.size() > 100) {
          System.out.println("Pre node:" + prenode);
          System.out.println("Cur node:" + node);
        }
        //System.out.println(node.getTableau());
        System.out.print(env.getTimeStep() + ": dep " + node.getDepth() + ": combine " + preCache.size() + " vs " + curCache.size() + " = ");
      }
      LSuccCache combined = preCache.combine(curCache);
      if (env.dbg(DBG_DIV_CONQ)) // preCache.size() > 100 || curCache.size() > 100)
        System.out.println(combined.size());
      combined.setStartInfStep(preCache.getStartInfStep());
      combined.setEndInfStep(curCache.getEndInfStep());
      if (env.dbgNow(DBG_DIV_CONQ_DETAIL)) {
        System.out.println("Combined succ cache (" + combined.size() + "succs)");
        System.out.println(combined);
      }
      prenode.clearLSuccCache();
      node.setLSuccCache(combined);
      curCache = combined;
    }

    if (curCache.isEmpty()) {
      if (env.dbgNow(DBG_TABLEAUX))
        System.out.println("NO LOCAL SUCCESSES");
      return false;
    }

    node.addTag(DIVISION_COMPLETED);
    super.apply();
    state = varTable.state();
    //tableau.stats().incSuccs(Stats.NEGATION_AS_FAILURE);

    if (node.getRight() != null)
      return true;

    Node parent = node.getParent();
    if (parent.isRoot()) {
      CFP cfp = tableau.getCFP();
      cfp.addConseqs(curCache.getConseqs());
      stats.setProds(Stats.CONSEQUENCES, cfp.getConseqSet().size());
      stats.setProds(Stats.CONSEQ_LITS , cfp.getConseqSet().getNumLiterals());
    }
    else {
      parent.addLocalSuccess(curCache);
      if (env.dbgNow(DBG_TABLEAUX) || env.dbgNow(DBG_DIV_CONQ_DETAIL)) {
        System.out.println(env.getTimeStep() + ": Added local succs to the parent node: " + node.getParent());
        System.out.println(node.getParent().getLSuccCache());
      }
    }

    return true;
  }

  /**
   * Cancels this operator.
   */
  public void cancel() {
    node.removeTag(DIVISION_COMPLETED);
    varTable.backtrackTo(state);
    if (node.getOrgNumVars() < varTable.getNumVars())
      varTable.removeVars(varTable.getNumVars() - node.getOrgNumVars());
    super.cancel();
  }

  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return null;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    return "CQR " + node;
  }

  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[CQR]";
  }
}
