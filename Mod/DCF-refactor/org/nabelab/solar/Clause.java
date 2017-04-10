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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.nabelab.solar.indexing.FVec;
import org.nabelab.solar.indexing.FVecMap;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.parser.Parser;
import org.nabelab.solar.util.ArrayStack;
import org.nabelab.solar.util.Pair;
import org.nabelab.util.IntPair;

/**
 * @author nabesima
 *
 */
public class Clause implements VarHolder, ClauseTypes, TermTypes, DebugTypes, Iterable<Literal> {

  /**
   * Constructs an axiom clause.
   * @param env      the environment.
   * @param literals the set of literals contained in the clause (each literal has to be in the same environment).
   */
  public Clause(Env env, List<Literal> literals) {
    this(env, "an", AXIOM, literals);
  }

  /**
   * Constructs a clause.
   * @param env      the environment.
   * @param name     the name of the clause.
   * @param type     the type of the clause.
   * @param literals the set of literals contained in the clause (each literal has to be in the same environment).
   */
  public Clause(Env env, String name, int type, List<Literal> literals) {
    this(env, name, type, literals, null);
  }

  /**
   * Constructs a clause.
   * @param env      the environment.
   * @param name     the name of the clause.
   * @param type     the type of the clause.
   * @param literals the set of literals contained in the clause (each literal has to be in the same environment).
   * @param origin   the original clause of this clause.
   */
  public Clause(Env env, String name, int type, List<Literal> literals, Clause origin) {
    this.env = env;
    this.name = name;
    this.type = type;
    this.literals = literals.toArray(new Literal[0]);
    this.origin = origin;
  }

  /**
   * Constructs a clause.
   * @param env      the environment.
   * @param name     the name of the clause.
   * @param type     the type of the clause.
   * @param literals the set of literals contained in the clause (each literal has to be in the same environment).
   */
  public Clause(Env env, String name, int type, Literal... literals) {
    this.env = env;
    this.name = name;
    this.type = type;
    this.literals = literals;
  }

  /**
   * Constructs a unit clause.
   * @param env      the environment.
   * @param name     the name of the clause.
   * @param type     the type of the clause.
   * @param literals the set of literals contained in the clause (each literal has to be in the same environment).
   */
  public Clause(Env env, String name, int type, Literal literal) {
    this(env, name, type, literal, null);
  }

  /**
   * Constructs a unit clause.
   * @param env      the environment.
   * @param name     the name of the clause.
   * @param type     the type of the clause.
   * @param literals the set of literals contained in the clause (each literal has to be in the same environment).
   * @param origin   the original clause of this clause.
   */
  public Clause(Env env, String name, int type, Literal literal, Clause origin) {
    this.env = env;
    this.name = name;
    this.type = type;
    this.literals = new Literal[] { literal };
    this.origin = origin;
  }

  /**
   * Constructs a empty clause.
   * @param env  the environment.
   * @param name the name of the clause.
   * @param type the type of the clause.
   */
  protected Clause(Env env, String name, int type) {
    this.env = env;
    this.name = name;
    this.type = type;
  }

  /**
   * Constructs a copy of the specified clause.
   * @param clause  the specified clause.
   */
  public Clause(Clause clause) {
    this.env = clause.env;
    this.name = clause.name;
    this.type = clause.type;
    this.origin = clause;
    this.literals = new Literal[clause.size()];
    for (int i=0; i < literals.length; i++)
      literals[i] = new Literal(clause.literals[i]);
    if (clause.posSymConClause != null)
      this.posSymConClause = new Clause(clause.posSymConClause);
  }

  /**
   * Constructs a copy of the specified clause.
   * @param env     a new environment.
   * @param clause  the specified clause.
   */
  public Clause(Env env, Clause clause) {
  	this.env = env;
    this.name = clause.name;
    this.type = clause.type;
    this.origin = clause;
    this.literals = new Literal[clause.size()];
    for (int i=0; i < literals.length; i++)
      literals[i] = new Literal(env, clause.literals[i]);
    if (clause.posSymConClause != null)
      this.posSymConClause = new Clause(env, clause.posSymConClause);
  }

  /**
   * Constructs an axiom clause.
   * @param env      the environment.
   * @param opt      the options.
   * @param name     the name of the clause.
   * @param type     the type of the clause.
   * @param literals the string representation of literals (ex. "[p(a), -q(b)]")
   */
  public static Clause parse(Env env, Options opt, String literals) throws ParseException {
    return parse(env, opt, "an", AXIOM, literals);
  }

  /**
   * Constructs an axiom clause.
   * @param env    the environment.
   * @param opt    the options.
   * @param name   the name of the clause.
   * @param type   the type of the clause.
   * @param clause the string representation of literals (ex. "[p(a), -q(b)]")
   */
  public static Clause parse(Env env, Options opt, String name, int type, String clause) throws ParseException {
    List<Literal> literals = new Parser(env, opt).literals(new BufferedReader(new StringReader(clause)));
    return new Clause(env, name, type,literals);
  }

  /**
   * Divides this clause into unit clauses.
   * @return a set of unit clauses.
   */
  public List<Clause> divideIntoUnitClauses() {
    ArrayList<Clause> units = new ArrayList<Clause>();
    for (Literal lit : literals) {
      units.add(new Clause(env, name, type, lit));
    }
    return units;
  }

  /**
   * Removes the negative equal(X,Y) literals from this clause.
   * @param out  the output of debug messages.
   * @return the new clause which does not contain -equal(X,Y).
   */
  public Clause removeNegEqXY(PrintWriter out) {
    if (isUnit())
      return this;

    ArrayList<Literal> newLits = new ArrayList<Literal>();
    for (Literal lit : literals)
      newLits.add(lit);

    boolean neverMod = true;
    boolean modified = true;
    while (modified) {
      modified = false;
      for (int i=0; i < newLits.size(); i++) {
        Literal lit = newLits.get(i);
        if (lit.isNegEqualPred()) {
          Term arg1 = lit.getArg(0);
          Term arg2 = lit.getArg(1);
          if (arg1.getStartType() == VARIABLE && arg2.getStartType() == VARIABLE) {
            ArrayList<Literal> tmpLits = new ArrayList<Literal>();
            for (int j=0; j < newLits.size(); j++) {
              if (i == j) continue;
              Literal l = newLits.get(j);
              if (l.contains(arg2))
                tmpLits.add(l.replaceAll(arg2, arg1));
              else
                tmpLits.add(l);
            }
            newLits = tmpLits;
            modified = true;
            neverMod = false;
            break;
          }
        }
      }
    }

    if (neverMod)
      return this;

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    newClause.rename();
    return newClause;
  }

