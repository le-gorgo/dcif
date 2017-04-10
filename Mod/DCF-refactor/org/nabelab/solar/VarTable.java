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

import org.nabelab.solar.util.IntStack;

/**
 * @author nabesima
 *
 */
public class VarTable implements TermTypes {
  
  /**
   * Constructs a variable table.
   * @param cfp the consequence finding problem to which this belongs.
   */
  public VarTable(Env env) {
    this.env = env;
    pvars = new Term[INITIAL_VARS];
    pvals = new Term[INITIAL_VARS];
    nvars = new Term[INITIAL_VARS];
    nvals = new Term[INITIAL_VARS];
    substitutions = new IntStack(INITIAL_VARS);
    for (int i=0; i < pvars.length; i++) { 
      pvars[i] = Term.createVar(env, +i);
      nvars[i] = Term.createVar(env, -i);
    }    
  }
  
  /**
   * Returns the value of the specified variable.
   * @param var the specified variable. 
   * @return the value of the variable.
   */
  public Term getValue(int var) {
    return (var >= 0) ? pvals[+var] : nvals(-var);
  }
  
  /**
   * Returns the last variable of the variable chain that starts from the specified variable.
   * @param var the specified variable.
   * @return the last variable of the variable chain.
   */
  public Term getTailVar(int var) {
    Term val = null;
    while ((val = (var >= 0) ? pvals[var] : nvals(-var)) != null) {
      if (val.getStartType() != VARIABLE) 
        break;
      int name = val.getStartName(); 
      if (name >= 0)
        var = name + val.getOffset();
      else
        var = name - val.getOffset();
    }
    return (var >= 0) ? pvars[var] : nvars[-var];
  }
  
  /**
   * Returns the last value of the variable chain that starts from the specified variable.
   * @param var the specified variable.
   * @return the last value of the variable chain.
   */
  public Term getTailValue(int var) {
    Term val = null;
    while ((val = (var >= 0) ? pvals[var] : nvals(-var)) != null) {
      if (val.getStartType() != VARIABLE) 
        break;
      int name = val.getStartName(); 
      if (name >= 0)
        var = name + val.getOffset();
      else
        var = name - val.getOffset();
    }
    return val;
  }

  /**
   * Substitutes the specified value for the specified variable.
   * @param var the specified variable.
   * @param val the specified value.
   */
  public void substitute(int var, Term val) {
    assert(((var >= 0) ? pvals[var] : nvals(-var)) == null);
    if (var >= 0)
      pvals[+var] = val;
    else
      nvals(-var, val);
    substitutions.push(var);
  }
  
  /**
   * Applies the specified substitution.
   * @param g the specified substitution.
   */
  public void substitute(Subst g) {
    for (int i=0; i < g.size(); i++) {
      int var = g.getVar(i);
      assert(((var >= 0) ? pvals[var] : nvals(-var)) == null);
      if (var >= 0)
        pvals[var] = g.getVal(i);
      else
        nvals(-var, g.getVal(i));
      substitutions.push(var);
      
    }
  }
  
  /**
   * Returns the variable at the specified substitution state.
   * @param state the specified state.
   * @return the variable at the specified substitution state.
   */
  public int getSubstitutedVar(int state) {
    return substitutions.get(state);
  }
  
  /**
   * Returns the state of this variable table.
   * @return the state of this variable table.
   */
  public int state() {
    return substitutions.size();
  }
  
  /**
   * Backtracks to the specified state.
   * @param to the specified state.
   */
  public void backtrackTo(int to) {
    assert(to <= substitutions.size());
    while (to < substitutions.size()) { 
      int var = substitutions.pop();
      if (var >= 0)
        pvals[var] = null;
      else
        nvals(-var, null);
    }
  }
  
