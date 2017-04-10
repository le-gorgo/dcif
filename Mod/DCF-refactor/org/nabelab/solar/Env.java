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
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;

import org.nabelab.debug.Debuggable;
import org.nabelab.solar.constraint.Constraint;
import org.nabelab.solar.equality.PriorityMap;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.equality.WeightMap;
import org.nabelab.solar.indexing.FVecCand;
import org.nabelab.solar.indexing.FVecMap;
import org.nabelab.solar.pfield.PField;
import org.nabelab.solar.util.ArrayQueue;
import org.nabelab.solar.util.ArrayStack;
import org.nabelab.util.Counter;

/**
 * @author nabesima
 *
 */
public class Env implements Debuggable, DebugTypes {

  /**
   * Constructs a environment.
   */
  public Env() {
    stats    = new Stats();
    symTable = new SymTable();
    varTable = new VarTable(this);
    debug = new boolean[256];
    for (int i=0; i < debug.length; i++)
      debug[i] = false;
  }

  /**
   * Constructs a environment with a new variable table.
   * @param symTable  a symbol table
   * @param debug     a debug information.
   */
  public Env(SymTable symTable, boolean[] debug) {
  	this.stats    = new Stats();
  	this.symTable = symTable;
  	this.varTable = new VarTable(this);
  	this.debug    = debug;
  }

  /**
   * Returns the statistics information.
   * @return the statistics information.
   */
  public Stats getStats() {
    return stats;
  }

  /**
   * Returns a debug information.
   * @return a debug information.
   */
  public boolean[] getDebug() {
  	return debug;
  }

  /**
   * Returns the statistics information.
   * @return the statistics information.
   */
  public void initStats() {
    stats = new Stats();
  }

  /**
   * Returns the symbol table.
   * @return the symbol table.
   */
  public SymTable getSymTable() {
    return symTable;
  }

  /**
   * Returns the variable table.
   * @return the variable table.
   */
  public VarTable getVarTable() {
    return varTable;
  }

  /**
   * Initializes the feature vector mapping by using the specified sample clauses.
   * @param samples the sample clauses.
   * @param pfield  the production field.
   */
  public void initFVecMap(List<Clause> samples, PField pfield) {
    fvecMap = new FVecMap(this, symTable, samples, pfield);
    numSyms = symTable.getNumSyms();
  }

  /**
   * Returns the feature vector mapping.
   * @return the feature vector mapping.
   */
  public FVecMap getFVecMap() {
    if (fvecMap == null || numSyms != symTable.getNumSyms())
      initFVecMap(null, null);
    return fvecMap;
  }

  /**
   * Adds consequence event listener to SOLAR.
   * @param l   a consequence event listener.
   */
  public void addSOLARListener(SOLARListener l) {
  	if (listeners == null)
  		listeners = new CopyOnWriteArrayList<SOLARListener>();
  	listeners.add(l);
  }

  /**
   * Returns SOLAR event listeners.
   * @return SOLAR event listeners.
   */
  public List<SOLARListener> getSOLARListers() {
  	return listeners;
  }

  /**
   * Initializes the weight and priority assignment of symbols.
   */
  public void initEqualityMapping(Options opt, List<Clause> clauses) {
    weightMap   = new WeightMap(this, opt, clauses);
    priorityMap = new PriorityMap(this, opt, clauses);
    termWeight1 = new TermWeight(weightMap, priorityMap);
    termWeight2 = new TermWeight(weightMap, priorityMap);

    if (dbg(DBG_REDUCTION_ORDER)) {
      System.out.println("[Reduction order]");
      System.out.println("<Constants>");
      for (Signature sig : symTable.getConstants())
        System.out.println(" " + sig.getName() + " : " + weightMap.get(sig.getID(), TermTypes.CONSTANT) + "w, " + priorityMap.get(sig.getID(), TermTypes.CONSTANT) + "p");
      System.out.println("<Functions>");
      for (Signature sig : symTable.getFunctions())
        System.out.println(" " + sig + " : " + weightMap.get(sig.getID(), TermTypes.FUNCTION) + "w, " + priorityMap.get(sig.getID(), TermTypes.FUNCTION) + "p");
    }
  }

  /**
   * Returns the symbol weight mapping.
   * @return the symbol weight mapping.
   */
  public WeightMap getWeightMap() {
    return weightMap;
  }

