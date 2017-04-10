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
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Stack;

import org.nabelab.solar.equality.PriorityMap;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.equality.WeightMap;
import org.nabelab.solar.indexing.FVec;
import org.nabelab.solar.indexing.FVecMap;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.parser.Parser;
import org.nabelab.solar.util.ArrayQueue;
import org.nabelab.util.IntSet;

/**
 * @author nabesima
 *
 */
public class Term implements VarHolder, TermTypes {

  /**
   * Constructs a term.
   * @param env    the environment.
   * @param name   the name sequence of the term.
   * @param type   the type sequence of the term.
   * @param next   the next term indexes of the term.
   * @param start  the start index of the term.
   * @param offset the variable offset of the term.
   */
  public Term(Env env, int[] name, int[] type, int[] next, int start, int offset) {
	  this.env    = env;
	  this.name   = name;
	  this.type   = type;
	  this.next   = next;
	  this.start  = start;
	  this.offset = offset;
	  
  }

  public int getDepth() {
	  int n = -1;
	  int k = 0;
	  int d = 0;
	  for (int i = 0; i<this.type.length; i++){
		  if(this.type[i]==TermTypes.FUNCTION){
			  if (n == this.name[i])
				  k++;
			  else{
				  n=this.name[i];
				  if (d < k)
					  d = k;
				  k = 1;
			  }  
		  }
		  else {
			  n = -1; 
			  if (d < k)
				  d = k;
			  k = 0;
		  }
			 
	  }
	  return d;
}

/**
   * Constructs a copy of the specified term.
   * @param term the term.
   */
  public Term(Term term)
  {
    this.env    = term.env;
    this.name   = term.name.clone();
    this.type   = term.type.clone();
    this.next   = term.next.clone();
    this.start  = term.start;
    this.offset = term.offset;
  }

  /**
   * Constructs a copy of the specified term.
   * @param env   a new environment.
   * @param term  the term.
   */
  public Term(Env env, Term term)
  {
    this.env    = env;
    this.name   = new int[term.name.length];
    this.type   = term.type.clone();
    this.next   = term.next.clone();
    this.start  = term.start;
    this.offset = term.offset;

    // Map names from the old environment to the new environment.
    SymTable oldSymTable = term.env.getSymTable();
    SymTable newSymTable = env.getSymTable();
    for (int i=0; i < name.length; i++) {
    	int oldname  = term.name[i];
    	int oldtype  = term.type[i];
    	int oldarity = oldSymTable.getArity(oldname, oldtype);
    	int newname  = 0;
    	switch (oldtype) {
    	case VARIABLE:   newname = oldname; break;
    	case CONSTANT:   newname = newSymTable.putConstant(oldSymTable.get(oldname, oldtype)); break;
    	case INTEGER:    newname = oldname; break;
    	case FUNCTION:   newname = newSymTable.putFunction(oldSymTable.get(oldname,  oldtype), oldarity); break;
    	case PREDICATE:  newname = newSymTable.putPredicate(oldSymTable.get(oldname, oldtype), oldarity); break;
    	}
    	name[i] = newname;
    }
  }

  /**
   * Constructs a term.
   * @param env   the environment.
   * @param name  the name sequence of the term.
   * @param type  the type sequence of the term.
   * @param next  the next term indexes of the term.
   * @param start the start index of the term.
   */
  public Term(Env env, int[] name, int[] type, int[] next, int start) {
    this(env, name, type, next, start, 0);
  }

  /**
   * Constructs a term.
   * @param env  the environment.
   * @param name the name sequence of the term.
   * @param type the type sequence of the term.
   * @param next the next term indexes of the term.
   */
  public Term(Env env, int[] name, int[] type, int[] next) {
    this(env, name, type, next, 0, 0);
  }

  /**
   * Constructs a term from the string.
   * @param env  the environment.
   * @param opt  the options.
   * @param term the string representation of a term (ex. "p(a)")
   */
  public static Term parse(Env env, Options opt, String term) throws ParseException {
    return new Parser(env, opt).term(new BufferedReader(new StringReader(term)));

  }

  /**
   * Constructs a new sub term from the specified term.
   * @param term  the specified term.
   * @param start the start position.
   * @return the new sub term.
   */
  public static Term newSubTerm(Term term, int start) {
    return new Term(term.env, term.name, term.type, term.next, start, term.offset);
  }

  /**
   * Constructs a new term from the specified term and offset.
   * @param term   the specified term.
   * @param offset the variable offset.
   * @return the new term.
   */
  public static Term newOffset(Term term, int offset) {
    return new Term(term.env, term.name, term.type, term.next, term.start, offset);
  }

  /**
   * Returns the new term in which all variables are replaced with the values.
   * @return the new term in which all variables are replaced with the values.
   */
  public Term instantiate() {

    int size = (next[start] - start) << 1;
    int[] newName = new int[size];
    int[] newType = new int[size];
    int[] newNext = new int[size];

    boolean extended = false;

    VarTable varTable = env.getVarTable();
    SymTable symTable = env.getSymTable();
    TermCont termCont = null;
    Term term = this;
    int  cur  = start;
    int  end  = term.next[cur];
    int  pos  = 0;
    while (true) {
      while (cur != end) {
        if (size == pos) {
          size <<= 1;
          // MEMO for J2SE1.6
          //newName = Arrays.copyOf(newName, size);
          //newType = Arrays.copyOf(newType, size);
          //newNext = Arrays.copyOf(newNext, size);
          int[] oldNewName = newName;
          int[] oldNewType = newType;
          int[] oldNewNext = newNext;
          newName = new int[size];
          newType = new int[size];
          newNext = new int[size];
          System.arraycopy(oldNewName, 0, newName, 0, oldNewName.length);
          System.arraycopy(oldNewType, 0, newType, 0, oldNewType.length);
          System.arraycopy(oldNewNext, 0, newNext, 0, oldNewNext.length);
        }
        if (term.type[cur] == VARIABLE) {
          Term tvar = varTable.getTailVar(term.getName(cur) + term.getOffset());
          int tvarname = tvar.name[tvar.start] + tvar.offset;
          Term tval = varTable.getTailValue(tvarname);
          if (tval != null) {
            if (tval.getArity() != 0) {
              termCont = new TermCont(term, cur+1, end, termCont);
              term = tval;
              cur  = term.getStart();
              end  = term.getNext(cur);
              extended = true;
              continue;
            }
            newName[pos] = tval.getStartName();
            newType[pos] = tval.getStartType();
            newNext[pos] = pos + 1;
          }
          else {
            newName[pos] = tvarname;
            newType[pos] = VARIABLE;
            newNext[pos] = pos + 1;
          }
        }
        else {
          newName[pos] = term.name[cur];
          newType[pos] = term.type[cur];
          newNext[pos] = term.next[cur] - start; //pos + 1;  // this value will be updated at the end of this method.
          //newNext[pos] = pos + 1;  // this value will be updated at the end of this method.
/*
          // How long extended?
          int diff = symTable.getArity(newName[pos], newType[pos]);
          newNext[pos] = pos + 1 + diff;
          if (diff != 0) {
            // Add the extended length to the original length
            int p = 0;
            while (p < pos)
              if (newNext[p] > pos)
                newNext[p++] += diff;
              else
                p = newNext[p];
          }
 */
        }
        cur++;
        pos++;
      }
      if (termCont == null) break;
      term = termCont.getTerm();
      cur  = termCont.getCur();
      end  = termCont.getEnd();
      termCont = termCont.getPrev();
    }

    // Updates the next indexes.
    if (extended) {
      for (int i = pos - 1; i >= 0; i--) {
        int arity = symTable.getArity(newName[i], newType[i]);
        int next = i + 1;
        if (arity > 0)
          for (int j = 0; j < arity; j++)
            next = newNext[next];
        newNext[i] = next;
      }
    }

    return new Term(env, newName, newType, newNext, 0, 0);
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
    VarTable varTable = env.getVarTable();
    int cur = start;
    int end = next[cur];
    while (cur != end) {
      if (type[cur] == VARIABLE) {
        int varname = name[cur] + offset;
        assert(varTable.getTailValue(varname) == null);
        name[cur] = renameMap.put(varname);
      }
      cur++;
    }
  }

