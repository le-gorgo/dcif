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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import org.nabelab.solar.indexing.FVec;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.parser.Parser;
import org.nabelab.util.IntSet;

public class Literal implements OptionTypes, DebugTypes {

  /**
   * Constructs a literal.
   * @param env      the environment.
   * @param positive the sign of this literal.
   * @param term     the predicate.
   */
  public Literal(Env env, boolean positive, Term term) {
    this.env = env;
    this.positive = positive;
    this.term = term;
  }

  /**
   * Constructs a literal.
   * @param env      the environment.
   * @param hasNAF   true if this literal has NAF.
   * @param positive the sign of this literal.
   * @param term     the predicate.
   */
  public Literal(Env env, boolean hasNAF, boolean positive, Term term) {
    this.env = env;
    this.hasNAF = hasNAF;
    this.positive = positive;
    this.term = term;
  }

  /**
   * Constructs a copy of literal.
   * @param literal the literal to be copied.
   */
  public Literal(Literal literal)
  {
    this.env      = literal.env;
    this.hasNAF   = literal.hasNAF;
    this.positive = literal.positive;
    this.term     = new Term(literal.term);
    this.numSyms  = literal.numSyms;
    this.numVars  = literal.numVars;
    this.numExts  = literal.numExts;
    this.redOrder = literal.redOrder;
  }

  /**
   * Constructs a copy of literal.
   * @param env     a new environment.
   * @param literal the literal to be copied.
   */
  public Literal(Env env, Literal literal)
  {
    this.env      = env;
    this.hasNAF   = literal.hasNAF;
    this.positive = literal.positive;
    this.term     = new Term(env, literal.term);
    this.numSyms  = literal.numSyms;
    this.numVars  = literal.numVars;
    this.numExts  = literal.numExts;
    this.redOrder = literal.redOrder;
  }

  /**
   * Constructs a new literal from the specified literal and offset.
   * @param literal the specified literal.
   * @param offset  the variable offset.
   * @return the new literal.
   */
  public static Literal newOffset(Literal literal, int offset) {
    Literal newLit = new Literal(literal.env, literal.hasNAF, literal.positive, Term.newOffset(literal.term, offset));
    newLit.numVars  = literal.numVars;
    newLit.numSyms  = literal.numSyms;
    newLit.numExts  = literal.numExts;
    newLit.redOrder = literal.redOrder;
    return newLit;
  }

  /**
   * Construct an empty node for identification.
   */
  private Literal() {
  }

  /**
   * Constructs a literal from the string.
   * @param env     the environment.
   * @param opt     the options.
   * @param literal the string representation of a literal (ex. "-p(a)")
   */
  public static Literal parse(Env env, Options opt, String literal) throws ParseException {
    return new Parser(env, opt).literal(new BufferedReader(new StringReader(literal)));
  }

  /**
   * Returns the new literal in which all variables are replaced with the values.
   * @return the new literal in which all variables are replaced with the values.
   */
  public Literal instantiate() {
    Literal newLit = new Literal(env, hasNAF, positive, term.instantiate());
    newLit.redOrder = redOrder;
    return newLit;
  }

  /**
   * Replaces the specified old term with the new term.
   * @param oldTerm the old term to be replaced.
   * @param newTerm the new term to replace.
   * @return the new literal in which old term are replaced with the new term.
   */
  public Literal replace(Term oldTerm, Term newTerm) {
    return new Literal(env, hasNAF, positive, term.replace(oldTerm, newTerm));
  }

  /**
   * Replaces all the specified old terms with the new term.
   * @param oldTerm the old term to be replaced.
   * @param newTerm the new term to replace.
   * @return the new literal in which all old terms are replaced with the new term.
   */
  public Literal replaceAll(Term oldTerm, Term newTerm) {
    return new Literal(env, hasNAF, positive, term.replaceAll(oldTerm, newTerm));
  }

  /**
   * Renames the variables. No variable in this clause must have a value.
   */
  public void rename() {
    rename(env.getVarRenameMap());
  }

  /**
   * Renames the variables using the specified rename-mapping. No variable in this clause must have a value.
   * @param renameMap the rename-mapping.
   */
  public void rename(VarRenameMap renameMap) {
    term.rename(renameMap);
  }

  /**
   * Renames the variables using the specified rename-mapping. No variable in this clause must have a value.
   * @param renameMap  the rename-mapping.
   * @param minVarName the smallest variable name to be renamed.
   * @param minVarName the largest variable name to be renamed.
   */
  public void subrename(VarRenameMap renameMap, int minVarName, int maxVarName) {
    term.subrename(renameMap, minVarName, maxVarName);
  }

