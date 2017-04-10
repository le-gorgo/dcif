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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nabelab.solar.indexing.FVec;
import org.nabelab.solar.indexing.FVecTrie;
import org.nabelab.solar.parser.ParseException;

/**
 * A set of consequences.
 * @author nabesima
 */
public class ConseqSet implements ClauseTypes, DebugTypes, Iterable<Conseq> {

  /**
   * Construct a empty set of consequences.
   * @param env  the environment.
   */
  public ConseqSet(Env env) {
    this.env = env;
    this.varTable = env.getVarTable();
    this.fvecTrie = new FVecTrie(env, true);
  }

  /**
   * Adds the clause to this database.
   * @param clause the clause to add.
   * @return true if the clause is added.
   */
  public boolean add(Clause clause) {
  	return add(clause, null);
  }

  /**
   * Adds the clause to this database.
   * @param clause the clause to add.
   * @return true if the clause is added.
   */
  public boolean add(Clause clause, List<SOLARListener> listeners) {

    int vars = clause.getNumVars();
    if (varTable.getNumVars() < vars)
      varTable.addVars(vars);
    else
      vars = 0;
    //GROUPcHECKING
    
    // Forward subsumption checking.
    Clause subsuming = fvecTrie.findSubsuming(clause.getFVec(false), clause);
    if (subsuming != null) {
      if (env.dbg(DBG_CONSQ_DETAIL)) {
        System.out.println(env.getTimeStep() + ": " + clause);
        System.out.println(" but subsumed by " + subsuming.toSimpString());
      }
      varTable.removeVars(vars);
      return false;
    }

    if (env.dbg(DBG_CONSQ) || env.dbg(DBG_CONSQ_DETAIL)) {
      Clause c = clause.instantiate();
      c.rename();
      System.out.println(env.getTimeStep() + ": " + c.toSimpString());
    }

    // Backward subsumption checking.
    List<Clause> subsumed = fvecTrie.removeSubsumed(clause.getFVec(false), clause);
    for (Clause c : subsumed) {
      if (c.getType() == CONSEQUENCE) {
        numConseqs--;
        numConseqLits -= c.size();
      }
      if (env.dbg(DBG_CONSQ_DETAIL))
        System.out.println(" removed: " + c);
    }

    // Adds the clause to the database.
    if (clause instanceof Conseq)
      clause.rename();
    fvecTrie.add(clause.getFVec(false), clause);
    if (clause.size() == 0)
      hasEmptyClause = true;
    if (clause.getType() == CONSEQUENCE) {
      numConseqs++;
      numConseqLits += clause.size();
    }
    if (listeners != null)
    	for (SOLARListener listener : listeners)
    		listener.conseqFound(new SOLAREvent(this, SOLAREvent.FOUND, clause, subsumed));

    varTable.removeVars(vars);
    return true;
  }

  /**
   * Adds a set of clauses to this database.
   * @param clauses  a set of clauses to be added.
   */
  public void addAll(ConseqSet conseqs) {
    for (Clause c : conseqs)
      add(c);
  }

  /**
   * Removes the clause to this database.
   * @param clause the clause to be removed.
     */
  public void remove(Clause clause) {

    int vars = clause.getNumVars();
    if (varTable.getNumVars() < vars)
      varTable.addVars(vars);
    else
      vars = 0;

    // Backward subsumption checking.
    List<Clause> subsumed = fvecTrie.removeSubsumed(clause.getFVec(false), clause);
    for (Clause c : subsumed) {
      if (c.getType() == CONSEQUENCE) {
        numConseqs--;
        numConseqLits -= c.size();
      }
      if (env.dbg(DBG_CONSQ_DETAIL))
        System.out.println(" removed: " + c + " by " + clause);
    }

    varTable.removeVars(vars);
  }

  /**
   * Returns a subsuming clause if the specified clause is subsumed by the clause.
   * @param fvec   the feature vector of the clause.
   * @param clause the clause to be checked.
   * @return the subsuming clause.
   */
  public Clause findSubsuming(FVec fvec, Clause clause) {
    return fvecTrie.findSubsuming(fvec, clause);
  }

  /**
   * Returns the set of consequences.
   * @return the set of consequences.
   */
  public List<Conseq> get() {
    ArrayList<Conseq> list = new ArrayList<Conseq>();
    for (Clause clause : fvecTrie.getClauses())
      //if (clause instanceof Conseq)
      if (clause.getType() == CONSEQUENCE)
        list.add((Conseq)clause);
    return list;
  }

  /**
   * Returns an iterator over the elements in this set of consequences.
   * @return an iterator over the elements in this set of consequences.
   */
  public Iterator<Conseq> iterator() {
    return get().iterator();
  }

  /**
   * Returns true if there is an empty consequence.
   * @return true if there is an empty consequence.
   */
  public boolean hasEmptyClause() {
    return hasEmptyClause;
  }

  /**
   * Returns true if this set contains no elements.
   * @return true if this set contains no elements.
   */
  public boolean isEmpty() {
    return numConseqs == 0;
  }

  /**
   * Returns the number of found consequences.
   * @return the number of found consequences.
   */
  public int size() {
    return numConseqs;
  }

  /**
   * Returns true if found consequences are valid.
   * @return true if found consequences are valid.
 * @throws Exception 
   */
  public boolean validate() throws Exception {
    boolean valid = true;
    for (Clause clause : fvecTrie.getClauses()) {
      if (clause instanceof Conseq) {
        Conseq conseq = (Conseq)clause;
        valid &= conseq.getProof().validate();
      }
    }
    return valid;
  }

  /**
   * Returns the set of instantiated clauses in this set.
   * @return the set of instantiated clauses in this set.
   */
  public ConseqSet instantiate() {
    ConseqSet newConseqSet = new ConseqSet(env);
    for (Clause c : fvecTrie.getClauses())
      newConseqSet.add(c.instantiate());
    return newConseqSet;
  }

  /**
   * Returns the number of literals in this set.
   * @return the number of literals in this set.
   */
  public int getNumLiterals() {
    return numConseqLits;
  }

  /**
   * Returns the number of subsumption-checking.
   * @return the number of subsumption-checking.
   */
  public long getNumSubsumChecks() {
    return fvecTrie.getNumSubsumChecks();
  }

  /**
   * Returns the number of subsumption-checking without filtering.
   * @return the number of subsumption-checking without filtering.
   */
  public long getNumSubsumChecksWithoutFiltering() {
    return fvecTrie.getNumSubsumChecksWithoutFiltering();
  }

  /**
   * Outputs the consequences to the specified file.
   * @param fileName  the file name.
   */
  public void output(String fileName)  {
    try {
      output(new PrintStream(new FileOutputStream(fileName)));
    } catch (IOException e) {
      System.out.println("Error: Cannot output the found consequences to the file (" + fileName + ").");
    }
  }

  /**
   * Outputs the consequences to the specified file.
   * @param out the print stream.
   */
  public void output(PrintStream out) {
    for (Conseq conseq : get())
      conseq.output(out);
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object.
   */
  public String toString() {
    return fvecTrie.getClauses().toString();
  }

  /** The environment. */
  private Env env = null;
  /** The set of consequences. */
  private FVecTrie fvecTrie = null;
  /** The variable table. */
  private VarTable varTable = null;
  /** The number of consequences. */
  private int numConseqs = 0;
  /** The number of literals in consequences. */
  private int numConseqLits = 0;
  /** Whether an empty consequence is found or not. */
  private boolean hasEmptyClause = false;
}