  /**
   * Renames the variables using the specified rename-mapping. No variable in this clause must have a value.
   * @param renameMap  the rename-mapping.
   * @param minVarName the smallest variable name to be renamed.
   * @param minVarName the largest variable name to be renamed.
   */
  public void subrename(VarRenameMap renameMap, int minVarName, int maxVarName) {
    VarTable varTable = env.getVarTable();
    int cur = start;
    int end = next[cur];
    while (cur != end) {
      if (type[cur] == VARIABLE) {
        int varname = name[cur] + offset;
        if (minVarName <= varname && varname <= maxVarName) {
          assert(varTable.getTailValue(varname) == null);
          name[cur] = renameMap.put(varname);
        }
      }
      cur++;
    }
  }

  /**
   * Returns a non-variable argument of a function or predicate if exits.
   * @param predicate If true, then returns a non-variable argument of a predicate.
   * @return a non-variable argument of a function or predicate if exits.
   */
  public Term getNonVarArg(boolean predicate) {
    assert(type[start] == PREDICATE);
    assert(env.getVarTable().state() == 0);

    PriorityMap priorityMap = env.getPriorityMap();
    WeightMap   weightMap   = env.getWeightMap();

    int cur = start + 1;
    int end = next[start];
    while (cur < end) {
      if (predicate) {
        if (type[cur] != VARIABLE) {
          Term sub = newSubTerm(this, cur);
          if (!(weightMap != null && priorityMap != null && weightMap.isMin(sub) && priorityMap.isMin(sub)))
            return sub;
        }
      }
      else if (type[cur] == FUNCTION) {
        int fcur = cur + 1;
        int fend = next[cur];
        while (fcur < fend) {
          if (type[fcur] != VARIABLE) {
            Term sub = newSubTerm(this, fcur);
            if (!(weightMap != null && priorityMap != null && weightMap.isMin(sub) && priorityMap.isMin(sub)))
              return sub;
          }
          fcur = next[fcur];
        }
      }
      cur = next[cur];
    }
    return null;
  }

  /**
   * Returns the name at the specified position in this term.
   * @param pos
   * @return the name at the specified position
   */
  public int getName(int pos) {
    return name[pos];
  }

  /**
   * Returns the name of this term.
   * @return the name of this term.
   */
  public int getName() {
    return name[start];
  }

  /**
   * Returns the type at the specified position in this term.
   * @param pos
   * @return the type at the specified position
   */
  public int getType(int pos) {
    return type[pos];
  }

  /**
   * Returns the type of this term.
   * @return the type of this term.
   */
  public int getType() {
    return type[start];
  }

  /**
   * Returns the next position at the specified position in this term.
   * @param pos
   * @return the next position at the specified position
   */
  public int getNext(int pos) {
    return next[pos];
  }

  /**
   * Returns the start position of this term.
   * @return the start position.
   */
  public int getStart() {
    return start;
  }

  /**
   * Returns the variable offset of this term.
   * @return the variable offset.
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Sets the variable offset of this term.
   * @param offset the variable offset.
   */
  protected void setOffset(int offset) {
    this.offset = offset;
  }

  /**
   * Returns the name at the start position of this term.
   * @return the name at the start position.
   */
  public int getStartName() {
    return name[start];
  }

  /**
   * Returns the type at the start position of this term.
   * @return the type at the start position.
   */
  public int getStartType() {
    return type[start];
  }

  /**
   * Returns the number of kinds of variables contained in this term.
   * @param deep if true, then look up values of variables in this term.
   * @return the number of kinds of variables contained in this term.
   */
  public int getNumVars() {
    VarCounter varCounter = env.getVarCounter();
    countVars(varCounter);
    return varCounter.size();
  }

  /**
   * Counts the number of kinds of variables in this term.
   * @param varCounter the variable counter.
   */
  public void countVars(VarCounter varCounter) {
    VarTable varTable = env.getVarTable();
    Term x = this;
    ArrayQueue<Term> xqueue = env.getCTermQueue();
    int xcur = x.start;
    int xend = x.next[xcur];
    while (true) {
      while (xcur != xend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        if (xtype == VARIABLE) {
          Term xvar = varTable.getTailVar(xname + x.offset);
          int xvarname = xvar.name[xvar.start] + xvar.offset;
          Term xval = varTable.getValue(xvarname);
          if (xval == null) {
            varCounter.set(xvarname);
          } else {
            xqueue.add(xval);
          }
        }
        xcur++;
      }
      if (xqueue.isEmpty())
        break;
      x = xqueue.remove();
      xcur = x.start;
      xend = x.next[xcur];
    }
  }

  /**
   * Counts the number of kinds of variables in this term.
   * @param varCounter the variable counter.
   */
  public void countVars(IntSet varCounter) {
    VarTable varTable = env.getVarTable();
    Term x = this;
    ArrayQueue<Term> xqueue = env.getCTermQueue();
    int xcur = x.start;
    int xend = x.next[xcur];
    while (true) {
      while (xcur != xend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        if (xtype == VARIABLE) {
          Term xvar = varTable.getTailVar(xname + x.offset);
          int xvarname = xvar.name[xvar.start] + xvar.offset;
          Term xval = varTable.getValue(xvarname);
          if (xval == null) {
            varCounter.add(xvarname);
          } else {
            xqueue.add(xval);
          }
        }
        xcur++;
      }
      if (xqueue.isEmpty())
        break;
      x = xqueue.remove();
      xcur = x.start;
      xend = x.next[xcur];
    }
  }