  /**
   * Applies the positive M-modification rule.
   * @param out  the output of debug messages.
   * @return the new clause in M-form.
   */
  public Clause applyPosMonoMod(PrintWriter out, Options opt) {

    int maxVarName = getMaxVarName();
    ArrayList<Literal> newLits = new ArrayList<Literal>();
    for (Literal lit : literals)
      newLits.add(lit);

    boolean neverMod = true;
    boolean modified = true;
    while (modified) {
      modified = false;
      for (int i=0; i < newLits.size(); i++) {
        Literal lit = newLits.get(i);
        Term sub = null;
        if (!lit.isPositive())
          continue;
        if (lit.isEqualPred())
          sub = lit.getNonVarArg(false);
        else if (!lit.isEqualPred())
          sub = lit.getNonVarArg(true);
        if (sub == null)
          continue;

        Term var = Term.createVar(env, ++maxVarName);
        Literal neq = new Literal(env, false, Term.createPredicate(env, EQUAL_PRED, sub, var));

        ArrayList<Literal> tmpLits = new ArrayList<Literal>();
        for (int j=0; j < i; j++)
          tmpLits.add(newLits.get(j));

        tmpLits.add(neq);
        tmpLits.add(lit.replace(sub, var));

        for (int j=i+1; j < newLits.size(); j++)
          tmpLits.add(newLits.get(j));

        newLits  = tmpLits;
        modified = true;
        neverMod = false;
        break;

        // The following code put the new term at last.
//        newLits.set(i, lit.replace(sub, var));
//        newLits.add(neq);

        /*
        // Replaces all occurrences of old non-variable terms with the new
        // variable, and the extracted non-variable term is put at the front of
        // the original term.
        // Sometimes it will generate a pseudo equality predicate whose first
        // argument is a variable and second is not a variable (ex. COL057-1 in
        // S->M->T). In such case, we need to apply negative S-modification
        // again.
        boolean inserted = false;
        ArrayList<Literal> tmpLits = new ArrayList<Literal>();
        for (int j=0; j < newLits.size(); j++) {
          Literal l = newLits.get(j);
          if (l.contains(sub)) {
            if (!inserted) {
              tmpLits.add(neq);
              inserted = true;
            }
            //tmpLits.add(l.replaceAll(sub, var));
            tmpLits.add(l.replace(sub, var));
          }
          else
            tmpLits.add(l);
        }
        newLits  = tmpLits;
        modified = true;
        neverMod = false;
        break;
        */
      }
    }

    if (neverMod)
      return this;

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    if (posSymConClause != null)
      newClause.setPosSymConnClause(new Clause(posSymConClause));
    return newClause;
  }

  /**
   * Applies the negative M-modification rule.
   * @param out  the output of debug messages.
   * @return the new clause in M-form.
   */
  public Clause applyNegMonoMod(PrintWriter out, Options opt) {

    int maxVarName = getMaxVarName();
    ArrayList<Literal> newLits = new ArrayList<Literal>();
    for (Literal lit : literals)
      newLits.add(lit);

    boolean neverMod = true;
    boolean modified = true;
    while (modified) {
      modified = false;
      for (int i=0; i < newLits.size(); i++) {
        Literal lit = newLits.get(i);
        Term sub = null;
        if (!lit.isNegative())
          continue;
        if (lit.isEqualPred() && opt.getEqType() != CFP.EQ_MSNT2 && opt.getEqType() != CFP.EQ_SNMT2 && opt.getEqType() != CFP.EQ_SNMT2A)
          sub = lit.getNonVarArg(false);
        else if (!lit.isEqualPred())
          sub = lit.getNonVarArg(true);
        if (sub == null)
          continue;

        Term var = Term.createVar(env, ++maxVarName);
        Literal neq = new Literal(env, false, Term.createPredicate(env, EQUAL_PRED, sub, var));

        ArrayList<Literal> tmpLits = new ArrayList<Literal>();
        for (int j=0; j < i; j++)
          tmpLits.add(newLits.get(j));

        tmpLits.add(neq);
        tmpLits.add(lit.replace(sub, var));

        for (int j=i+1; j < newLits.size(); j++)
          tmpLits.add(newLits.get(j));

        newLits  = tmpLits;
        modified = true;
        neverMod = false;
        break;

        // The following code put the new term at last.
//        newLits.set(i, lit.replace(sub, var));
//        newLits.add(neq);

        /*
        // Replaces all occurrences of old non-variable terms with the new
        // variable, and the extracted non-variable term is put at the front of
        // the original term.
        // Sometimes it will generate a pseudo equality predicate whose first
        // argument is a variable and second is not a variable (ex. COL057-1 in
        // S->M->T). In such case, we need to apply negative S-modification
        // again.
        // Ex:
        // ORG: [-equal(apply(_0,f(_0)),apply(f(_0),apply(_0,f(_0))))]
        // MOD: [-equal(f(_0),_1), -equal(apply(_0,_1),_2), -equal(_2,apply(_1,_2))]
        boolean inserted = false;
        ArrayList<Literal> tmpLits = new ArrayList<Literal>();
        for (int j=0; j < newLits.size(); j++) {
          Literal l = newLits.get(j);
          if (l.contains(sub)) {
            if (!inserted) {
              tmpLits.add(neq);
              inserted = true;
            }
            tmpLits.add(l.replaceAll(sub, var));
          }
          else
            tmpLits.add(l);
        }
        newLits  = tmpLits;
        modified = true;
        neverMod = false;
        break;
        */
      }
    }

    if (neverMod)
      return this;

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    if (posSymConClause != null)
      newClause.setPosSymConnClause(new Clause(posSymConClause));
    return newClause;
  }

  /**
   * Applies the positive S-modification rule.
   * @param out  the output of debug messages.
   * @return a set of clauses to which the positive S-modification rule was applied.
   */
  public List<Clause> applyPosSymMod(PrintWriter out) {

    ArrayList<ArrayList<Literal>> family = new ArrayList<ArrayList<Literal>>();
    family.add(new ArrayList<Literal>());

    for (Literal lit : literals) {

      if (lit.isPosEqualPred()) {
        // For equality predicates.
        Term arg1 = lit.getArg(0);
        Term arg2 = lit.getArg(1);

        // If two arguments are same, then just add it to clauses.
        if (Term.equals(arg1, arg2)) {
          for (ArrayList<Literal> list : family)
            list.add(new Literal(lit));
          continue;
        }

        Term eq1 = Term.createPredicate(env, EQUAL_PRED, arg1, arg2);
        Term eq2 = Term.createPredicate(env, EQUAL_PRED, arg2, arg1);
        Literal posEq1 = new Literal(env, true, eq1); // +equal(X,Y)
        Literal posEq2 = new Literal(env, true, eq2); // +equal(Y,X)

        ArrayList<ArrayList<Literal>> newFamily = new ArrayList<ArrayList<Literal>>();
        for (ArrayList<Literal> list : family) {
          ArrayList<Literal> list1 = new ArrayList<Literal>();
          ArrayList<Literal> list2 = new ArrayList<Literal>();
          for (Literal element : list) {
            list1.add(new Literal(element));
            list2.add(new Literal(element));
          }
          list1.add(new Literal(posEq1));
          list2.add(new Literal(posEq2));
          newFamily.add(list1);
          newFamily.add(list2);
        }
        family = newFamily;
      }
      else {
        // For non-equality predicates.
        for (ArrayList<Literal> list : family)
          list.add(new Literal(lit));
      }
    }

    ArrayList<Clause> clauses = new ArrayList<Clause>();
    for (ArrayList<Literal> list : family)
      clauses.add(new Clause(env, name, type, list, getOrigin(true)));

    return clauses;
  }