  /**
   * Toggles the sign of this literal.
   */
  public void negate() {
    positive = !positive;
  }

  /**
   * Returns a non-variable argument of a function or predicate if exits.
   * @param predicate If true, then returns a non-variable argument of a predicate.
   * @return a non-variable argument of a function or predicate if exits.
   */
  public Term getNonVarArg(boolean predicate) {
    return term.getNonVarArg(predicate);
  }

  /**
   * Unifies this literal and the specified literal. If unifiable, then return the mgu.
   * @param y the specified literal.
   * @return the mgu if unifiable.
   */
  public Subst unify(Literal y) {
    if (positive == y.positive)
      return term.unify(y.term);
    return null;
  }

  /**
  /**
   * Unifies this literal and the complement of the specified literal. If unifiable, then return the mgu.
   * @param y the specified literal.
   * @return the mgu if unifiable.
   */
  public Subst compUnify(Literal y) {
    if (positive == !y.positive)
      return term.unify(y.term);
    return null;
  }

  /**
   * Returns the mgu if this literal is unifiable with the specified literal.
   * @param y the specified literal.
   * @return the mgu if this literal is unifiable with the specified literal.
   */
  public Subst isUnifiable(Literal y) {
    if (positive == y.positive)
      return term.isUnifiable(y.term);
    return null;
  }

  /**
   * Returns the mgu if this literal is unifiable with the complement of the specified literal.
   * @param y the specified literal.
   * @return the mgu if this literal is unifiable with the complement of the specified literal.
   */
  public Subst isCompUnifiable(Literal y) {
    if (positive != y.positive)
      return term.isUnifiable(y.term);
    return null;
  }

  /**
   * Returns the substitution if this term is able to subsume with the specified term.
   * @param y the specified term.
   * @return the substitution if this term is unifiable with the specified term.
   */
  public Subst isSubsuming(Literal y) {
    if (positive == y.positive)
      return term.isSubsuming(y.term);
    return null;
  }

  /**
   * Returns a substitution if this literal subsumes the specified literal.
   * @param y the specified literal.
   * @return the substitution if this literal subsumes the specified literal.
   */
  public Subst subsumes(Literal y) {
    if (positive == y.positive)
      return term.subsumes(y.term);
    return null;
  }

  /**
   * Returns a substitution if this literal subsumes the specified literal.
   * @param y the specified literal.
   * @param yclause the clause contains the literal y.
   * @return the substitution if this literal subsumes the specified literal.
   */
  public Subst subsumes(Literal y, Clause yclause) {
    if (positive == y.positive)
      return term.subsumes(y.term, yclause);
    return null;
  }

  /**
   * Returns the name of this literal.
   * @return the name of this literal.
   */
  public int getName() {
    return term.getStartName();
  }

  /**
   * Returns true if this literal is positive.
   * @return true if this literal is positive.
   */
  public boolean getSign() {
    return positive;
  }

  /**
   * Returns the argument at the specified position.
   * @param pos the position of the argument (the first argument is 0).
   * @return the argument at the specified position.
   */
  public Term getArg(int pos) {
    return term.getArg(pos);
  }

  /**
   * Returns the arity at this literal.
   * @return the arity at this literal.
   */
  public int getArity() {
    return term.getArity();
  }

  /**
   * Returns true if this is positive.
   * @return true if this is positive.
   */
  public boolean isPositive() {
    return positive;
  }

  /**
   * Returns true if this is negative.
   * @return true if this is negative.
   */
  public boolean isNegative() {
    return !positive;
  }

  /**
   * Returns true if this literal is ground.
   * @return true if this literal is ground.
   */
  public boolean isGround() {
    return term.isGround();
  }

  /**
   * Returns true if this literal is maximally general, i.e., all arguments of this literal are variables.
   * @return true if this literal is maximally general.
   */
  public boolean isMaxGeneral() {
    return term.isMaxGeneral();
  }

  /**
   * Sets the variable offset of this literal.
   * @param offset the variable offset.
   */
  protected void setOffset(int offset) {
    term.setOffset(offset);
  }

  /**
   * Returns the term of this literal.
   * @return the term of this literal.
   */
  public Term getTerm() {
    return term;
  }