  /**
   * Returns the size of this term.
   * @param deep if true, then look up values of variables in this term.
   * @return the size of the term.
   */
  public int getNumSyms(boolean deep) {
    VarTable varTable = env.getVarTable();
    TermCont termCont = null;
    Term term = this;
    int  num  = 0;
    int  cur  = 0;
    int  end  = term.next[cur];
    while (true) {
      while (cur != end) {
        if (term.type[cur] == VARIABLE) {
          if (deep) {
            Term tval = varTable.getTailValue(term.getName(cur) + term.getOffset());
            if (tval != null) {
              if (tval.getArity() != 0) {
                if (cur + 1 != end)
                  termCont = new TermCont(term, cur+1, end, termCont);
                term = tval;
                cur  = term.getStart();
                end  = term.getNext(cur);
                continue;
              }
            }
          }
          cur++;
          continue;
        }
        num++;
        cur++;
      }
      if (termCont == null) break;
      term = termCont.getTerm();
      cur  = termCont.getCur();
      end  = termCont.getEnd();
      termCont = termCont.getPrev();
    }
    return num;
  }

  /**
   * Returns true if this term has binded variables.
   * @return true if this term has binded variables.
   */
  public boolean hasBindedVars() {
    VarTable varTable = env.getVarTable();
    Term x = this;
    int xcur = x.start;
    int xend = x.next[xcur];
    while (xcur != xend) {
      if (x.type[xcur] == VARIABLE)
        if (varTable.getValue(x.name[xcur] + x.offset) != null)
          return true;
      xcur++;
    }
    return false;
  }

  /**
   * Creates a new variable with the specified name number.
   * @param env the environment.
   * @param var the variable name.
   * @return the new variable.
   */
  public static Term createVar(Env env, int var) {
    int[] name = { var };
    int[] kind = { VARIABLE } ;
    int[] next = { 1 };
    return new Term(env, name, kind, next);
  }

  /**
   * Creates a new constant with the specified name.
   * @param env the environment.
   * @param str the constant name.
   * @return the new constant.
   */
  public static Term createConstant(Env env, String str) {
    int[] name = { env.getSymTable().putConstant(str) };
    int[] kind = { CONSTANT } ;
    int[] next = { 1 };
    return new Term(env, name, kind, next);
  }

  /**
   * Creates a new constant with the specified name.
   * @param env  the environment.
   * @param name the constant name.
   * @return the new constant.
   */
  public static Term createConstant(Env env, int _name) {
    int[] name = { _name };
    int[] kind = { CONSTANT } ;
    int[] next = { 1 };
    return new Term(env, name, kind, next);
  }

  /**
   * Creates a new predicate that has no argument.
   * @param env the environment.
   * @param str the predicate name.
   * @return the new predicate.
   */
  public static Term createPredicate(Env env, String str) {
    int[] name = { env.getSymTable().putPredicate(str, 0) };
    int[] kind = { PREDICATE } ;
    int[] next = { 1 };
    return new Term(env, name, kind, next);
  }

  /**
   * Creates a new predicate that has the specified arguments. Every argument must be instantiated.
   * @param env  the environment.
   * @param str  the predicate name.
   * @param args the arguments of the predicate.
   * @return the new predicate.
   */
  public static Term createPredicate(Env env, String name, List<Term> args) {
    return createPredicate(env, name, args.toArray(new Term[0]));
  }

  /**
   * Creates a new predicate that has the specified arguments. Every argument must be instantiated.
   * @param env  the environment.
   * @param str  the predicate name.
   * @param args the arguments of the predicate.
   * @return the new predicate.
   */
  public static Term createPredicate(Env env, String name, Term... args) {
    return createPredicate(env, env.getSymTable().putPredicate(name, args.length), args);
  }

  /**
   * Creates a new predicate that has the specified arguments. Every argument must be instantiated.
   * @param env  the environment.
   * @param str  the predicate name.
   * @param args the arguments of the predicate.
   * @return the new predicate.
   */
  public static Term createPredicate(Env env, int name, List<Term> args) {
    return createPredicate(env, name, args.toArray(new Term[0]));
  }

  /**
   * Creates a new predicate that has the specified arguments. Every argument must be instantiated.
   * @param env  the environment.
   * @param name the predicate name.
   * @param args the arguments of the predicate.
   * @return the new predicate.
   */
  public static Term createPredicate(Env env, int name, Term... args) {

    // Calculates the size of the new predicate.
    int size = 1;
    for (Term arg : args)
      size += arg.size(false);

    int[] newName = new int[size];
    int[] newType = new int[size];
    int[] newNext = new int[size];
    SymTable symTable = env.getSymTable();

    // Copies the argument to the predicate.
    newName[0] = name;
    newType[0] = PREDICATE;
    int pos = 1;
    for (Term arg : args) {
      int cur = arg.start;
      int end = arg.next[cur];
      while (cur < end) {
        newName[pos] = arg.name[cur];
        newType[pos] = arg.type[cur];
        if (newType[pos] == VARIABLE)
          newName[pos] += arg.offset;
        pos++;
        cur++;
      }
    }

    // Updates the next indexes.
    for (int i = pos - 1; i >= 0; i--) {
      int arity = symTable.getArity(newName[i], newType[i]);
      int next = i + 1;
      if (arity > 0)
        for (int j = 0; j < arity; j++)
          next = newNext[next];
      newNext[i] = next;
    }

    return new Term(env, newName, newType, newNext, 0, 0);
  }

  /**
   * Creates a new function that has the specified arguments. Every argument must be instantiated.
   * @param env  the environment.
   * @param name the function name.
   * @param args the arguments of the predicate.
   * @return the new function.
   */
	public static Term createFunction(Env env, int name, List<Term> args) {
		return createFunction(env, name, args.toArray(new Term[0]));
	}

	/**
   * Creates a new function that has the specified arguments. Every argument must be instantiated.
   * @param env  the environment.
   * @param name the function name.
   * @param args the arguments of the function.
   * @return the new function.
   */
  public static Term createFunction(Env env, int name, Term... args) {

    // Calculates the size of the new function.
    int size = 1;
    for (Term arg : args)
      size += arg.size(false);

    int[] newName = new int[size];
    int[] newType = new int[size];
    int[] newNext = new int[size];
    SymTable symTable = env.getSymTable();

    // Copies the argument to the function.
    newName[0] = name;
    newType[0] = FUNCTION;
    int pos = 1;
    for (Term arg : args) {
      int cur = arg.start;
      int end = arg.next[cur];
      while (cur < end) {
        newName[pos] = arg.name[cur];
        newType[pos] = arg.type[cur];
        if (newType[pos] == VARIABLE)
          newName[pos] += arg.offset;
        pos++;
        cur++;
      }
    }

    // Updates the next indexes.
    for (int i = pos - 1; i >= 0; i--) {
      int arity = symTable.getArity(newName[i], newType[i]);
      int next = i + 1;
      if (arity > 0)
        for (int j = 0; j < arity; j++)
          next = newNext[next];
      newNext[i] = next;
    }

    return new Term(env, newName, newType, newNext, 0, 0);
  }