  /**
   * Returns the symbol priority mapping.
   * @return the symbol priority mapping.
   */
  public PriorityMap getPriorityMap() {
    return priorityMap;
  }

  /**
   * Returns the term weight object.
   * @return the term weight object.
   */
  public TermWeight getTermWeight1() {
    termWeight1.clear();
    return termWeight1;
  }

  /**
   * Returns the term weight object.
   * @return the term weight object.
   */
  public TermWeight getTermWeight2() {
    termWeight2.clear();
    return termWeight2;
  }

  /**
   * Returns the term queue for subsumption and unification checking.
   * @return the term queue for subsumption and unification checking.
   */
  public ArrayQueue<Term> getXTermQueue() {
    xTermQueue.clear();
    return xTermQueue;
  }

  /**
   * Returns the term queue for subsumption and unification checking.
   * @return the term queue for subsumption and unification checking.
   */
  public ArrayQueue<Term> getYTermQueue() {
    yTermQueue.clear();
    return yTermQueue;
  }

  /**
   * Returns the term queue for occur checking.
   * @return the term queue for occur checking.
   */
  public ArrayQueue<Term> getVTermQueue() {
    vTermQueue.clear();
    return vTermQueue;
  }

  /**
   * Returns the term queue for counting variables.
   * @return the term queue for counting variables.
   */
  public ArrayQueue<Term> getCTermQueue() {
    cTermQueue.clear();
    return cTermQueue;
  }

  /**
   * Returns the node queue for traversing nodes.
   * @return the node queue for traversing nodes.
   */
  public ArrayQueue<Node> getNodeQueue() {
    nodeQueue.clear();
    return nodeQueue;
  }

  /**
   * Returns the stack for retrieving a feature vector trie.
   * @return the stack for retrieving a feature vector trie.
   */
  public ArrayStack<FVecCand> getFVecCandStack() {
    fvecCandStack.clear();
    return fvecCandStack;
  }

  /**
   * Returns the stack for handling clause candidates.
   * @return the stack for handling clause candidates.
   */
  public ArrayStack<Clause.Cand> getClauseCandStack() {
    clauseCandStack.clear();
    return clauseCandStack;
  }

  /**
   * Returns the variable rename mapping.
   * @return the variable rename mapping.
   */
  public VarRenameMap getVarRenameMap() {
    varRenameMap.clear();
    return varRenameMap;
  }

  /**
   * Returns the variable negative rename mapping.
   * @return the variable negative rename mapping.
   */
  public NegVarRenameMap getNegVarRenameMap() {
    negVarRenameMap.clear();
    return negVarRenameMap;
  }

  /**
   * Returns the working substitution for local success caching.
   * @return the working substitution for local success caching.
   */
  public Subst getLSuccSubst() {
    lsuccSubst.clear();
    return lsuccSubst;
  }

  /**
   * Returns the working substitution for local failure caching.
   * @return the working substitution for local failure caching.
   */
  public Subst getLFailSubst() {
    lfailSubst.clear();
    return lfailSubst;
  }

  /**
   * Returns the working set of satisfied constraint list.
   * @return the working set of satisfied constraint list.
   */
  public ArrayList<Constraint> getSATConstraintList() {
    satConstraintList.clear();
    return satConstraintList;
  }

  /**
   * Returns the variable counter.
   * @return the variable counter.
   */
  public VarCounter getVarCounter() {
    varCounter.clear();
    return varCounter;
  }

  /**
   * Sets the time step.
   * @param time the time step.
   */
  public void setTimeStep(Counter time) {
    this.time = time;
  }

  /**
   * Returns the time step.
   * @return the time step.
   */
  public long getTimeStep() {
    if (time != null)
      return time.value();
    return 0;
  }

  /**
   * Turns on the specified debug flag.
   * @param type the debug flag.
   * @param on   true if outputs the specified debug information.
   */
  public void setDebug(int type, boolean on) {
    debug[type] = on;
    if (debug[DBG_ALL_INFO])
      for (int i=0; i < debug.length; i++)
        debug[i] = true;
  }

  /**
   * Returns true if the specific debug mode is on.
   * @param c a character represents a debug mode.
   * @return true if the specific debug mode is on.
   */
  public boolean dbg(int c) {
    return debug[c];
  }