  /**
   * Applies the pseudo positive S-modification rule.
   * @param out  the output of debug messages.
   * @return a set of clauses to which the pseudo positive S-modification rule was applied.
   */
  public List<Clause> applyPseudoPosSymMod(PrintWriter out) {

    assert(env.getVarTable().state() == 0);

    ArrayList<Literal> newLits = new ArrayList<Literal>();
    LinkedList<Clause> clauses = new LinkedList<Clause>();

    // If this is unit equality clause, then generates the new unit clause that is swapped the arguments.
    if (isUnit() && literals[0].isPosEqualPred()) {
      // For equality predicates.
      Term arg1 = literals[0].getArg(0);
      Term arg2 = literals[0].getArg(1);

      // If two arguments are same, then do nothing.
      if (Term.equals(arg1, arg2)) {
        clauses.add(this);
        return clauses;
      }

      Term t1 = Term.createPredicate(env, EQUAL_PRED, arg1, arg2);  // original
      Term t2 = Term.createPredicate(env, EQUAL_PRED, arg2, arg1);  // swapped
      Literal l1 = new Literal(env, true, t1);
      Literal l2 = new Literal(env, true, t2);
      Clause c1 = new Clause(env, name, type, l1, getOrigin(true));
      Clause c2 = new Clause(env, name, type, l2, getOrigin(true));
      clauses.add(c1);
      clauses.add(c2);
      return clauses;
    }

    for (Literal lit : literals) {

      if (lit.isPosEqualPred()) {
        // For equality predicates.
        Term arg1 = lit.getArg(0);
        Term arg2 = lit.getArg(1);

        // If two arguments are same, then just add it to clauses.
        if (Term.equals(arg1, arg2)) {
          newLits.add(new Literal(lit));
          continue;
        }

        // Creates eq(X,Y) and eq(Y,X)
        Term eq1 = Term.createPredicate(env, EQUAL_PRED, arg1, arg2);
        Term eq2 = Term.createPredicate(env, EQUAL_PRED, arg2, arg1);
        Literal posEq1 = new Literal(env, true, eq1); // +equal(X,Y)
        Literal posEq2 = new Literal(env, true, eq2); // +equal(Y,X)

        // Subsumption check: prevents the following generation.
        // C : {-f(A,X,B), -f(A,Y,B), +eq(X,Y)}
        // ------------------------------------
        // C1: {-f(A,X,B), -f(A,Y,B), +eq(X,Y)}
        // C1: {-f(A,X,B), -f(A,Y,B), +eq(Y,X)}
        /*
        {
          ArrayList<Literal> lits1 = new ArrayList<Literal>();
          ArrayList<Literal> lits2 = new ArrayList<Literal>();
          lits1.add(new Literal(posEq1));
          lits2.add(new Literal(posEq2));
          for (Literal l : literals) {
            if (l == lit) continue;
            lits1.add(new Literal(l));
            lits2.add(new Literal(l));
          }

          Clause c1 = new Clause(env, name, type, lits1);
          Clause c2 = new Clause(env, name, type, lits2);
          c2.setOffset(c1.getNumVars());
          if (c1.subsumes(c2)) {
            newLits.add(new Literal(lit));
            continue;
          }
        }
        */

        // Creates a new symmetry connector predicate.
        VarCounter varCounter = env.getVarCounter();
        lit.countVars(varCounter);
        ArrayList<Term> vars = new ArrayList<Term>();
        for (int i=0; i < varCounter.size(); i++)
          vars.add(Term.createVar(env, varCounter.get(i)));
        int symConName = env.getSymTable().createNewPosSrcConnector(vars.size());
        Term symCon1 = Term.createPredicate(env, symConName, vars);
        Term symCon2 = Term.createPredicate(env, symConName, vars);
        Term symCon3 = Term.createPredicate(env, symConName, vars);
        Literal posSymCon  = new Literal(env, true,  symCon1);
        Literal negSymCon1 = new Literal(env, false, symCon2);
        Literal negSymCon2 = new Literal(env, false, symCon3);

        // TEST
        Clause c1 = new Clause(env, name, AXIOM, negSymCon1, posEq1);
        Clause c2 = new Clause(env, name, AXIOM, negSymCon2, posEq2);
        //Clause c1 = new Clause(env, name, AXIOM, posEq1, negSymCon1);
        //Clause c2 = new Clause(env, name, AXIOM, posEq2, negSymCon2);
        ////c1.rename();
        ////c2.rename();
        c1.origin = getOrigin(true);
        c2.origin = getOrigin(true);

        newLits.add(posSymCon);
        clauses.add(c1);
        clauses.add(c2);

//        // Prevents generation of duplicated clauses (this technique was found from SWV339-2).
//        // C:  { eq(a,X) v eq(a,Y) v ... }
//        // ---------------------------
//        // C1: { P1(X) v P2(Y) v ... }
//        // C2: { -P1(X) v eq(a,X) }
//        // C3: { -P1(X) v eq(X,a) }
//        // C4: { -P2(Y) v eq(a,Y) }  <- same as C2
//        // C5: { -P2(Y) v eq(Y,a) }  <- same as C3
//        for (Clause c : clauses) {
//          Literal otherEq = c.get(1);
//          assert(otherEq.isPosEqualPred());
//          if (otherEq.equals(posEq1) || otherEq.equals(posEq2)) {
//            c1 = null;
//            c2 = null;
//            symCon1   = Term.createPredicate(env, c.get(0).getTerm().getStartName(), vars);
//            posSymCon = new Literal(env, true,  symCon1);
//            break;
//          }
//        }
//
//        newLits.add(posSymCon);
//        if (c1 != null)
//          clauses.add(c1);
//        if (c2 != null)
//          clauses.add(c2);
      }
      else {
        // For non-equality predicates.
        newLits.add(new Literal(lit));
      }
    }

    // Prevents generation of duplicated clauses (this technique was found from SWV339-2).
    // C:  { eq(a,X) v eq(a,Y) v ... }             C:  { eq(a,X) v eq(b,a) v ... }
    // -------------------------------             --------------------------------------
    // C1: { P1(X) v P2(Y) v ... }                 C1: { P1(X) v P2 v ... }
    // C2: { -P1(X) v eq(a,X) }                    C2: { -P1(X) v eq(a,X) }
    // C3: { -P1(X) v eq(X,a) }                    C3: { -P1(X) v eq(X,a) }
    // C4: { -P2(Y) v eq(a,Y) }  <- same as C2     C4: { -P2 v eq(b,a) }  <- subsumed by C3
    // C5: { -P2(Y) v eq(Y,a) }  <- same as C3     C5: { -P2 v eq(a,b) }  <- subsumed by C2
    /*
    VarTable varTable = env.getVarTable();
    LinkedList<Clause> newClauses = new LinkedList<Clause>();
    OuterLoop:
    for (int i=0; i < clauses.size(); i+=2) {
      Clause c1 = clauses.get(i  );
      Clause c2 = clauses.get(i+1);
      // TEST
      Literal eq1 = c1.get(1);
      Literal eq2 = c2.get(1);
//      Literal eq1 = c1.get(0);
//      Literal eq2 = c2.get(0);
      assert(eq1.isPosEqualPred() && eq2.isPosEqualPred());

      LinkedList<Clause> otherClauses = new LinkedList<Clause>();
      otherClauses.addAll(newClauses);
      otherClauses.addAll(clauses.subList(i+2, clauses.size()));

      // Checks whether c1 or c2 is NOT subsumed by other clauses.
      for (Clause d : otherClauses) {
        // TEST
        Literal otherEq = d.get(1);
        //Literal otherEq = d.get(0);
        assert(otherEq.isPosEqualPred());
        Subst g = null;
        if (g == null) g = otherEq.isSubsuming(eq1);
        if (g == null) g = otherEq.isSubsuming(eq2);
        if (g != null) {
          // There is no need to generate c1 and c2.
          Literal negSymCon = d.get(0);
          Literal posSymCon = new Literal(negSymCon);
          posSymCon.negate();
          varTable.substitute(g);
          Literal newPosSymCon = posSymCon.instantiate();
          varTable.backtrackTo(0);

          Literal orgNegSymCon = c1.get(0);
          Literal orgPosSymCon = new Literal(orgNegSymCon);
          orgPosSymCon.negate();

          for (int j=0; j < newLits.size(); j++) {
            if (newLits.get(j).equals(orgPosSymCon)) {
              newLits.set(j, newPosSymCon);
              break;
            }
          }

          continue OuterLoop;
        }
      }

      newClauses.add(c1);
      newClauses.add(c2);
    }

    for (Clause c : newClauses) {
      ArrayList<Literal> posSymConLits = new ArrayList<Literal>();
      for (Literal l : newLits)
        posSymConLits.add(new Literal(l));
      c.setPosSymConClause(new Clause(env, name, type, posSymConLits, getOrigin(true)));
      c.rename();
    }

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    newClause.rename();
    newClauses.addFirst(newClause);

    return newClauses;
    */

    for (Clause c : clauses) {
      ArrayList<Literal> posSymConnLits = new ArrayList<Literal>();
      for (Literal l : newLits)
        posSymConnLits.add(new Literal(l));
      c.setPosSymConnClause(new Clause(env, name, type, posSymConnLits, getOrigin(true)));
      c.rename();
    }

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    newClause.rename();
    clauses.addFirst(newClause);

    return clauses;
  }