  /**
   * Adds the specified number of variables.
   * @param num the number of variables to add.
   */
  public void addVars(int num) {
    numVars += num;
    if (pvars.length < numVars) {
      int oldSize = pvars.length;
      int newSize = numVars << 1;
      // MEMO for J2SE1.6
      //vars = Arrays.copyOf(vars, newSize);
      //vals = Arrays.copyOf(vals, newSize);
      Term[] oldVars = pvars;
      Term[] oldVals = pvals;
      pvars = new Term[newSize];
      pvals = new Term[newSize];
      System.arraycopy(oldVars, 0, pvars, 0, oldVars.length);
      System.arraycopy(oldVals, 0, pvals, 0, oldVals.length);
      for (int i=oldSize; i < newSize; i++)
        pvars[i] = Term.createVar(env, i);
    }    
    // Debug
//    for (int i=numVars - num; i < numVars; i++)
//        assert(pvals[i] == null);
  }

  /**
   * Removes the specified number of variables.
   * @param num the number of variables to remove.
   */
  public void removeVars(int num) {
    numVars -= num;
    // Debug
//    for (int i=numVars + num - 1; i >= numVars; i--)
//      assert(pvals[i] == null);
  }
  
  /**
   * Removes the all variables.
   * @param the all variables.
   */
  public void removeAllVars() {
    numVars = 0;
  }

  /**
   * Returns the number of variables.
   * @return the number of variables.
   */
  public int getNumVars() {
    return numVars;
  }
  
  /**  
   * Returns the value of the specified negative variable.
   * @param nvar the specified negative variable.
   * @return the value of the specified negative variable.
   */
  private Term nvals(int nvar) {
    assert(0 < nvar);
    if (nvars.length < nvar) {
      int oldSize = nvars.length;
      int newSize = (nvar) << 1;
      // MEMO for J2SE1.6
      //vars = Arrays.copyOf(vars, newSize);
      //vals = Arrays.copyOf(vals, newSize);
      Term[] oldVars = nvars;
      Term[] oldVals = nvals;
      nvars = new Term[newSize];
      nvals = new Term[newSize];
      System.arraycopy(oldVars, 0, nvars, 0, oldVars.length);
      System.arraycopy(oldVals, 0, nvals, 0, oldVals.length);
      for (int i=oldSize; i < newSize; i++)
        nvars[i] = Term.createVar(env, -i);
    }
    return nvals[nvar];
  }
  
  /**  
   * Sets the value of the specified negative variable.
   * @param nvar the specified negative variable.
   * @param nval the value of the specified negative variable.
   */
  private void nvals(int nvar, Term nval) {
    assert(0 < nvar);
    if (nvars.length < nvar) {
      int oldSize = nvars.length;
      int newSize = (nvar) << 1;
      // MEMO for J2SE1.6
      //vars = Arrays.copyOf(vars, newSize);
      //vals = Arrays.copyOf(vals, newSize);
      Term[] oldVars = nvars;
      Term[] oldVals = nvals;
      nvars = new Term[newSize];
      nvals = new Term[newSize];
      System.arraycopy(oldVars, 0, nvars, 0, oldVars.length);
      System.arraycopy(oldVals, 0, nvals, 0, oldVals.length);
      for (int i=oldSize; i < newSize; i++)
        nvars[i] = Term.createVar(env, -i);
    }
     nvals[nvar] = nval;
  }
  
  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();
    
    for (int i=0; i < numVars; i++) 
      if (pvals[i] != null) 
        str.append("_" + i + " : " + pvals[i].toSimpString() + "\n");
      else
        str.append("_" + i + " : " + pvals[i] + "\n");    
    for (int i=0; i < nvals.length; i++) 
      if (nvals[i] != null) 
        str.append("_-" + i + " : " + nvals[i].toSimpString() + "\n");
    
    return str.toString();
  }
  
  /** The environment. */ 
  private Env env = null;
  /** Variables. */
  private Term[] pvars = null;
  /** Values. */
  private Term[] pvals = null;
  /** The number of variables. */
  private int numVars = 0;
  /** The stack of applied substitutions. */
  private IntStack substitutions = null;

  /** Negative indexed variables. */
  private Term[] nvars = null;
  /** Values of Negative indexed variables. */
  private Term[] nvals = null;
  
  
  /** The initial number of variables */
  private final static int INITIAL_VARS = 1024;

}