  /**
   * Returns the argument at the specified position.
   * @param pos the position of the argument (the first argument is 0).
   * @return the argument at the specified position.
   */
  public Term getArg(int pos) {
    assert(pos < getArity());
    int index = start + 1;
    for (int i=0; i < pos; i++)
      index = next[index];
    return newSubTerm(this, index);
  }

  /**
   * Returns the arity at this literal.
   * @return the arity at this literal.
   */
  public int getArity() {
    return env.getSymTable().getArity(name[start], type[start]);
  }

  /**
   * Replaces the specified old term with the new term.
   * @param oldTerm the old term to be replaced.
   * @param newTerm the new term to replace.
   * @return the new term in which old term are replaced with the new term.
   */
  public Term replace(Term oldTerm, Term newTerm) {
    assert(env.getVarTable().state() == 0);
    assert(oldTerm.name == this.name);  // requires that the old term is truly sub term of this object.

    // Calculates the size of the new predicate.
    int repSize = size(false) + newTerm.size(false) - oldTerm.size(false);

    int[] newName = new int[repSize];
    int[] newType = new int[repSize];
    int[] newNext = new int[repSize];

    // Replaces old terms with the new term.
    int newSize = newTerm.size(false);
    int pos = 0;
    int cur = start;
    int end = next[cur];
    while (cur < end) {
      if (cur == oldTerm.start) {
        for (int i=0; i < newSize; i++) {
          newName[pos] = newTerm.name[newTerm.start + i];
          newType[pos] = newTerm.type[newTerm.start + i];
          pos++;
        }
        cur = next[cur];
      }
      else {
        newName[pos] = name[cur];
        newType[pos] = type[cur];
        pos++;
        cur++;
      }
    }

    // Updates the next indexes.
    SymTable symTable = env.getSymTable();
    for (int i = pos - 1; i >= 0; i--) {
      int arity = symTable.getArity(newName[i], newType[i]);
      int next = i + 1;
      if (arity > 0)
        for (int j = 0; j < arity; j++)
          next = newNext[next];
      newNext[i] = next;
    }

    return new Term(env, newName, newType, newNext, 0, 0);
  }

  /**
   * Replaces the all specified old terms with the new term.
   * @param oldTerm the old term to be replaced.
   * @param newTerm the new term to replace.
   * @return the new term in which all old terms are replaced with the new term.
   */
  public Term replaceAll(Term oldTerm, Term newTerm) {
    assert(env.getVarTable().state() == 0);

    // Finds the occurrences of oldTerm in this term.
    BitSet occ = findSubTerms(oldTerm);
    if (occ == null)
      return this;

    // Calculates the size of the new predicate.
    int repSize = size(false) + occ.cardinality() * (newTerm.size(false) - oldTerm.size(false));

    int[] newName = new int[repSize];
    int[] newType = new int[repSize];
    int[] newNext = new int[repSize];

    // Replaces old terms with the new term.
    int newSize = newTerm.size(false);
    int pos = 0;
    int cur = start;
    int end = next[cur];
    while (cur < end) {
      if (occ.get(cur)) {
        for (int i=0; i < newSize; i++) {
          newName[pos] = newTerm.name[newTerm.start + i];
          newType[pos] = newTerm.type[newTerm.start + i];
          pos++;
        }
        cur = next[cur];
      }
      else {
        newName[pos] = name[cur];
        newType[pos] = type[cur];
        pos++;
        cur++;
      }
    }

    // Updates the next indexes.
    SymTable symTable = env.getSymTable();
    for (int i = pos - 1; i >= 0; i--) {
      int arity = symTable.getArity(newName[i], newType[i]);
      int next = i + 1;
      if (arity > 0)
        for (int j = 0; j < arity; j++)
          next = newNext[next];
      newNext[i] = next;
    }

    return new Term(env, newName, newType, newNext, 0, 0);
  }

  /**
   * Replaces the specified sub-term with a variable.
   * @param pos  the position of the sub-term to be replaced.
   * @param var  the variable name,
   * @return the new term.
   */
  public Term replaceWithVar(int pos, int var) {

    int shrink = (next[pos] - pos) - 1;
    int len = name.length - shrink;
    int[] newName = new int[len];
    int[] newType = new int[len];
    int[] newNext = new int[len];

    // Copies the original values.
    int src = 0;
    int dst = 0;
    // 1. Copies the elements before the sub-term to be replaced.
    while (src < pos) {
      newName[dst] = name[src];
      newType[dst] = type[src];
      newNext[dst] = next[src];
      if (pos < newNext[dst])
        newNext[dst] -= shrink;
      dst++;
      src++;
    }
    // 2. Inserts the variable.
    newName[dst] = var - offset;
    newType[dst] = VARIABLE;
    newNext[dst] = dst + 1;
    dst++;
    src = next[src];
    // 3. Copies the rest elements.
    while (src < name.length) {
      newName[dst] = name[src];
      newType[dst] = type[src];
      newNext[dst] = next[src] - shrink;
      dst++;
      src++;
    }

//    System.arraycopy(name, 0, newName, 0, len);
//    System.arraycopy(type, 0, newType, 0, len);
//    System.arraycopy(next, 0, newNext, 0, len);
//
//    // Replaces the specified argument.
//    newName[pos] = var - offset;
//    newType[pos] = VARIABLE;
//
//    for (int i=pos+1; i < newNext[pos]; i++) {
//      newName[i] = 0;
//      newType[i] = UNDEF_TERM_TYPE;
//      newNext[i] = i+1;
//    }

    return new Term(env, newName, newType, newNext, start, offset);
  }

  /**
   * Returns the set of indexes at which the sub-term occurs.
   * @param subTerm the sub term to find.
   * @return return the set of indexes at which the sub-term occurs.
   */
  public BitSet findSubTerms(Term subTerm) {
    BitSet occ = null;

    int cur  = start;
    int end  = next[cur];
    int spos = subTerm.start;
    int size = subTerm.size(false);
    while (cur < end - size + 1) {
      int i = 0;
      for (; i < size; i++)
        if (name[cur + i] != subTerm.name[spos + i] || type[cur + i] != subTerm.type[spos + i])
          break;
      // Found?
      if (i == size) {
        if (occ == null)
          occ = new BitSet();
        occ.set(cur);
      }
      cur++;
    }

    return occ;
  }