  /**
   * Applies the general pseudo positive S-modification rule.
   * @param symConName  the name of symmetry connector predicate.
   * @param out         the output of debug messages.
   * @return a set of clauses to which the general pseudo positive S-modification rule was applied. If not changed, then returns null.
   */
  public List<Clause> applyGeneralPseudoPosSymMod(int symConName, PrintWriter out) {

    assert(env.getVarTable().state() == 0);

    ArrayList<Literal> newLits = new ArrayList<Literal>();
    ArrayList<Clause>  clauses = new ArrayList<Clause>();

    // If this is unit equality clause, then generates the new unit clause that is swapped the arguments.
    if (isUnit() && literals[0].isPosEqualPred()) {
      // For equality predicates.
      Term arg1 = literals[0].getArg(0);
      Term arg2 = literals[0].getArg(1);

      // If two arguments are same, then do nothing.
      if (Term.equals(arg1, arg2))
        return null;

      Term t1 = Term.createPredicate(env, EQUAL_PRED, arg1, arg2);  // original
      Term t2 = Term.createPredicate(env, EQUAL_PRED, arg2, arg1);  // swapped
      Literal l1 = new Literal(env, true, t1);
      Literal l2 = new Literal(env, true, t2);
      Clause c1 = new Clause(env, name, type, l1, getOrigin(true));
      Clause c2 = new Clause(env, name, type, l2, getOrigin(true));
      clauses.add(c1);
      clauses.add(c2);
      return clauses;
    }

    boolean modified = false;
    for (Literal lit : literals) {

      if (lit.isPosEqualPred()) {
        // For equality predicates.
        Term arg1 = lit.getArg(0);
        Term arg2 = lit.getArg(1);

        // If two arguments are same, then just add it to clauses.
        if (Term.equals(arg1, arg2)) {
          newLits.add(new Literal(lit));
          continue;
        }

        Term symCon = Term.createPredicate(env, symConName, arg1, arg2);
        Literal posSymCon = new Literal(env, true, symCon);
        newLits.add(posSymCon);
        modified = true;
      }
      else {
        // For non-equality predicates.
        newLits.add(new Literal(lit));
      }
    }

    if (modified) {
      clauses.add(new Clause(env, name, type, newLits, getOrigin(true)));
      return clauses;
    }

    return null;
  }

  /**
   * Applies the negative S-modification rule.
   * @param out  the output of debug messages.
   * @return a clause to which the negative S-modification rule was applied.
   */
  public Clause applyNegSymMod(PrintWriter out) {
    boolean modified = false;
    ArrayList<Literal> newLits = new ArrayList<Literal>();
    for (Literal lit : literals) {
      if (lit.isNegEqualPred()
          && lit.getArg(0).getStartType() == VARIABLE
          && lit.getArg(1).getStartType() != VARIABLE) {
        Term arg1 = lit.getArg(0);
        Term arg2 = lit.getArg(1);
        Term newEq = Term.createPredicate(env, EQUAL_PRED, arg2, arg1);
        Literal newNegEq = new Literal(env, false, newEq);
        newLits.add(newNegEq);
        modified = true;
      }
      else
        newLits.add(lit);
    }

    if (!modified)
      return this;

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    if (posSymConClause != null)
      newClause.setPosSymConnClause(new Clause(posSymConClause));
    return newClause;
  }