  /**
   * Updates the specified feature vector.
   * @param deep if true, then look up values of variables in this literal.
   * @param fvec the specified feature vector.
   */
  public void getFVec(boolean deep, FVec fvec) {
    fvec.inc(positive);
    term.getFVec(deep, fvec, positive);
  }

  /**
   * Returns the raw feature vector of this literal.
   * @param raw the raw feature vector of this literal (output).
   */
  public void getRawFVec(int[] raw) {
    raw[env.getFVecMap().getRawIdx(positive)]++;
    term.getRawFVec(raw, positive);
  }

  /**
   * Returns the number of kinds of variables contained in this literal.
   * @param update if true, then recalculates the number of variables.
   * @return the number of kinds of variables contained in this term.
   */
  public int getNumVars() {
    if (numVars == -1)
      numVars = term.getNumVars();
    return numVars;
  }

  /**
   * Returns true if this literal contains the specified sub-term.
   * @param subTerm the sub-term to be checked.
   * @return true if this literal contains the specified sub-term.
   */
  public boolean contains(Term subTerm) {
    return term.findSubTerms(subTerm) != null;
  }

  /**
   * Counts the number of kinds of variables.
   * @param varCounter the variable counter.
   */
  public void countVars(VarCounter varCounter) {
    term.countVars(varCounter);
  }

  /**
   * Counts the number of kinds of variables.
   * @param varCounter the variable counter.
   */
  public void countVars(IntSet varCounter) {
    term.countVars(varCounter);
  }

  /**
   * Returns true if this literal has binded variables.
   * @return true if this literal has binded variables.
   */
  public boolean hasBindedVars() {
    return term.hasBindedVars();
  }

  /**
   * Return the number of symbols contained in this literal.
   * @param update if true, then recalculates the number contained in symbols.
   * @return the number of symbols contained in this literal.
   */
  public int getNumSyms(boolean update) {
    if (numSyms == -1 || update)
      // TODO: performance test for b219
      //numSyms = term.getNumSyms(update);
      numSyms = term.size(update);
    return numSyms;
  }

  /**
   * Return the number of symbols contained in this literal.
   * @return the number of symbols contained in this literal.
   */
  public int getNumSyms() {
    assert(numSyms != -1);
    return numSyms;
  }

  /**
   * Sets the number of extendable clauses  of this literal.
   * @param num  the number of extendable clauses  of this literal.
   */
  public void setNumExts(int num) {
    numExts = num;
  }

  /**
   * Returns the number of extendable clauses of this literal.
   * @param update   if true, then recalculates the number of extendable clauses with literals in this clause.
   * @param clauseDB the input clause database.
   * @return the number of extendable clauses of this literal.
   */
  public int getNumExts(boolean update, ClauseDB clauseDB) {
    if (numExts == -1 || update) {

      env.getVarTable().addVars(getNumVars());
      List<Unifiable<PClause>> exts = clauseDB.getCompUnifiable(this);
      numExts = (exts != null) ? exts.size() : 0;
      env.getVarTable().removeVars(getNumVars());

//      // TODO: performance test for b219 (diff)
//      if (clauseDB.getCalcType() == CALC_ME || isNegative() || hasTag(TermTypes.NO_RESTART)) {
//        env.getVarTable().addVars(getNumVars());
//        List<Unifiable<PClause>> exts = clauseDB.getCompUnifiable(this);
//        numExts = (exts != null) ? exts.size() : 0;
//        env.getVarTable().removeVars(getNumVars());
//      }
//      else {
//        //numExts = clauseDB.getNumNegClauses();
//        // The following code is b234
////        numExts = 0;
////        for (Clause c : clauseDB.getNegClauses())
////          numExts += c.get(0).getNumExts(update, clauseDB);
//        // The follwing code is b236
//        numExts = Integer.MAX_VALUE;
//      }
    }
    return numExts;
  }

  /**
   * Returns the number of extendable clauses of this literal.
   * @return the number of extendable clauses of this literal.
   */
  public int getNumExts() {
    assert(numExts != -1);
    return numExts;
  }

//  /**
//   * Sets the (maximum) number of extendable clauses with this literal.
//   * @param num the (maximum) number of extendable clauses with this literal.
//   */
//  public void setNumExts(int num){
//    numExts = num;
//  }

  /**
   * Returns the reduction order.
   * @returns the value which represents the literal order between literals.
   */
  public int getReductionOrder() {
    assert(redOrder != -1);
    return redOrder;
  }