  /**
   * Returns the mgu if this term is unifiable with the specified term.
   * @param y the specified term.
   * @return the mgu if this term is unifiable with the specified term.
   */
  public Subst isUnifiable(Term y) {
    VarTable varTable = env.getVarTable();
    int state = varTable.state();
    Subst g = unify(y);
    varTable.backtrackTo(state);
    return g;
  }

  /**
   * Unifies this term and the specified term. If unifiable, then return the mgu.
   * @param y the specified term.
   * @return the mgu if unifiable.
   */
  public Subst unify(Term y) {
    return unify(this, start, y, y.start);
  }

  /**
   * Unifies the term x and the term y. If unifiable, then return the mgu.
   * @param x    the term.
   * @param xcur the start position of x.
   * @param y    the term.
   * @param ycur the start position of y.
   * @return the mgu if unifiable.
   */
  public static Subst unify(Term x, int xcur, Term y, int ycur) {
    VarTable varTable = x.env.getVarTable();
    int   state = varTable.state();
    Subst g     = new Subst();
    int   xend  = x.next[xcur];
    int   yend  = y.next[ycur];
    ArrayQueue<Term> xqueue = x.env.getXTermQueue();
    ArrayQueue<Term> yqueue = x.env.getYTermQueue();
    while (true) {
      while (xcur != xend && ycur != yend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        int yname = y.name[ycur];
        int ytype = y.type[ycur];
        if (xtype != VARIABLE) {
          if (ytype != VARIABLE) {
            if (xname != yname || xtype != ytype) {
              varTable.backtrackTo(state);
              return null;
            }
            xcur++;
            ycur++;
            continue;
          }
          else {  // ytype == VARIABLE
            Term xval = x;
            if (xcur != x.start)
              xval = Term.newSubTerm(x, xcur);
            Term yvar = varTable.getTailVar(yname + y.offset);
            int yvarname = yvar.name[yvar.start] + yvar.offset;
            Term yval = varTable.getValue(yvarname);
            if (yval == null) {
              int syms = xval.containsVar(yvarname);
              if (syms == -1) {
                varTable.backtrackTo(state);
                return null;
              }
              g.add(yvarname, xval, syms);
              varTable.substitute(yvarname, xval);
            }
            else {
              xqueue.add(xval);
              yqueue.add(yval);
            }
          }
        }
        else {  // xtype == VARIABLE
          if (ytype != VARIABLE) {
            Term xvar = varTable.getTailVar(xname + x.offset);
            int xvarname = xvar.name[xvar.start] + xvar.offset;
            Term xval = varTable.getValue(xvarname);
            Term yval = y;
            if (ycur != y.start)
              yval = Term.newSubTerm(y, ycur);
            if (xval == null) {
              int syms = yval.containsVar(xvarname);
              if (syms == -1) {
                varTable.backtrackTo(state);
                return null;
              }
              g.add(xvarname, yval, syms);
              varTable.substitute(xvarname, yval);
            }
            else {
              xqueue.add(xval);
              yqueue.add(yval);
            }
          }
          else {  // xtype == VARIABLE && ytype == VARIABLE
            Term xvar = varTable.getTailVar(xname + x.offset);
            Term yvar = varTable.getTailVar(yname + y.offset);
            int xvarname = xvar.name[xvar.start] + xvar.offset;
            int yvarname = yvar.name[yvar.start] + yvar.offset;
            Term xval = varTable.getValue(xvarname);
            Term yval = varTable.getValue(yvarname);
            if (xval == null) {
              if (yval == null) {
                if (xvarname != yvarname) {
                  g.add(xvarname, yvar, 0);
                  varTable.substitute(xvarname, yvar);
                }
                else {
                  ;
                }
              }
              else {
                int syms = yval.containsVar(xvarname);
                if (syms == -1) {
                  varTable.backtrackTo(state);
                  return null;
                }
                g.add(xvarname, yval, syms);
                varTable.substitute(xvarname, yval);
              }
            }
            else {
              if (yval == null) {
                int syms = xval.containsVar(yvarname);
                if (syms == -1) {
                  varTable.backtrackTo(state);
                  return null;
                }
                g.add(yvarname, xval, syms);
                varTable.substitute(yvarname, xval);
              }
              else {
                xqueue.add(xval);
                yqueue.add(yval);
              }
            }
          }
        }
        xcur = x.next[xcur];
        ycur = y.next[ycur];
      }
      assert(xcur == xend && ycur == yend);
      if (xqueue.isEmpty()) break;
      assert(!yqueue.isEmpty());
      x = xqueue.remove();
      y = yqueue.remove();
      xcur = x.start;
      ycur = y.start;
      xend = x.next[xcur];
      yend = y.next[ycur];
    }
    assert(ycur == yend && yqueue.isEmpty());
    return g;
  }

  /**
   * Returns the substitution if this term is able to subsume with the specified term.
   * @param y the specified term.
   * @return the substitution if this term is unifiable with the specified term.
   */
  public Subst isSubsuming(Term y) {
    VarTable varTable = env.getVarTable();
    int state = varTable.state();
    Subst g = subsumes(y);
    varTable.backtrackTo(state);
    return g;
  }

  /**
   * Returns a substitution if this term subsumes the specified term.
   * @param y the specified term.
   * @return the substitution if this term subsumes the specified term.
   */
  public Subst subsumes(Term y) {
    return subsumes(this, start, y, y.start, y);
  }

  /**
   * Returns a substitution if this term subsumes the specified term.
   * @param y the specified term.
   * @param yclause the clause contains the literal y.
   * @return the substitution if this term subsumes the specified term.
   */
  public Subst subsumes(Term y, VarHolder yorg) {
    return subsumes(this, start, y, y.start, yorg);
  }