  /**
   * Applies the positive T-modification rule.
   * @param out  the output of debug messages.
   * @return a clause to which the positive T-modification rule was applied.
   */
  public Clause applyPosTrnMod(PrintWriter out) {
    boolean modified = false;
    int maxVarName = getMaxVarName();
    ArrayList<Literal> newLits = new ArrayList<Literal>();
    for (Literal lit : literals) {
      if (lit.isPosEqualPred() && lit.getArg(1).getStartType() != VARIABLE) {
        Term arg1 = lit.getArg(0);
        Term arg2 = lit.getArg(1);
        Term var = Term.createVar(env, ++maxVarName);

        Term eq1 = Term.createPredicate(env, EQUAL_PRED, arg1, var);
        Term eq2 = Term.createPredicate(env, EQUAL_PRED, arg2, var);
        Literal newPosEq = new Literal(env, true,  eq1);
        Literal newNegEq = new Literal(env, false, eq2);
        newLits.add(newPosEq);
        newLits.add(newNegEq);
        modified = true;
      }
      else
        newLits.add(lit);
    }

    if (!modified)
      return this;

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    if (posSymConClause != null)
      newClause.setPosSymConnClause(new Clause(posSymConClause));
    return newClause;
  }

  /**
   * Applies the negative T-modification rule.
   * @param out  the output of debug messages.
   * @return a clause to which the negative T-modification rule was applied.
   */
  public Clause applyNegTrnMod(PrintWriter out) {
    boolean modified = false;
    int maxVarName = getMaxVarName();
    ArrayList<Literal> newLits = new ArrayList<Literal>();
    for (Literal lit : literals) {
      if (lit.isNegEqualPred()
          && lit.getArg(0).getStartType() != VARIABLE
          && lit.getArg(1).getStartType() != VARIABLE
          && !lit.getArg(1).isSkolemConstant()) {
        Term arg1 = lit.getArg(0);
        Term arg2 = lit.getArg(1);
        Term var = Term.createVar(env, ++maxVarName);

        Term eq1 = Term.createPredicate(env, EQUAL_PRED, arg1, var);
        Term eq2 = Term.createPredicate(env, EQUAL_PRED, arg2, var);
        Literal newNegEq1 = new Literal(env, false, eq1);
        Literal newNegEq2 = new Literal(env, false, eq2);
        newLits.add(newNegEq1);
        newLits.add(newNegEq2);
        modified = true;
      }
      else
        newLits.add(lit);
    }

    if (!modified)
      return this;

    Clause newClause = new Clause(env, name, type, newLits, getOrigin(true));
    if (posSymConClause != null)
      newClause.setPosSymConnClause(new Clause(posSymConClause));
    return newClause;
  }

  /**
   * Constructs a copy of the specified clause and offset.
   * @param clause  the clause to copy.
   * @param offset  the variable offset.
   * @return the new clause.
   */
  public static Clause newOffset(Clause clause, int offset) {
    Clause newClause = new Clause(clause.env, clause.name, clause.type);
    newClause.origin  = clause;
    newClause.id      = clause.id;
    newClause.fvec    = clause.fvec;
    newClause.numSyms = clause.numSyms;
    newClause.numVars = clause.numVars;
    newClause.numExts = clause.numExts;

    newClause.literals = new Literal[clause.literals.length];
    for (int i=0; i < clause.literals.length; i++)
      newClause.literals[i] = Literal.newOffset(clause.literals[i], offset);

    clause.copyProperties(newClause);

    return newClause;
  }

  /**
   * Returns a new clause in which all variables are replaced with the values.
   * @return a new clause in which all variables are replaced with the values.
   */
  public Clause instantiate() {
    Clause newClause = new Clause(env, name, type);
    newClause.origin = this.origin;
    newClause.literals = new Literal[literals.length];
    for (int i = 0; i < literals.length; i++)
      newClause.literals[i] = literals[i].instantiate();

    copyProperties(newClause);

    return newClause;
  }

  /**
   * Copy properties of this clause to the specified clause.
   * @param dest  a destination clause.
   */
  protected void copyProperties(Clause dest) {
    if (posSymConClause != null)
      dest.posSymConClause = posSymConClause.instantiate();

    if (compUnifIdxs == null)
      initCompUnifiableLiterals();
    dest.compUnifIdxs = compUnifIdxs;    // Shares the indexes of unifiable literals.
    dest.getCompUnifiableLiterals();     // Updates the pairs by using new generated literals.

    dest.unitSubIdxs = unitSubIdxs;      // Shares the indexes of unit subsumption candidate literals.
    dest.getUnitSubsumptionCandidates(); // Updates the candates by using new generated literals.
  }

  /**
   * Renames the variables. No variable in this clause must have a value.
   */
  public void rename() {
    rename(env.getVarRenameMap());
  }

  /**
   * Renames the variables using the specified rename-mapping. No variable in
   * this clause must have a value.
   * @param renameMap  the rename-mapping.
   */
  public void rename(VarRenameMap renameMap) {
    for (Literal literal : literals)
      literal.rename(renameMap);
    if (posSymConClause != null)
      posSymConClause.rename(renameMap);
  }

  /**
   * Renames the variables using the specified rename-mapping. No variable in
   * this clause must have a value.
   * @param renameMap  the rename-mapping.
   * @param minVarName the smallest variable name to be renamed.
   * @param minVarName the largest variable name to be renamed.
   */
  public void subrename(VarRenameMap renameMap, int minVarName, int maxVarName) {
    for (Literal literal : literals)
      literal.subrename(renameMap, minVarName, maxVarName);
  }

  /**
   * Returns the name of this clause.
   * @return the name of this clause.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the type of this clause.
   * @return the type of this clause.
   */
  public int getType() {
    return type;
  }

  /**
   * Sets the type of this clause.
   * @param type the new type.
   */
  public void setType(int type) {
    this.type = type;
  }

  /**
   * Returns the identifier of this clause.
   * @return the identifier of this clause.
   */
  public int getID() {
    return id;
  }

  /**
   * Sets the identifier of this clause.
   * @param id  the new identifier.
   */
  public void setID(int id) {
    this.id = id;
  }

  /**
   * Sets the variable offset of this clause.
   * @param offset  the variable offset.
   */
  public void setOffset(int offset) {
    for (Literal lit : literals)
      lit.setOffset(offset);
  }

  /**
   * Returns the literal at the specified position.
   * @param index  the specified position.
   * @return the literal at the specified position.
   */
  public Literal get(int index) {
    return literals[index];
  }

  /**
   * Returns the set of literals of this clause.
   * @return the set of literals of this clause.
   */
  public List<Literal> getLiterals() {
    return Arrays.asList(literals);
  }

  /**
   * Returns an iterator over the literals in this clause.
   * @return an iterator over the literals in this clause.
   */
  public Iterator<Literal> iterator() {
    return new ClauseIterator();
  }