  /**
   * Sets the reduction order.
   * @param order the value which represents the literal order between literals.
   */
  public void setReductionOrder(int order) {
    redOrder = order;
  }

  /**
   * Returns true if this literal is the special top predicate.
   * @return true if this literal is the special top predicate.
   */
  public boolean isTopPred() {
    return term.isTopPred();
  }

  /**
   * Returns true if this literal is an equality predicate.
   * @return true if this literal is an equality predicate.
   */
  public boolean isEqualPred() {
    return term.isEqualPred();
  }

  /**
   * Returns true if this literal is a positive equality predicate.
   * @return true if this literal is a positive equality predicate.
   */
  public boolean isPosEqualPred() {
    return positive && term.isEqualPred();
  }

  /**
   * Returns true if this literal is a negative equality predicate.
   * @return true if this literal is a negative equality predicate.
   */
  public boolean isNegEqualPred() {
    return !positive && term.isEqualPred();
  }

  /**
   * Returns true if this literal is a connector predicate.
   * @return true if this literal is a connector predicate.
   */
  public boolean isConnPred() {
    return term.isConnPred();
  }

  /**
   * Returns true if this literal is a connector predicate at the source side.
   * @return true if this literal is a connector predicate at the source side.
   */
  public boolean isSrcConnPred() {
    if (isPositive() && term.isPosConnPred())
      return true;
    if (isNegative() && term.isNegConnPred())
      return true;
    return false;
  }

  /**
   * Returns true if this literal is a connector predicate at the destination side.
   * @return true if this literal is a connector predicate at the destination side.
   */
  public boolean isDstConnPred() {
    return isNegative() && term.isConnPred();
  }

  /**
   * Returns true if this literal has NAF.
   * @return true if this literal has NAF.
   */
  public boolean hasNAF() {
    return hasNAF;
  }

  /**
   * Returns true if this literal has the specified tag.
   * @return true if this literal has the specified tag.
   */
  public boolean hasTag(int tag) {
    return term.hasTag(tag);
  }

 /**
   * Returns the hash code value of this object.
   * @return the hash code value of this object.
   */
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (positive ? 1231 : 1237);
    result = prime * result + ((term == null) ? 0 : term.hashCode());
    return result;
  }

  /**
   * Returns true if the literal x is equals to the literal y.
   * @param x    the literal.
   * @param xcur the start position of x.
   * @param y    the literal.
   * @param ycur the start position of y.
   * @return true if the literal x is equals to the literal y.
   */
  public static boolean equals(Literal x, Literal y) {
    if (x.positive == y.positive)
      return Term.equals(x.term, y.term);
    return false;
  }

  /**
   * Compares the specified object with this object for equality.
   * @param obj the reference object with which to compare.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Literal other = (Literal) obj;
    if (positive != other.positive)
      return false;
    if (term == null) {
      if (other.term != null)
        return false;
    } else if (!term.equals(other.term))
      return false;
    return true;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toSimpString() {
    return toSimpString(term.getOffset());
  }

  /**
   * Returns a string representation of this object.
   * @param offset a variable offset.
   * @return a string representation of this object.
   */
  public String toSimpString(int offset) {
    StringBuilder str = new StringBuilder();
    if (hasNAF)
      str.append("\\+ ");
    if (positive)
      str.append('+').append(term.toSimpString(offset));
    else
      str.append('-').append(term.toSimpString(offset));
    if (env.dbg(DBG_LIT_ORDER))
      str.append("<" + numSyms + "s/" + numExts + "e>");
    return str.toString();
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();
    if (hasNAF)
      str.append("\\+ ");
    if (positive)
      str.append('+').append(term.toString());
    else
      str.append('-').append(term.toString());
    if (env.dbg(DBG_LIT_ORDER))
      str.append("<" + numSyms + "s/" + numExts + "e>");
    return str.toString();
  }

  /** The environment. */
  private Env env = null;
  /** Whether this literal has NAF. */
  private boolean hasNAF = false;
  /** The sign of this literal. */
  private boolean positive = true;
  /** The predicate. */
  private Term term = null;
  /** The number of symbols contained in this literal. */
  private int numSyms = -1;
  /** The number of variables contained in this literal. */
  private int numVars = -1;
  /** The (maximum) number of extendable clauses with this literal. */
  private int numExts = -1;
  /** The reduction ordering. */
  private int redOrder = -1;

  /** The special literal that represents restarting the computation is required. */
  public final static Literal restart = new Literal();

public void setTerm(Term term2) {
	replace(term, term2);	
}
}