  /**
   * Returns a substitution if the term x subsumes the term y.
   * @param x    the term.
   * @param xcur the start position of x.
   * @param y    the term.
   * @param ycur the start position of y.
   * @param yorg the original term of y.
   * @return the substitution if the term x subsumes the term y.
   */
  public static Subst subsumes(Term x, int xcur, Term y, int ycur, VarHolder yorg) {
    VarTable varTable = x.env.getVarTable();
    int   state  = varTable.state();
    Subst g      = new Subst();
    int   xend   = x.next[xcur];
    int   yend   = y.next[ycur];
    ArrayQueue<Term> xqueue = x.env.getXTermQueue();
    ArrayQueue<Term> yqueue = x.env.getYTermQueue();
    while (true) {
      while (xcur != xend && ycur != yend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        int yname = y.name[ycur];
        int ytype = y.type[ycur];
        if (xtype != VARIABLE) {
          if (ytype != VARIABLE) {
            if (xname != yname || xtype != ytype) {
              varTable.backtrackTo(state);
              return null;
            }
            xcur++;
            ycur++;
            continue;
          }
          else {  // ytype == VARIABLE
            Term yvar = varTable.getTailVar(yname + y.offset);
            Term yval = varTable.getValue(yvar.name[yvar.start] + yvar.offset);
            if (yval == null) {
              varTable.backtrackTo(state);
              return null;
            }
            else {
              Term xval = x;
              if (xcur != x.start)
                xval = Term.newSubTerm(x, xcur);
              xqueue.add(xval);
              yqueue.add(yval);
            }
          }
        }
        else {  // xtype == VARIABLE
          if (ytype != VARIABLE) {
            Term xvar = varTable.getTailVar(xname + x.offset);
            int xvarname = xvar.name[xvar.start] + xvar.offset;
            Term xval = varTable.getValue(xvarname);
            Term yval = y;
            if (ycur != y.start)
              yval = Term.newSubTerm(y, ycur);
            if (xval == null) {
              int syms = yorg.containsVar(xvarname);
              if (syms == -1) {
                varTable.backtrackTo(state);
                return null;
              }
              g.add(xvarname, yval, syms);
              varTable.substitute(xvarname, yval);
            }
            else {
              xqueue.add(xval);
              yqueue.add(yval);
            }
          }
          else {  // xtype == VARIABLE && ytype == VARIABLE
            Term xvar = varTable.getTailVar(xname + x.offset);
            Term yvar = varTable.getTailVar(yname + y.offset);
            int xvarname = xvar.name[xvar.start] + xvar.offset;
            int yvarname = yvar.name[yvar.start] + yvar.offset;
            Term xval = varTable.getValue(xvarname);
            Term yval = varTable.getValue(yvarname);
            if (xval == null) {
              if (yval == null) {
                if (xvarname != yvarname) {
                  int syms = yorg.containsVar(xvarname);
                  if (syms == -1) {
                    varTable.backtrackTo(state);
                    return null;
                  }
                  g.add(xvarname, yvar, syms);
                  varTable.substitute(xvarname, yvar);
                }
                else {
                  ;
                }
              }
              else {
                int syms = yorg.containsVar(xvarname);
                if (syms == -1) {
                  varTable.backtrackTo(state);
                  return null;
                }
                g.add(xvarname, yval, syms);
                varTable.substitute(xvarname, yval);
              }
            }
            else {
              if (yval == null) {
                varTable.backtrackTo(state);
                return null;
              }
              else {
                xqueue.add(xval);
                yqueue.add(yval);
              }
            }
          }
        }
        xcur = x.next[xcur];
        ycur = y.next[ycur];
      }
      assert(xcur == xend && ycur == yend);
      if (xqueue.isEmpty()) break;
      assert(!yqueue.isEmpty());
      x = xqueue.remove();
      y = yqueue.remove();
      xcur = x.start;
      ycur = y.start;
      xend = x.next[xcur];
      yend = y.next[ycur];
    }
    assert(ycur == yend && yqueue.isEmpty());
    return g;
  }

  /**
   * Returns -1 if this object contains the specified variable.
   * @param varname the variable number to check. It should be the last variable of a variable-chain.
   * @return -1 if this object contains the specified variable. Otherwise, returns the number of symbols in the object.
   */
  public int containsVar(int varname) {
    VarTable varTable = env.getVarTable();
//    // If{X/Y, Y/null}, then Y doesn't contain X.
//    if (this.isVar())
//      if (varTable.getValue(name[start] + offset) == null)
//        return 0;
//    Term v = varTable.getTailVar(varname);
//    varname = v.name[v.start] + v.offset;
    int num = 0;
    Term  x = this;
    ArrayQueue<Term> vqueue = env.getVTermQueue();
    int xcur = x.start;
    int xend = x.next[xcur];
    // We should check whether the variable-chain contains the "varname"?
    while (true) {
      while (xcur != xend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        if (xtype == VARIABLE) {
          if (xname + x.offset == varname)
            return -1;
          Term xval = varTable.getValue(xname + x.offset);
          if (xval != null)
            vqueue.add(xval);
        }
        else
          num++;
        xcur++;
      }
      if (vqueue.isEmpty())
        break;
      x = vqueue.remove();
      xcur = x.start;
      xend = x.next[xcur];
    }
    return num;
  }

  /**
   * Returns true if this term is ground.
   * @return true if this term is ground.
   */
  public boolean isGround() {
    VarTable varTable = env.getVarTable();
    Term x = this;
    ArrayQueue<Term> vqueue = env.getVTermQueue();
    int xcur = x.start;
    int xend = x.next[xcur];
    while (true) {
      while (xcur != xend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        if (xtype == VARIABLE) {
          Term xval = varTable.getTailValue(xname + x.offset);
          if (xval == null)
            return false;
          vqueue.add(xval);
        }
        xcur++;
      }
      if (vqueue.isEmpty())
        break;
      x = vqueue.remove();
      xcur = x.start;
      xend = x.next[xcur];
    }
    return true;
  }

  /**
   * Returns true if this literal is maximally general, i.e., all arguments of this literal are variables.
   * @return true if this literal is maximally general.
   */
  public boolean isMaxGeneral() {
    VarTable varTable = env.getVarTable();
    int cur = start;
    int end = next[start];
    for (cur++; cur != end; cur++) {
      if (type[cur] != VARIABLE)
        return false;
      Term val = varTable.getTailValue(name[cur] + offset);
      if (val != null)
        return false;
    }
    return true;
  }

  /**
   * Returns true if this term is a variable.
   * @return true if this term is a variable.
   */
  public boolean isVar() {
    return type[start] == VARIABLE;
  }

  /**
   * Returns a variable name of this variable.
   * @return a variable name of this variable.
   */
  public int getVarName() {
    assert(isVar());
    return name[start] + offset;
  }

  /**
   * Returns true if the term x is equals to the term y.
   * @param x    the term.
   * @param xcur the start position of x.
   * @param y    the term.
   * @param ycur the start position of y.
   * @return true if the term x is equals to the term y.
   */
  public static boolean equals(Term x, Term y) {
    return equals(x, x.start, y, y.start);
  }