  /**
   * Returns the number of kinds of variables contained in this clause.
   * @return the number of kinds of variables contained in this clause.
   */
  public int getNumVars() {
    if (numVars == -1) {
      VarCounter varCounter = env.getVarCounter();
      for (Literal lit : literals)
        lit.countVars(varCounter);
      numVars = varCounter.size();
    }
    return numVars;
  }

  /**
   * Returns the maximum variable name in this clause.
   * @return the maximum variable name in this clause.
   */
  public int getMaxVarName() {
    VarCounter varCounter = env.getVarCounter();
    for (Literal lit : literals)
      lit.countVars(varCounter);
    return varCounter.getMaxVarName();
  }

  /**
   * Return the number of symbols contained in this clause.
   * @param update  if true, then recalculates the number contained in symbols.
   * @return the number of symbols contained in this clause.
   */
  public int getNumSyms(boolean update) {
    if (numSyms == -1 || update) {
      numSyms = 0;
      for (Literal lit : literals)
        numSyms += lit.getNumSyms(update);
    }
    return numSyms;
  }

  /**
   * Returns the number of extendable clauses with literals in this clause.
   * @param update    if true, then recalculates the number of extendable clauses with literals in this clause.
   * @param clauseDB  the input clause database.
   * @return the number of extendable clauses with literals in this clause.
   */
  public int getNumExts(boolean update, ClauseDB clauseDB) {
    if (numExts == -1 || update) {
      numExts = 0;
      for (Literal lit : literals)
        numExts += lit.getNumExts(update, clauseDB);
//      for (Literal lit : literals) {
//        if (!(lit.isNegative() && lit.isConnPred()) || posSymConClause == null)
//          numExts += lit.getNumExts(update, clauseDB);
//        else {
//          int num = posSymConClause.getNumExts(update, clauseDB);
//          lit.setNumExts(num);
//          numExts += num;
//        }
//      }
    }
    return numExts;
  }

  /**
   * Sets the positive symmetry connected clause.
   * @param posSymConClause the positive symmetry connected clause.
   */
  public void setPosSymConnClause(Clause posSymConClause) {
    this.posSymConClause = posSymConClause;
  }

  /**
   * Finds out complementary unifiable literal pairs in this clause.
   * @return true if this clause is not tautology.
   */
  public boolean initCompUnifiableLiterals() {
    // Calculates the indexes.
    compUnifIdxs = new ArrayList<IntPair>();
    for (int i = 0; i < literals.length; i++) {
      for (int j = i + 1; j < literals.length; j++) {
        Subst g = literals[i].isCompUnifiable(literals[j]);
        if (g == null)
          continue;
        if (g.isEmpty())
          return false;
        compUnifIdxs.add(new IntPair(i, j));
      }
    }
    // Clear the unifiable pairs (they are calculated when
    // 'getCompUnifiableLiterals()' is called).
    compUnifPairs = null;
    return true;
  }

  /**
   * Returns the set of complementary unifiable literal pairs in this clause.
   * @return the set of complementary unifiable literal pairs in this clause.
   */
  public List<Pair<Literal, Literal>> getCompUnifiableLiterals() {
    if (compUnifPairs != null)
      return compUnifPairs;
    if (compUnifIdxs == null)
      if (!initCompUnifiableLiterals())
        return null;
    if (compUnifIdxs.size() == 0)
      return null;
    compUnifPairs = new ArrayList<Pair<Literal, Literal>>();
    for (int i=0; i < compUnifIdxs.size(); i++) {
      IntPair p = compUnifIdxs.get(i);
      compUnifPairs.add(new Pair<Literal, Literal>(literals[p.get1st()],
          literals[p.get2nd()]));
    }
    return compUnifPairs;
  }

  /**
   * Finds out unifiable literals with the specified unit clause.
   * @param unit  the unit clause to be checked.
   * @return true if the unit clause has possibility for unit subsumption.
   */
  public boolean initUnitSubsumptionCechking(Clause unit) {
    assert (unit.isUnit());
    if (this == unit)
      return false;
    // Calculates the indexes.
    boolean added = false;
    Literal ulit = unit.get(0);
    for (int i=0; i < literals.length; i++) {
      if (ulit.isUnifiable(literals[i]) != null) {
        if (unitSubIdxs == null)
          unitSubIdxs = new ArrayList<UnitSubIdx>();
        unitSubIdxs.add(new UnitSubIdx(i, ulit));
        added = true;
      }
    }
    // Clear the unifiable pairs (they are calculated when
    // 'getCompUnifiableLiterals()' is called).
    unitSubLits = null;

    return added;
  }

  /**
   * Returns the set of complementary unifiable literal pairs in this clause.
   * @return the set of complementary unifiable literal pairs in this clause.
   */
  public List<Pair<Literal, Literal>> getUnitSubsumptionCandidates() {
    if (unitSubLits != null)
      return unitSubLits;
    if (unitSubIdxs == null)
      return null;
    if (unitSubIdxs.size() == 0)
      return null;
    unitSubLits = new ArrayList<Pair<Literal, Literal>>();
    for (UnitSubIdx p : unitSubIdxs)
      unitSubLits.add(new Pair<Literal, Literal>(literals[p.getIndex()], p.getUnitLiteral()));
    return unitSubLits;
  }

  /**
   * Return the origin clause of this clause.
   * @param deep if true, then look up the deepest origin of this clause.
   * @return the origin clause of this clause.
   */
  public Clause getOrigin(boolean deep) {
    if (origin == null)
      return this;
    if (!deep)
      return origin;
    Clause c = origin;
    while (c.origin != null)
      c = c.origin;
    return c;
  }

  /**
   * Returns the number of literals.
   * @return the number of literals.
   */
  public int size() {
    return literals.length;
  }

  /**
   * Returns true if this is a positive clause.
   * @return true if this is a positive clause.
   */
  public boolean isPositive() {
    for (Literal lit : literals)
      if (lit.isNegative())
        return false;
    return true;
  }

  /**
   * Returns true if this is a negative clause.
   * @return true if this is a negative clause.
   */
  public boolean isNegative() {
    for (Literal lit : literals)
      if (lit.isPositive())
        return false;
    return true;
  }

  /**
   * Returns true if this clause is ground.
   * @return true if this clause is ground.
   */
  public boolean isGround() {
    for (Literal lit : literals)
      if (!lit.isGround())
        return false;
    return true;
  }

  /**
   * Returns true if this is a unit clause.
   * @return true if this is a unit clause.
   */
  public boolean isUnit() {
    return size() == 1;
  }

  /**
   * Returns true if this is a empty clause.
   * @return true if this is a empty clause.
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns true if this clause contains a bridge predicate.
   * @return true if this clause contains a bridge predicate.
   */
  public boolean hasBridgePred() {
    SymTable symTable = env.getSymTable();
    for (Literal lit : literals) {
      Term term = lit.getTerm();
      if (symTable.hasTag(term.getStartName(), term.getStartType(), BRIDGE))
        return true;
    }
    return false;
  }