  /**
   * Returns true if the specific debug mode is on now.
   * @param c a character represents a debug mode.
   * @return true if the specific debug mode is on now.
   */
  public boolean dbgNow(int c) {
    if (time == null)
      return debug[c];
    long t = time.value();
    return debug[c] && dbgStart <= t && t <= dbgEnd && t % dbgInterval == 0;
  }

  /**
   * Sets the period in which the system prints the debug information.
   * @param period the string that represents the period such like "xx-yy" or "xx-".
   */
  public void setDbgPeriod(String period)
  {
    period = period.trim();

    if (period.indexOf('-') == -1)
      dbgStart = dbgEnd = Long.parseLong(period);
    else {
      boolean beginning = true;
      StringTokenizer st = new StringTokenizer(period, "-", true);
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        if (token.equals("-")) {
          beginning = false;
          continue;
        }

        if (beginning)
          dbgStart = Long.parseLong(token);
        else
          dbgEnd = Long.parseLong(token);
      }
    }
  }

  /**
   * Sets the period in which the system prints the debug information.
   * @param period the string that represents the period such like "xx-yy" or "xx-".
   */
  public void setDbgPeriod(long from, long to) {
    this.dbgStart = from;
    this.dbgEnd   = to;
  }

  /**
   * Sets the interval for printing the debug information.
   * @param interval the string that represents the interval.
   */
  public void setDbgInterval(String interval)
  {
    dbgInterval = Long.parseLong(interval);
  }

  /**
   * Sets the interval for printing the debug information.
   * @param interval the interval for printing the debug information.
   */
  public void setDbgInterval(long interval)
  {
    dbgInterval = interval;
  }

  /** The statistics information. */
  private Stats stats = null;

  /** The variable table. */
  private VarTable varTable = null;

  /** The symbol table. */
  private SymTable symTable = null;
  /** The number of symbols in the symbol table. */
  private int numSyms = 0;

  /** The feature vector mapping. */
  private FVecMap fvecMap = null;

  /** The weight assignment of symbols. */
  private WeightMap weightMap = null;
  /** The priority assignment of symbols. */
  private PriorityMap priorityMap = null;
  /** The term weight object. */
  private TermWeight termWeight1 = null;
  /** The term weight object. */
  private TermWeight termWeight2 = null;

  /** Consequence event listeners. */
  private CopyOnWriteArrayList<SOLARListener> listeners = null;

  /** The term queue for subsumption and unification checking. */
  private ArrayQueue<Term> xTermQueue = new ArrayQueue<Term>();
  /** The term queue for subsumption and unification checking. */
  private ArrayQueue<Term> yTermQueue = new ArrayQueue<Term>();
  /** The term queue for occur checking. */
  private ArrayQueue<Term> vTermQueue = new ArrayQueue<Term>();
  /** The term queue for counting variables. */
  private ArrayQueue<Term> cTermQueue = new ArrayQueue<Term>();
  /** The node queue for traversing nodes. */
  private ArrayQueue<Node> nodeQueue = new ArrayQueue<Node>();
  /** The stack for retrieving a feature vector trie. */
  private ArrayStack<FVecCand> fvecCandStack = new ArrayStack<FVecCand>();
  /** The stack for handling clause candidates. */
  private ArrayStack<Clause.Cand> clauseCandStack = new ArrayStack<Clause.Cand>();
  /** The variable rename mapping. */
  private VarRenameMap varRenameMap = new VarRenameMap();
  /** The variable negative rename mapping. */
  private NegVarRenameMap negVarRenameMap = new NegVarRenameMap();
  /** The working substitution for local success caching. */
  private Subst lsuccSubst = new Subst();
  /** The working substitution for local failure caching. */
  private Subst lfailSubst = new Subst();
  /** The working set of satisfied constraint list. */
  private ArrayList<Constraint> satConstraintList = new ArrayList<Constraint>();
  /** The variable counter. */
  private VarCounter varCounter = new VarCounter();

  /** The time step. */
  private Counter time = null;
  /** The debug flags. */
  private boolean[] debug = null;
  /** The start time of the debugging period. */
  private long dbgStart = 0;
  /** The end time of the debugging period. */
  private long dbgEnd = Long.MAX_VALUE;
  /** The interval for printing the debugging period. */
  private long dbgInterval = 1;

}