  /**
   * Returns true if the term x is equals to the term y.
   * @param x    the term.
   * @param xcur the start position of x.
   * @param y    the term.
   * @param ycur the start position of y.
   * @return true if the term x is equals to the term y.
   */
  public static boolean equals(Term x, int xcur, Term y, int ycur) {
    VarTable varTable = x.env.getVarTable();
    int xend = x.next[xcur];
    int yend = y.next[ycur];
    ArrayQueue<Term> xqueue = x.env.getXTermQueue();
    ArrayQueue<Term> yqueue = x.env.getYTermQueue();
    while (true) {
      while (xcur != xend && ycur != yend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        int yname = y.name[ycur];
        int ytype = y.type[ycur];
        if (xtype != VARIABLE) {
          if (ytype != VARIABLE) {
            if (xname != yname || xtype != ytype)
              return false;
            xcur++;
            ycur++;
            continue;
          }
          else {  // ytype == VARIABLE
            Term yval = varTable.getTailValue(yname + y.offset);
            if (yval == null)
              return false;
            Term xval = x;
            if (xcur != x.start)
              xval = Term.newSubTerm(x, xcur);
            xqueue.add(xval);
            yqueue.add(yval);
          }
        }
        else {  // xtype == VARIABLE
          if (ytype != VARIABLE) {
            Term xval = varTable.getValue(xname + x.offset);
            if (xval == null)
              return false;
            Term yval = y;
            if (ycur != y.start)
              yval = Term.newSubTerm(y, ycur);
            xqueue.add(xval);
            yqueue.add(yval);
          }
          else {  // xtype == VARIABLE && ytype == VARIABLE
            Term xvar = varTable.getTailVar(xname + x.offset);
            Term yvar = varTable.getTailVar(yname + y.offset);
            int xvarname = xvar.name[xvar.start] + xvar.offset;
            int yvarname = yvar.name[yvar.start] + yvar.offset;
            Term xval = varTable.getValue(xvarname);
            Term yval = varTable.getValue(yvarname);
            if (xval != null && yval != null) {
              xqueue.add(xval);
              yqueue.add(yval);
            }
            else if (xval == null && yval == null) {
              if (xvarname != yvarname)
                return false;
            }
            else
              return false;
          }
        }
        xcur = x.next[xcur];
        ycur = y.next[ycur];
      }
      assert(xcur == xend && ycur == yend);
      if (xqueue.isEmpty()) break;
      assert(!yqueue.isEmpty());
      x = xqueue.remove();
      y = yqueue.remove();
      xcur = x.start;
      ycur = y.start;
      xend = x.next[xcur];
      yend = y.next[ycur];
    }
    assert(ycur == yend && yqueue.isEmpty());
    return true;
  }

  /**
   * Updates the specified feature vector.
   * @param deep if true, then look up values of variables in this term.
   * @param fvec the specified feature vector.
   * @param sign the sign of this term.
   */
  public void getFVec(boolean deep, FVec fvec, boolean sign) {
    VarTable varTable = env.getVarTable();
    Term x = this;
    ArrayQueue<Term> xqueue = x.env.getXTermQueue();
    int xcur = x.start;
    int xend = x.next[xcur];
    while (true) {
      while (xcur != xend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        if (xtype != VARIABLE) {
          fvec.inc(xname, xtype, sign);
        }
        else {
          if (deep) {
            Term xvar = varTable.getTailVar(xname + x.offset);
            int xvarname = xvar.name[xvar.start] + xvar.offset;
            Term xval = varTable.getValue(xvarname);
            if (xval != null) {
              xqueue.add(xval);
            }
            else
              fvec.setMaxVarName(xvarname);
          }
          else {
            fvec.setMaxVarName(xname + x.offset);
          }
        }
        xcur ++;
      }
      if (xqueue.isEmpty())
        break;
      x = xqueue.remove();
      xcur = x.start;
      xend = x.next[xcur];
    }
  }

  /**
   * Returns the raw feature vector of this term.
   * @param raw      the raw feature vector of this term (output).
   * @param positive the sign of a term that contains this term.
   */
  public void getRawFVec(int[] raw, boolean positive) {
    VarTable varTable = env.getVarTable();
    FVecMap  fvecMap  = env.getFVecMap();
    Term x = this;
    ArrayQueue<Term> xqueue = x.env.getXTermQueue();
    int xcur = x.start;
    int xend = x.next[xcur];
    while (true) {
      while (xcur != xend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        if (xtype != VARIABLE) {
          raw[fvecMap.getRawIdx(xname, xtype, positive)]++;
        }
        else {
          Term xvar = varTable.getTailVar(xname + x.offset);
          int xvarname = xvar.name[xvar.start] + xvar.offset;
          Term xval = varTable.getValue(xvarname);
          if (xval != null)
            xqueue.add(xval);
        }
        xcur++;
      }
      if (xqueue.isEmpty())
        break;
      x = xqueue.remove();
      xcur = x.start;
      xend = x.next[xcur];
    }
  }

  /**
   * Returns the size of this term.
   * @param deep if true, then look up values of variables in this term.
   * @return the size of the term.
   */
  public int size(boolean deep) {
    if (deep)
      return size(start);
    return next[start] - start;
  }

  /**
   * Returns the size of this term.
   * @param deep if true, then look up values of variables in this term.
   * @param pos the start position.
   * @return the size of the term.
   */
  public int size(boolean deep, int pos) {
    if (deep)
      return size(pos);
    return next[pos] - pos;
  }

  /**
   * Returns the size of this term.
   * @param pos the start position.
   * @return the size of the term.
   */
  public int size(int pos) {
    VarTable varTable = env.getVarTable();
    TermCont termCont = null;
    Term term = this;
    int  num  = 0;
    int  cur  = pos;
    int  end  = term.next[cur];
    while (true) {
      while (cur != end) {
        if (term.type[cur] == VARIABLE) {
          Term tval = varTable.getTailValue(term.getName(cur) + term.getOffset());
          if (tval != null) {
            if (tval.getArity() != 0) {
              if (cur + 1 != end)
                termCont = new TermCont(term, cur+1, end, termCont);
              term = tval;
              cur  = term.getStart();
              end  = term.getNext(cur);
              continue;
            }
          }
        }
        num++;
        cur++;
      }
      if (termCont == null) break;
      term = termCont.getTerm();
      cur  = termCont.getCur();
      end  = termCont.getEnd();
      termCont = termCont.getPrev();
    }
    return num;
  }

  /**
   * Returns true if this term is the special top predicate.
   * @return true if this term is the special top predicate.
   */
  public boolean isTopPred() {
    return
      type[0] == PREDICATE &&
      name[0] == env.getSymTable().getName(TOP_PRED, PREDICATE, 0);
  }

  /**
   * Returns true if this term is an equality predicate.
   * @return true if this term is an equality predicate.
   */
  public boolean isEqualPred() {
    return type[0] == PREDICATE && name[0] == env.getSymTable().getEqualPredName();
  }

  /**
   * Returns true if this term is a connector predicate.
   * @return true if this term is a connector predicate.
   */
  public boolean isConnPred() {
    return env.getSymTable().hasTag(name[start], type[start], POS_SRC_CONN | NEG_SRC_CONN);
  }

  /**
   * Returns true if this term is a positive source connector predicate.
   * @return true if this term is a positive source connector predicate.
   */
  public boolean isPosConnPred() {
    return env.getSymTable().hasTag(name[start], type[start], POS_SRC_CONN);
  }