  /**
   * Returns the specified literal if it exists.
   * @param name     the name of the literal.
   * @param positive the sign of the literal.
   * @return the specified literal that is found firstly.
   */
  public Literal find(int name, boolean positive) {
    for (Literal lit : literals)
      if (lit.isPositive() == positive && lit.getName() == name)
        return lit;
    return null;
  }

  /**
   * Returns true if this clause subsumes the specified clause.
   * @param c2  the specified clause.
   * @return true if this clause subsumes the specified clause.
   */
  public boolean subsumes(Clause c2) {
    Clause c1 = this;
    if (c1.literals.length == 0)
      return true;
    if (c1.literals.length > c2.literals.length)
      return false;

    // Pre-filtering of subsumption checking.
    int i = 0;
    int j = 0;
    if (env.getFVecMap() == null || env.getFVecMap().hasUncheckedPredOcc()) {
      while (i < c1.literals.length) {
        Literal l1 = c1.literals[i];
        j = 0;
        while (j < c2.literals.length) {
          Literal l2 = c2.literals[j];
          if (l1.getName() == l2.getName() && l1.getSign() == l2.getSign())
            break;
          j++;
        }
        if (j == c2.literals.length)
          return false;
        i++;
      }
    }

    VarTable varTable = env.getVarTable();
    int orgState = varTable.state();
    ArrayStack<Cand> candStack = env.getClauseCandStack();
    i = j = 0;
    while (true) {
      CANDIDATE: while (true) {
        Literal l1 = c1.literals[i];
        while (true) {
          Literal l2 = c2.literals[j];
          int state = varTable.state();
          if (l1.subsumes(l2, c2) != null) {
            // Saves the another candidate.
            if (j + 1 < c2.literals.length)
              candStack.push(new Cand(i, j + 1, state));
            j = 0;
            break;
          }
          j++;
          if (j == c2.literals.length)
            break CANDIDATE;
        }
        i++;
        if (i == c1.literals.length) {
          varTable.backtrackTo(orgState);
          return true;
        }
      }
      if (candStack.isEmpty()) {
        varTable.backtrackTo(orgState);
        return false;
      }
      Cand cand = candStack.pop();
      i = cand.getCur1();
      j = cand.getCur2();
      varTable.backtrackTo(cand.getState());
    }
  }

  /**
   * Returns -1 if this object contains the specified variable.
   * @param varname  the variable number to check. It should be the last variable of a variable-chain.
   * @return -1 if this object contains the specified variable. Otherwise, returns the number of symbols in the object.
   */
  public int containsVar(int varname) {
    int syms = 0;
    for (int i = 0; i < literals.length; i++) {
      int ret = literals[i].getTerm().containsVar(varname);
      if (ret == -1)
        return -1;
      syms += ret;
    }
    return syms;
  }

  /**
   * Returns true if this clause subsumes the specified clause.
   * @param c2  the specified clause.
   * @return true if this clause subsumes the specified clause.
   */
  public static boolean equals(Clause c1, Clause c2) {
    return c1.subsumes(c2) && c2.subsumes(c1);
  }

  /**
   * Sets the necessity of this clause.
   * @param isNecessary true if this clause is necessary.
   */
  public void setNecessity(boolean isNecessary) {
    this.isNecessary = isNecessary;
  }

  /**
   * Returns the necessity of this clause.
   * @return the necessity of this clause.
   */
  public boolean isNecessary() {
    return isNecessary;
  }

  /**
   * Returns true if this is the reflective equal axiom.
   * @return true if this is the reflective equal axiom.
   */
  public boolean isEqReflectAxiom() {
    if (!isUnit())
      return false;
    Literal lit = literals[0];
    if (!lit.isPosEqualPred() || !lit.isMaxGeneral())
      return false;
    Term arg1 = lit.getArg(0);
    Term arg2 = lit.getArg(1);
    return arg1.isVar() && arg2.isVar() && arg1.getVarName() == arg2.getVarName();
  }

  /**
   * Returns true if this clause is a predicate monotonicity axiom.
   * @return true if this clause is a predicate monotonicity axiom.
   */
  public boolean isPredMonoAxiom() {
    // Is a predicate monotonicity axiom?
    if (size() != 3)
      return false;

    // Ex: [-equal(X,Y), -p(X,T1,T2), +p(Y,T1,T2)]
    Literal negEqXY  = null;
    Literal negPredX = null;
    Literal posPredY = null;
    for (int i=0; i < literals.length; i++) {
      Literal lit = literals[i];
      if (lit.isNegEqualPred() && lit.isMaxGeneral())
        negEqXY = lit;
      else if (!lit.isEqualPred() && lit.isNegative() && lit.isMaxGeneral())
        negPredX = lit;
      else if (!lit.isEqualPred() && lit.isPositive() && lit.isMaxGeneral())
        posPredY = lit;
    }
    if (negEqXY == null || negPredX == null || posPredY == null || negPredX.getName() != posPredY.getName())
      return false;

    int varX = negEqXY.getArg(0).getVarName();
    int varY = negEqXY.getArg(1).getVarName();
    int arity = negPredX.getArity();
    boolean hasXY = false;
    for (int i=0; i < arity; i++) {
      Term arg1 = negPredX.getArg(i);
      Term arg2 = posPredY.getArg(i);
      if (arg1.isVar() && arg2.isVar()) {
        // -eq(X,Y), -p(..., V, ...), +p(..., V, ...)
        if (arg1.getVarName() == arg2.getVarName())
          continue;
        // -eq(X,Y), -p(..., X, ...), +p(..., Y, ...)
        if (arg1.getVarName() == varX && arg2.getVarName() == varY && !hasXY) {
          hasXY = true;
          continue;
        }
        // -eq(X,Y), -p(..., Y, ...), +p(..., X, ...)
        if (arg1.getVarName() == varY && arg2.getVarName() == varX && !hasXY) {
          hasXY = true;
          continue;
        }
        return false;
      }
      else if (Term.equals(arg1, arg2))
        continue;
      return false;
    }

    return hasXY;
  }