  /**
   * Returns true if this term is a negative source connector predicate.
   * @return true if this term is a negative source connector predicate.
   */
  public boolean isNegConnPred() {
    return env.getSymTable().hasTag(name[start], type[start], POS_SRC_CONN);
  }

  /**
   * Returns true if this term is an equality predicate.
   * @return true if this term is an equality predicate.
   */
  public boolean isSkolemConstant() {
    return type[start] == CONSTANT && env.getSymTable().hasTag(name[start], CONSTANT, SKOLEM);
  }

  /**
   * Returns true if this term has the specified tag.
   * @return true if this term has the specified tag.
   */
  public boolean hasTag(int tag) {
    return env.getSymTable().hasTag(name[start], type[start], tag);
  }

  /**
   * Calculates the term weight.
   * @param weight the weight of this term (output).
   */
  public void calcTermWegiht(TermWeight weight) {
    VarTable varTable = env.getVarTable();
    TermCont termCont = null;
    Term term = this;
    int cur = term.start;
    int end = term.next[cur];
    while (true) {
      while (cur != end) {
        if (term.type[cur] == VARIABLE) {
          Term var = varTable.getTailVar(term.name[cur] + term.offset);
          int varname = var.name[var.start] + var.offset;
          Term val = varTable.getValue(varname);
          if (val != null) {
            if (cur + 1 != end)
              termCont = new TermCont(term, cur+1, end, termCont);
            term = val;
            cur  = term.getStart();
            end  = term.getNext(cur);
            continue;
          }
          else {
            weight.count(varname, VARIABLE);
          }
        }
        else {
          weight.count(term.name[cur], term.type[cur]);
        }
        cur++;
      }
      if (termCont == null) break;
      term = termCont.getTerm();
      cur  = termCont.getCur();
      end  = termCont.getEnd();
      termCont = termCont.getPrev();
    }

/*
    VarTable varTable = env.getVarTable();
    ArrayQueue<Term> xqueue = env.getXTermQueue();
    Term x = this;
    int xcur = x.start;
    int xend = x.next[xcur];
    while (true) {
      while (xcur != xend) {
        int xname = x.name[xcur];
        int xtype = x.type[xcur];
        if (xtype != VARIABLE) {
          weight.count(xname, xtype);
        }
        else {
          Term xvar = varTable.getTailVar(xname + x.offset);
          int xvarname = xvar.name[xvar.start] + xvar.offset;
          Term xval = varTable.getValue(xvarname);
          if (xval != null)
            xqueue.add(xval);
          else
            weight.count(xvarname, VARIABLE);
        }
        xcur++;
      }
      if (xqueue.isEmpty())
        break;
      x = xqueue.remove();
      xcur = x.start;
      xend = x.next[xcur];
    }
 */
  }

  /**
   * Returns the hash code value of this object.
   * @return the hash code value of this object.
   */
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(name);
    result = prime * result + Arrays.hashCode(next);
    result = prime * result + offset;
    result = prime * result + start;
    result = prime * result + Arrays.hashCode(type);
    return result;
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
    Term other = (Term) obj;
    if (offset != other.offset)
      return false;
    if (start != other.start)
      return false;
    if (!Arrays.equals(name, other.name))
      return false;
    if (!Arrays.equals(next, other.next))
      return false;
    if (!Arrays.equals(type, other.type))
      return false;
    return true;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toSimpString() {
    return toSimpString(offset);
  }

  /**
   * Returns a string representation of this object.
   * @param offset a variable offset.
   * @return a string representation of this object.
   */
  public String toSimpString(int offset) {
    StringBuilder str = new StringBuilder();
    Stack<Character>  separators = new Stack<Character>();
    SymTable symTable = env.getSymTable();

    int cur = start;
    int end = next[cur];
    while (cur != end) {
      switch (type[cur]) {
      case CONSTANT:
        str.append(symTable.get(name[cur], type[cur]));
        break;
      case INTEGER:
        str.append(name[cur]);
        break;
      case VARIABLE:
        str.append('_');
        str.append(name[cur] + offset);
        break;
      case FUNCTION:
      case PREDICATE:
        str.append(symTable.get(name[cur], type[cur]));
        if (symTable.getArity(name[cur], type[cur]) != 0) {
          separators.push(')');
          for (int j=1; j < symTable.getArity(name[cur], type[cur]); j++)
            separators.push(',');
          separators.push('(');
        }
        break;
      default:
        assert(false);
      }

      // Print separators
      if (separators.size() > 0) {
        char last = separators.pop();
        str.append(last);

        // We must close the left parenthesis
        if (last == ')') {
          while (separators.size() > 0 && last != ',') {
            last = separators.pop();
            str.append(last);
          }
        }
      }

      cur++;
    }

    return str.toString();
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();
    Stack<Character>  separators = new Stack<Character>();
    SymTable symTable = env.getSymTable();

    int cur = start;
    int end = next[cur];
    while (cur != end) {
      switch (type[cur]) {
      case CONSTANT:
        str.append(symTable.get(name[cur], type[cur]));
        break;
      case INTEGER:
        str.append(name[cur]);
        break;
      case VARIABLE:
        Term tvar = env.getVarTable().getTailVar(name[cur] + offset);
        int varname = tvar.name[tvar.start] + tvar.offset;
        Term tval = env.getVarTable().getTailValue(varname);
        if (tval != null)
          str.append(tval.toString());
        else {
          str.append('_');
          str.append(varname);
        }
        break;
      case FUNCTION:
      case PREDICATE:
        str.append(symTable.get(name[cur], type[cur]));
        if (symTable.getArity(name[cur], type[cur]) != 0) {
          separators.push(')');
          for (int j=1; j < symTable.getArity(name[cur], type[cur]); j++)
            separators.push(',');
          separators.push('(');
        }
        break;
      default:
        assert(false);
      }

      // Print separators
      if (separators.size() > 0) {
        char last = separators.pop();
        str.append(last);

        // We must close the left parenthesis
        if (last == ')') {
          while (separators.size() > 0 && last != ',') {
            last = separators.pop();
            str.append(last);
          }
        }
      }

      cur++;
    }

    return str.toString();
  }

  /** The environment. */
  private Env env = null;
  /** The name sequence of the term. */
  private int[] name = null;
  /** The type sequence of the term. */
  private int[] type = null;
  /** The next term indexes of the term. */
  private int[] next = null;
  /** The start index of the term. */
  private int start = 0;
  /** The variable offset of the term. */
  private int offset = 0;
  
  
  public boolean contains(Term term) {

	  int k = 0;
	  for (int i = 0; i<this.type.length; i++){
		  if(this.type[i]==term.getType(i)){
			  //if ((term.getName(i) != null) && k>0)

			  if (this.name[i] == term.getName(i))
				  k++;
			  else 
				  return false;
		  }
	  }
	  return true;
  }




}