  /**
   * Returns true if this clause is a function monotonicity axiom.
   * @return true if this clause is a function monotonicity axiom.
   */
  public boolean isFuncMonoAxiom() {
    // Is a function monotonicity axiom?
    if (size() != 2)
      return false;

    // Ex: [-equal(X,Y), +equal(foo(X,A,B),foo(Y,A,B))]
    Literal negEq = null;
    Term    funcX = null;
    Term    funcY = null;
    for (int i=0; i < literals.length; i++) {
      Literal lit = literals[i];
      if (lit.isNegEqualPred() && lit.isMaxGeneral())
        negEq = lit;
      else if (lit.isPosEqualPred()) {
        funcX = lit.getArg(0);
        funcY = lit.getArg(1);
      }
    }
    if (negEq == null || funcX == null || funcY == null)
      return false;
    if (funcX.getStartType() != FUNCTION || funcY.getStartType() != FUNCTION || funcX.getStartName() != funcY.getStartName())
      return false;

    int varX = negEq.getArg(0).getVarName();
    int varY = negEq.getArg(1).getVarName();
    int arity = funcX.getArity();
    boolean hasXY = false;
    for (int i=0; i < arity; i++) {
      Term l = funcX.getArg(i);
      Term r = funcY.getArg(i);
      if (l.isVar() && r.isVar()) {
        // -eq(X,Y), +eq(foo(..., V, ...), foo(..., V, ...))
        if (l.getVarName() == r.getVarName())
          continue;
        // -eq(X,Y), +eq(foo(..., X, ...), foo(..., Y, ...))
        if (l.getVarName() == varX && r.getVarName() == varY && !hasXY) {
          hasXY = true;
          continue;
        }
        // -eq(X,Y), +eq(foo(..., Y, ...), foo(..., X, ...))
        if (l.getVarName() == varY && r.getVarName() == varX && !hasXY) {
          hasXY = true;
          continue;
        }
        return false;
      }
      else if (Term.equals(l, r))
        continue;
    }

    return hasXY;
  }

  /**
   * Sorts the literals using the specified comparator.
   * @param comp  the specified comparator.
   */
  public void sort(LitOrder order) {
    sort(order, 0);
  }

  /**
   * Sorts the literals using the specified comparator.
   * @param comp  the specified comparator.
   * @param from  the index of the first element (inclusive) to be sorted.
   */
  public void sort(LitOrder order, int from) {
    Comparator<Literal> comparator = order.comparator();
    if (size() < INSERTIONSORT_THRESHOLD) {
      for (int i = from; i < size(); i++)
        for (int j = i; j > from && comparator.compare(literals[j - 1], literals[j]) > 0; j--)
          swap(j, j - 1);
    } else
      Arrays.sort(literals, from, literals.length, comparator);
    // The indexes are obsolete.
    compUnifIdxs = null;
    unitSubIdxs = null;
  }

  /**
   * Swaps two specified literals.
   * @param x  the position of a literal.
   * @param y  the position of a literal.
   */
  public void swap(int x, int y) {
    Literal tmp = literals[x];
    literals[x] = literals[y];
    literals[y] = tmp;
  }

  /**
   * Updates and returns a feature vector.
   * @param update  if true, then updates the feature vector.
   * @returns the feature vector.
   */
  public FVec getFVec(boolean update) {
    if (update || fvec == null || fvecMap != env.getFVecMap()) {
      fvecMap = env.getFVecMap();
      fvec = new FVec(fvecMap);
      for (Literal lit : literals)
        lit.getFVec(update, fvec);
    }
    return fvec;
  }

  /**
   * Returns the raw feature vector of this clause.
   * @param raw  the raw feature vector of this clause (output).
   */
  public void getRawFVec(int[] raw) {
    for (Literal lit : literals)
      lit.getRawFVec(raw);
  }

  // /**
  // * Returns the feature vector.
  // * @returns the feature vector.
  // */
  // public FVec getFVec() {
  // return getFVec(false);
  // }

  /**
   * Returns the hash code value of this object.
   * @return the hash code value of this object.
   */
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(literals);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + type;
    return result;
  }

  /**
   * Compares the specified object with this object for equality.
   * @param obj  the reference object with which to compare.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Clause other = (Clause) obj;
    if (!Arrays.equals(literals, other.literals))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (type != other.type)
      return false;
    return true;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    String str = Arrays.toString(literals);
    if (env.dbg(DBG_LIT_ORDER))
      str += "<" + numSyms + "s/" + numExts + "e>";
    return str;
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toSimpString() {
    return toSimpString(0);
  }

  /**
   * Returns a string representation of this object (used variable names instead
   * of its values)
   * @param offset  a variable offset.
   * @return a string representation of this object.
   */
  public String toSimpString(int offset) {
    StringBuilder str = new StringBuilder("[");
    for (int i = 0; i < literals.length; i++) {
      str.append(literals[i].toSimpString());
      if (i < literals.length - 1)
        str.append(", ");
    }
    str.append("]");
    return str.toString();
  }

  /** The environment. */
  protected Env env = null;
  /** The origin clause of this clause. */
  protected Clause origin = null;
  /** The name of this clause. */
  protected String name = null;
  /** The type of this clause. */
  protected int type = AXIOM;
  /** The integer identifier of this clause. */
  protected int id = 0;
  /** The elements of this clause. */
  protected Literal[] literals = null;
  /** The feature vector mapping. */
  protected FVecMap fvecMap = null;
  /** The feature vector of this clause. */
  protected FVec fvec = null;
  /** The number of symbols contained in this literal. */
  private int numSyms = -1;
  /** The maximum variable name in this literal. */
  private int numVars = -1;
  /** The (maximum) number of extendable clauses with literals in this clause. */
  private int numExts = -1;
  /** The positive symmetry connected clause. */
  private Clause posSymConClause = null;
  /** The complementary unifiable literal indexes. */
  private ArrayList<IntPair> compUnifIdxs = null;
  /** The complementary unifiable literal pairs. */
  private ArrayList<Pair<Literal, Literal>> compUnifPairs = null;
  /** The unit subsumption candidate indexes. */
  private ArrayList<UnitSubIdx> unitSubIdxs = null;
  /** The unit subsumption candidate literals. */
  private ArrayList<Pair<Literal, Literal>> unitSubLits = null;
  /** The necessity of this clause */
  private boolean isNecessary = true;

  /** The threshold value for sorting. */
  private static final int INSERTIONSORT_THRESHOLD = 7;

  /** The candidate in subsumption checking */
  public final static class Cand {
    public Cand(int cur1, int cur2, int state) {
      this.cur1 = cur1;
      this.cur2 = cur2;
      this.state = state;
    }

    public int getCur1() {
      return cur1;
    }

    public int getCur2() {
      return cur2;
    }

    public int getState() {
      return state;
    }

    private int cur1  = 0;
    private int cur2  = 0;
    private int state = 0;
  }

  /** The unit subsumption target and literal. */
  private final static class UnitSubIdx {
    public UnitSubIdx(int index, Literal unitlit) {
      this.index = index;
      this.unitlit = unitlit;
    }

    public int getIndex() {
      return index;
    }

    public Literal getUnitLiteral() {
      return unitlit;
    }

    private int     index   = 0;
    private Literal unitlit = null;
  }

  private class ClauseIterator implements Iterator<Literal> {

    public boolean hasNext() {
      return index < literals.length;
    }

    public Literal next() {
      return literals[index++];
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    private int index = 0;
  }

}
