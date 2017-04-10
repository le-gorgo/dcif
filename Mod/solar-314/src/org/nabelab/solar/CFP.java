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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.nabelab.mining.DB;
import org.nabelab.mining.FreqItemSet;
import org.nabelab.mining.ItemSet;
import org.nabelab.mining.Miner;
import org.nabelab.mining.PrefixSpan;
import org.nabelab.solar.equality.TermWeight;
import org.nabelab.solar.indexing.FVecTrie;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.parser.Parser;
import org.nabelab.solar.pfield.PFCardConstraint;
import org.nabelab.solar.pfield.PField;
//import org.nabelab.solar.pfield.PFieldChecker;
import org.nabelab.solar.pfield.PFieldCheckerCardConstraints;
import org.nabelab.solar.simp.TermIntMap;
import org.nabelab.solar.util.IntHashSet;
import org.nabelab.solar.util.Pair;
import org.nabelab.util.IntArraySet;
import org.nabelab.util.IntIterator;
import org.nabelab.util.IntSet;

/**
 * @author nabesima
 *
 */
public class CFP implements ClauseTypes, TermTypes, ExitStatus, OptionTypes, DebugTypes, Iterable<Clause> {

  /**
   * Constructs a empty consequence finding problem.
   * @param env the environment.
   * @param opt the options.
   */
  public CFP(Env env, Options opt) {
    this.env      = env;
    this.opt      = opt;
    this.pfield   = new PField(env, opt);
    this.strategy = new Strategy(this);
    this.litOrder = new LitOrder(this);
    this.opOrder  = new OpOrder(this);
  }

  /**
   * Parses a consequence finding problem from the specified reader.
   * @param reader the reader.
   * @param source the name of the source file.
   * @param base   the base directory for the "include" directives.
   */
  public void parse(Reader reader, String source, String base) throws ParseException {
    if (parser == null)
      parser = new Parser(env, opt, this);

    this.problemName = source;
    parser.parse(reader, source, base);

    // Updates the maximum length condition.
    if (opt.getMaxLenConseqs() != PField.NOT_DEFINED)
      pfield.setMaxLength(opt.getMaxLenConseqs());
  }

  /**
   * Parses a consequence finding problem from the specified file.
   * @param file the consequence finding problem file name.
   * @param base the base directory for the "include" directives.
   */
  public void parse(File file, String base) throws FileNotFoundException, IOException, ParseException {
    Reader reader = null;
    if (file.getName().endsWith(".gz"))
      reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
    else
      reader = new BufferedReader(new FileReader(file));
    parse(reader, file.getName(), base);
  }

  /**
   * Parses a consequence finding problem from the specified file.
   * @param file the consequence finding problem file name.
   */
  public void parse(File file) throws FileNotFoundException, IOException, ParseException {
    parse(file, null);
  }

  /**
   * Parses a consequence finding problem from the specified string.
   * @param description the consequence finding problem description.
   */
  public void parse(String description) throws ParseException {
    parse(new BufferedReader(new StringReader(description)), null, null);
  }

  /**
   * Parses a literal  from the specified string.
   * @param description the literal description.
   */
  public Literal parseLiteral(String description) throws ParseException {
    if (parser == null)
      parser = new Parser(env, opt, this);
    return parser.literal(new BufferedReader(new StringReader(description)));
  }

  /**
   * Parses a term from the specified string.
   * @param description the term description.
   */
  public Term parseTerm(String description) throws ParseException {
    if (parser == null)
      parser = new Parser(env, opt, this);
    return parser.term(new BufferedReader(new StringReader(description)));
  }

  /**
   * Returns the name of this problem.
   * @return the name of this problem.
   */
  public String getName() {
    return problemName;
  }

  /**
   * Adds the specified clause to this problem.
   * @param clause the specified clause to be added.
   */
  public void addClause(Clause clause) {
    clauses.add(clause);
    if (clause.getType() == TOP_CLAUSE)
      topClauses.add(clause);
  }

  /**
   * Returns the set of clauses.
   * @return the set of clauses.
   */
  public List<Clause> getClauses() {
    return clauses;
  }

  /**
   * Returns the number of clauses.
   * @return the number of clauses.
   */
  public int getNumClauses() {
    return clauses.size();
  }

  /**
   * Returns the total number of literals.
   * @return the total number of literals.
   */
  public int getNumLiterals() {
    int num = 0;
    for (Clause c : clauses)
      num += c.size();
    return num;
  }

  /**
   * Returns the top clause at the specified position.
   * @param index the index of the element to return
   * @return the top clause at the specified position.
   */
  public Clause getTopClause(int index) {
    return topClauses.get(index);
  }

  /**
   * Returns the set of top clauses.
   * @return the set of top clauses.
   */
  public List<Clause> getTopClauses() {
    return topClauses;
  }

  /**
   * Returns true if this problem has a single divisible top clause.
   * @return true if this problem has a single divisible top clause.
   */
  public boolean hasSingleDivisibleTopClause() {
    if (topClauses.size() != 1)
      return false;
    Clause topClause = topClauses.get(0);
    if (topClause.isUnit())
      return false;

    return topClause.isGround();
  }

  /**
   * Returns the set of unit clauses for unit axiom matching.
   * @return the set of unit clauses for unit axiom matching.
   */
  public List<Clause> getUnitsForMatching() {
    return unitsForMatching;
  }

  /**
   * Returns the production field.
   * @return the production field in this problem.
   */
  public PField getPField() {
    return pfield;
  }

  /**
   * Sets the production field.
   * @param pfield the production field to be set.
   */
  public void setPField(PField pfield) {
    this.pfield = pfield;
  }

  /**
   * Returns the search strategy.
   * @return the search strategy in this problem.
   */
  public Strategy getStrategy() {
    return strategy;
  }

  /**
   * Adds the specified consequence.
   * @param conseq the specified consequence.
   * @return true if the specified consequence is added.
   */
  public boolean addConseq(Conseq conseq) {
    if (conseqSet == null)
      conseqSet = new ConseqSet(env);
    if (conseq.isEmpty())
      setStatus(UNSATISFIABLE);
    return conseqSet.add(conseq, env.getSOLARListers());
  }

  /**
   * Adds the specified set of consequences.
   * @param conseq  a set of consequences to be added.
   */
  public void addConseqs(ConseqSet conseqs) {
    for (Conseq conseq : conseqs)
      addConseq(conseq);
  }

  /**
   * Returns true if there is an empty consequence.
   * @return true if there is an empty consequence.
   */
  public boolean hasEmptyConseq() {
    if (conseqSet == null)
      conseqSet = new ConseqSet(env);
    return conseqSet.hasEmptyClause();
  }

  /**
   * Returns the found consequences.
   * @return the found consequences.
   */
  public ConseqSet getConseqSet() {
    if (conseqSet == null)
      conseqSet = new ConseqSet(env);
    return conseqSet;
  }

  /**
   * Sets the set of characteristic clauses.
   * @param carcSet  the set of characteristic clauses.
   */
  public void setCarcSet(ConseqSet carcSet) {
    this.carcSet = carcSet;
  }

  /**
   * Clears the consequences.
   */
  public void clearConseqs() {
    conseqSet = null;
  }

  /**
   * Returns the status of this problem.
   * @return the status of this problem.
   */
  public int getStatus() {
    return status;
  }

  /**
   * Sets the status of this problem.
   * @param status the status of this problem.
   */
  public void setStatus(int status) {
    this.status = status;
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
   * Returns the options.
   * @return the options.
   */
  public Options getOptions() {
    return opt;
  }

  /**
   * Return the type of this consequence finding problem.
   * @return the type of this consequence finding problem.
   */
  public int getProblemType() {
    return problemType;
  }

  /**
   * Converts the unstable production field into the stable one.
   * @param out the output of debug messages.
   */
  public void convertToStablePField(PrintWriter out) {
    pfield.convertToStablePField(this, out);
  }

  /**
   * Converts the found consequences into the original literal format.
   */
  public void convertConqsToOrgFmt() throws ParseException {
    conseqSet = pfield.convertToOrgFmt(conseqSet);
  }

  /**
   * Converts this problem into the no-equality format.
   * @param out the output of debug messages.
   */
  public void convertToNoEqualityFormat(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION)) {
      out.println("[Original problem]");
      for (Clause c : clauses)
        out.println(" " + c);
    }

    // Removes monotonicity axioms for predicates and functions.
    {
      if (env.dbg(DBG_MODIFICATION))
        out.println("[Remove monotonicity axioms]");
      ArrayList<Clause> newClauses = new ArrayList<Clause>();
      for (Clause c : clauses) {
        if (!c.isPredMonoAxiom() && !c.isFuncMonoAxiom())
          newClauses.add(c);
        else if (env.dbg(DBG_MODIFICATION))
          out.println(" DEL: " + c);
      }
      clauses = newClauses;
    }

    if (opt.useEqConstraint()) {
      if (env.dbg(DBG_MODIFICATION))
        out.println("[Swap args in unit equalities]");

      env.initEqualityMapping(opt, clauses);

      // Swap two arguments of unit equality clauses in order to fit the reduction ordering.
      ArrayList<Clause> newClauses = new ArrayList<Clause>();
      for (Clause c : clauses) {
        if (!c.isUnit()) {
          newClauses.add(c);
          continue;
        }
        if (!c.get(0).isEqualPred()) {
          newClauses.add(c);
          continue;
        }

        Literal lit = c.get(0);
        TermWeight weight1 = env.getTermWeight1();
        TermWeight weight2 = env.getTermWeight2();
        Term arg1 = lit.getArg(0);
        Term arg2 = lit.getArg(1);
        arg1.calcTermWegiht(weight1);
        arg2.calcTermWegiht(weight2);
        if (weight2.isGreaterThan(weight1) == TermWeight.TRUE) {
          Literal eq = new Literal(env, lit.isPositive(), Term.createPredicate(env, EQUAL_PRED, arg2, arg1));
          Clause  cc = new Clause(env, c.getName(), c.getType(), eq);
          newClauses.add(cc);
          if (env.dbg(DBG_MODIFICATION))
            out.println(" SWP: " + c + " -> " + cc);
        }
        else
          newClauses.add(c);
      }
      clauses = newClauses;
    }

    unitsForMatching.clear();
    for (Clause c : clauses)
      if (c.isUnit())
        unitsForMatching.add(c);

    switch (opt.getEqType()) {
    case EQ_M:
      applyPosMonoMod(out);
      applyNegMonoMod(out);
      break;

    case EQ_SMT:
      removeNegEqXY(out);

      applyPosSymMod(out);
      applyNegSymMod(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);
      //applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_MST:
      removeNegEqXY(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);

      applyPosSymMod(out);
      applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_SNMT:
      removeNegEqXY(out);

      applyPseudoPosSymMod(out);
      applyNegSymMod(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);
      //applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_SGMT:
      removeNegEqXY(out);

      applyGeneralPseudoPosSymMod(out);
      applyNegSymMod(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);
      //applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_SNMT2:
    case EQ_SNMT2A:
      removeNegEqXY(out);

      applyPseudoPosSymMod(out);
      applyNegSymMod(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);
      //applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_MSNT:
      removeNegEqXY(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);

      applyPseudoPosSymMod(out);
      applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_MSGT:
      removeNegEqXY(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);

      applyGeneralPseudoPosSymMod(out);
      applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_MSNT2:
      removeNegEqXY(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);

      applyPseudoPosSymMod(out);
      applyNegSymMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_NSMT:
      removeNegEqXY(out);

      applyGeneralPseudoPosSymMod(out);
      applyNegSymMod(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);

      applyPosTrnMod(out);
      applyNegTrnMod(out);
      break;

    case EQ_SNMTN:
      removeNegEqXY(out);

      applyNegSymMod(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);
      //applyNegSymMod(out);

      applyNegTrnMod(out);
      break;

    case EQ_SNM:
      removeNegEqXY(out);

      applyNegSymMod(out);

      applyPosMonoMod(out);
      applyNegMonoMod(out);
      //applyNegSymMod(out);
      break;
    }

    // Computes unit clauses for unit axiom matching. Some unit clauses in the
    // original clauses may become non-unit clauses. Hence, we extract such unit
    // clauses for unit axiom matching.
    ArrayList<Clause> newUnitsForMatching = new ArrayList<Clause>();
    for (Clause c1 : unitsForMatching) {
      boolean hasSameClause = false;
      for (Clause c2 : clauses) {
        if (Clause.equals(c1, c2)) {
          hasSameClause = true;
          break;
        }
      }
      if (!hasSameClause)
        newUnitsForMatching.add(c1);
    }
    unitsForMatching = newUnitsForMatching;

//    // Checks whether a positive literal need to restart or not in RME.
//    if (opt.getCalcType() == CALC_RME) {
//      SymTable symTable = env.getSymTable();
//      List<Signature> preds = symTable.getPredicates();
//      NextPred:
//        for (Signature pred : preds) {
//          int name = pred.getID();
//          for (Clause c : clauses) {
//            for (int i=0; i < c.size(); i++) {
//              Literal lit = c.get(i);
//              if (lit.isPositive() || lit.getName() != name)
//                continue;
//              if (!lit.isMaxGeneral()) {
//                continue NextPred;
//              }
//            }
//          }
//          symTable.addTag(name, PREDICATE, NO_RESTART);
//          System.out.println(symTable.get(name, PREDICATE) + " is NO_RESTART");
//        }
//    }

  }

  /**
   * Returns true if this problem contains equality.
   * @return true if this problem contains equality.
   */
  public boolean useEquality() {
    return env.getSymTable().hasEqualPred();
  }

  /**
   * Applies the positive S-modification rule.
   * @param out the output of debug messages.
   */
  private void applyPosSymMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Pos S-modification]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      List<Clause> cs = c.applyPosSymMod(out);
      newClauses.addAll(cs);
      if (env.dbg(DBG_MODIFICATION) && cs.size() != 1) {
        out.println(" ORG: " + c);
        for (Clause cc : cs)
          out.println(" MOD: " + cc);
      }
    }

    clauses = newClauses;
  }

  /**
   * Applies the pseudo positive S-modification rule.
   * @param out the output of debug messages.
   */
  private void applyPseudoPosSymMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Pseudo Pos S-modification]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      List<Clause> cs = c.applyPseudoPosSymMod(out);
      newClauses.addAll(cs);
      if (env.dbg(DBG_MODIFICATION) && cs.size() != 1) {
        out.println(" ORG: " + c);
        for (Clause cc : cs)
          out.println(" MOD: " + cc);
      }
    }

    clauses = newClauses;
  }

  /**
   * Applies the general pseudo positive S-modification rule.
   * @param out the output of debug messages.
   */
  private void applyGeneralPseudoPosSymMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[General Pseudo Pos S-modification]");

    // Creates symmetry connector predicate.
    SymTable symTable = env.getSymTable();
    int symConName = symTable.createNewPosSrcConnector(2);

    boolean useSymCon = false;
    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      List<Clause> cs = c.applyGeneralPseudoPosSymMod(symConName, out);
      if (cs != null) {
        newClauses.addAll(cs);
        if (!useSymCon) {
          for (Clause cc : cs) {
            for (Literal lit : cc) {
              if (lit.isPositive() && lit.getTerm().getStartName() == symConName) {
                useSymCon = true;
                break;
              }
            }
          }
        }
      }
      else
        newClauses.add(c);
      if (env.dbg(DBG_MODIFICATION) && cs != null) {
        out.println(" ORG: " + c);
        for (Clause cc : cs)
          out.println(" MOD: " + cc);
      }
    }

    if (useSymCon) {
      // Generates pseudo symmetry axioms.
      Term var1 = Term.createVar(env, 0);
      Term var2 = Term.createVar(env, 1);
      Term symCon1 = Term.createPredicate(env, symConName, var1, var2);
      Term symCon2 = Term.createPredicate(env, symConName, var1, var2);
      Literal negSymCon1 = new Literal(env, false, symCon1);
      Literal negSymCon2 = new Literal(env, false, symCon2);

      Term eq1 = Term.createPredicate(env, EQUAL_PRED, var1, var2);
      Term eq2 = Term.createPredicate(env, EQUAL_PRED, var2, var1);
      Literal posEq1 = new Literal(env, true, eq1); // +equal(X,Y)
      Literal posEq2 = new Literal(env, true, eq2); // +equal(Y,X)

      Clause c1 = new Clause(env, "positive symmetry", AXIOM, negSymCon1, posEq1);
      Clause c2 = new Clause(env, "positive symmetry", AXIOM, negSymCon2, posEq2);

      newClauses.add(c1);
      newClauses.add(c2);
    }

    clauses = newClauses;
  }

  /**
   * Applies the negative S-modification rule.
   * @param out the output of debug messages.
   */
  private void applyNegSymMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Neg S-modification]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      Clause cc = c.applyNegSymMod(out);
      newClauses.add(cc);
      if (env.dbg(DBG_MODIFICATION) && c != cc) {
        out.println(" ORG: " + c);
        out.println(" MOD: " + cc);
      }
    }

    clauses = newClauses;
  }

  /**
   * Removes the negative equal(X,Y) literals from the axiom set.
   * @param out the output of debug messages.
   */
  private void removeNegEqXY(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Remove -equal(X,Y)]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {

      Clause cc = c.removeNegEqXY(out);
      if (c != cc && cc.isUnit() && cc.get(0).isPosEqualPred()) {
        Literal eq = cc.get(0);
        Term arg1 = eq.getArg(0);
        Term arg2 = eq.getArg(1);
        if (Term.equals(arg1, arg2))
          cc = null;    // because cc is subsumed by +eq(X,X) (reflexivity)
      }

      if (cc != null)
        newClauses.add(cc);

      if (env.dbg(DBG_MODIFICATION) && c != cc) {
        if (cc != null) {
          out.println(" ORG: " + c);
          out.println(" MOD: " + cc);
        }
        else
          out.println(" DEL: " + c);
      }
    }

    clauses = newClauses;
  }

  /**
   * Applies the positive M-modification rule.
   * @param out the output of debug messages.
   */
  private void applyPosMonoMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Pos M-modification]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      Clause cc = c.applyPosMonoMod(out, opt);
      newClauses.add(cc);

      if (env.dbg(DBG_MODIFICATION) && c != cc) {
        out.println(" ORG: " + c);
        out.println(" MOD: " + cc);
      }
    }

    clauses = newClauses;
  }

  /**
   * Applies the negative M-modification rule.
   * @param out the output of debug messages.
   */
  private void applyNegMonoMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Neg M-modification]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      Clause cc = c.applyNegMonoMod(out, opt);
      newClauses.add(cc);

      if (env.dbg(DBG_MODIFICATION) && c != cc) {
        out.println(" ORG: " + c);
        out.println(" MOD: " + cc);
      }
    }

    clauses = newClauses;
  }

  /**
   * Applies the positive T-modification rule.
   * @param out the output of debug messages.
   */
  private void applyPosTrnMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Pos T-modification]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      Clause cc = c.applyPosTrnMod(out);
      newClauses.add(cc);
      if (env.dbg(DBG_MODIFICATION) && c != cc) {
        out.println(" ORG: " + c);
        out.println(" MOD: " + cc);
      }
    }

    clauses = newClauses;
  }

  /**
   * Applies the negative T-modification rule.
   * @param out the output of debug messages.
   */
  private void applyNegTrnMod(PrintWriter out) {

    if (env.dbg(DBG_MODIFICATION))
      out.println("[Neg T-modification]");

    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c : clauses) {
      Clause cc = c.applyNegTrnMod(out);
      newClauses.add(cc);
      if (env.dbg(DBG_MODIFICATION) && c != cc) {
        out.println(" ORG: " + c);
        out.println(" MOD: " + cc);
      }
    }

    clauses = newClauses;
  }

  /**
   * Applies the pure literal elimination to this problem.
   * @param out the output of debug messages.
   */
  public void applyPureLitElimination(PrintWriter out) {

    SymTable symTable = env.getSymTable();

    ArrayList<Clause> removed = new ArrayList<Clause>();
    boolean modified = true;
    while (modified) {
      modified = false;

      boolean pocc[] = new boolean[symTable.getNumSyms(PREDICATE)];
      boolean nocc[] = new boolean[symTable.getNumSyms(PREDICATE)];

      // Equality predicates can not be removed.
      int eqName = symTable.getEqualPredName();
      if (eqName >= 0) pocc[eqName] = nocc[eqName] = true;

      // Literals belonging a production field can not be removed.
      for (PLiteral pl : pfield.getPLiterals()) {
        if (pl.isSpecial()) {
          for (int name=0; name < symTable.getNumSyms(PREDICATE); name++)
            pocc[name] = nocc[name] = true;
        }
        else {
          int name = pl.getTerm().getName();
          pocc[name] = nocc[name] = true;
        }
      }

      for (Clause c : clauses) {
        if (c.isPredMonoAxiom()) continue;    // monotonicity axioms for predicates can be ignored.
        for (Literal l : c) {
          int name = l.getName();
          if (l.isPositive())
            pocc[name] = true;
          else
            nocc[name] = true;
        }
      }

      IntHashSet pures = new IntHashSet();
      for (int i=0; i < pocc.length; i++)
        if (pocc[i] != nocc[i])
          pures.add(i);

      ArrayList<Clause> newClauses = new ArrayList<Clause>();
      for (Clause c : clauses) {
        boolean redundant = false;
        for (Literal l : c) {
          if (pures.contains(l.getName())) {
            redundant = true;
            break;
          }
        }
        if (redundant) {
          removed.add(c);
          modified =true;
        }
        else
          newClauses.add(c);
      }
      clauses = newClauses;
    }

    if (env.dbg(DBG_PROBLEM) && !removed.isEmpty()) {
      out.println("[Pure literal elimination]");
      for (Clause c : removed)
        out.println(" DEL: " + c);
    }

  }

  /**
   * Applies the frequent common literals extraction to this problem.
   * @param out the output of debug messages.
   */
  public void applyFreqCommonLitsExtraction(PrintWriter out) {

    int numTrials = 0;

    while (true) {

      boolean first = true;

      // Translates each term into an integer.
      TermIntMap map = new TermIntMap();
      DB db = new DB();
      for (Clause c : clauses) {
        ItemSet items = new ItemSet();
        for (Literal l : c) {
          int id = map.put(l.getTerm());
          if (l.isNegative())
            id = -id;
          items.add(id);
        }
        items.sort();
        db.add(items);
      }

      // Finds closed frequent patterns.
      Miner miner = new PrefixSpan();
      System.out.println("mining start");
      ArrayList<FreqItemSet> freqItemSets = db.getClosedFreqItemSets(miner, 2, 2);
      System.out.println("mining end");

      // Sorts the patterns according to the number of literals to be replaced.
      Collections.sort(freqItemSets, new Comparator<FreqItemSet>() {
        public int compare(FreqItemSet o1, FreqItemSet o2) {
          return (o2.size() * o2.getFreq()) - (o1.size() * o1.getFreq());
        }
      });

      // Extracts frequent common literals from the clauses.
      ArrayList<Clause> newClauses = new ArrayList<Clause>();
      IntHashSet modified = new IntHashSet();
      for (FreqItemSet freqItemSet : freqItemSets) {
        IntArraySet occs = new IntArraySet(freqItemSet.getOccurrences());
        if (!modified.isDisjoint(occs))
          continue;

        // Prepares the frequent literals.
        ArrayList<Literal> freqLits = new ArrayList<Literal>();
        boolean posDstClause = true;
        boolean negDstClause = true;
        IntIterator i = freqItemSet.iterator();
        while (i.hasNext()) {
          int id = i.next();
          if (id > 0) {
            freqLits.add(new Literal(env, true, new Term(map.get(+id))));
            negDstClause = false;
          }
          else {
            freqLits.add(new Literal(env, false, new Term(map.get(-id))));
            posDstClause = false;
          }
        }

        if (env.dbg(DBG_PROBLEM)) {
          if (first) {
            out.println("[Frequent Common Literal Extraction #" + (++numTrials) + "]");
            first = false;
          }
          out.println("FREQ: " + freqLits + " * " + freqItemSet.getFreq());
        }

        // Extracts frequent literals from clauses.
        ArrayList<ArrayList<Literal>> srcClauseCands = new ArrayList<ArrayList<Literal>>();
        IntHashSet srcVars = new IntHashSet();
				@SuppressWarnings("unused")
				int numPosSrcClauses = 0;
        int numNegSrcClauses = 0;
        i = occs.iterator();
        while (i.hasNext()) {
          Clause corg = clauses.get(i.next());
          ArrayList<Literal> newLits = new ArrayList<Literal>();
          boolean posSrcClause = true;
          boolean negSrcClause = true;
          NEXT_LIT:
          for (Literal lorg : corg) {
            for (Literal lit : freqLits) {
              if (Literal.equals(lorg, lit))
                continue NEXT_LIT;
            }
            newLits.add(new Literal(lorg));
            if (lorg.isPositive())
              negSrcClause = false;
            else
              posSrcClause = false;
            lorg.countVars(srcVars);    // Collects variables in source literals.
          }

          srcClauseCands.add(newLits);
          if (posSrcClause) numPosSrcClauses++;
          if (negSrcClause) numNegSrcClauses++;
        }

        // Collects variables in destination literals.
        IntSet dstVars = new IntHashSet();
        i = freqItemSet.iterator();
        while (i.hasNext())
          map.get(Math.abs(i.next())).countVars(dstVars);

        // Computes the intersection of variable sets.
        IntArraySet commons = new IntArraySet(srcVars.intersect(dstVars));
        commons.sort();

        // Determines the sign of a connector predicate in source side.
//        out.println("posDstClause     = " + posDstClause);
//        out.println("negDstClause     = " + negDstClause);
//        out.println("numPosSrcClauses = " + numPosSrcClauses);
//        out.println("numNegSrcClauses = " + numNegSrcClauses);
        boolean isPosSrcConnector = false;
        if (posDstClause)
          isPosSrcConnector = true;
        else if (negDstClause)
          isPosSrcConnector = false;
        else if (numNegSrcClauses > 0)
          isPosSrcConnector = true;

        // Creates a connector predicate.
        ArrayList<Term> vars = new ArrayList<Term>();
        for (int j=0; j < commons.size(); j++)
          vars.add(Term.createVar(env, commons.getAt(j)));
        int connName = 0;
        if (isPosSrcConnector)
          connName = env.getSymTable().createNewPosSrcConnector(vars.size());
        else
          connName = env.getSymTable().createNewNegSrcConnector(vars.size());
        Term conn = Term.createPredicate(env, connName, vars);
        Literal srcConn = new Literal(env,  isPosSrcConnector, new Term(conn));
        Literal dstConn = new Literal(env, !isPosSrcConnector, new Term(conn));

        // Creates a destination clause.
        ArrayList<Literal> dstLits = new ArrayList<Literal>();
        dstLits.add(dstConn);
        dstLits.addAll(freqLits);
        Clause dstClause = new Clause(env, "connector", AXIOM, dstLits);
        newClauses.add(dstClause);
        if (env.dbg(DBG_PROBLEM)) {
          out.println(" ADD: " + dstClause);
        }

        // Creates source clauses.
        for (int j=0; j < srcClauseCands.size(); j++) {
          Clause corg = clauses.get(occs.getAt(j));
          ArrayList<Literal> srcClauseCand = srcClauseCands.get(j);
          srcClauseCand.add(new Literal(srcConn));
          Clause cnew = new Clause(env, corg.getName(), corg.getType(), srcClauseCand);
          newClauses.add(cnew);

          if (env.dbg(DBG_PROBLEM)) {
            out.println(" ORG: " + corg);
            out.println(" CHG: " + cnew);
          }
        }

        modified.addAll(occs);
      }

      if (modified.isEmpty())
        break;

      // Rename variables in new clauses.
      for (Clause c : newClauses)
        c.rename();

      // Creates the new set of clauses.
      for (int i=0; i < clauses.size(); i++)
        if (!modified.contains(i))
          newClauses.add(clauses.get(i));
      clauses = newClauses;
    }
  }

  /**
   * Converts the input clauses to the subsumption minimal ones.
   * @param out the output of debug messages.
   */
  public void convertToSubsumpMinimal(PrintWriter out) {

    // Subsumption check: prevents the following generation for example.
    // C1: [+&(_0,_1,_2), -equal(f(_0,_1),_3), -equal(f(_0,_2),_3)]
    // C2: [-&(_0,_1,_2), +equal(g(_0,_2),_3), -equal(g(_0,_1),_3)]
    // C3: [-&(_0,_1,_2), +equal(g(_0,_1),_3), -equal(g(_0,_2),_3)]
    // C1 + C2 subsumes C1 + C3.
    if (opt.getEqType() != CFP.EQ_AXIOMS_REQUIRED && useEquality()) {

      boolean first = true;
      List<Signature> preds = env.getSymTable().getPredicates();
      for (Signature pred : preds) {

        if (!pred.hasTag(TermTypes.POS_SRC_CONN | TermTypes.NEG_SRC_CONN))
          continue;

        // Find out
        // (1) the clause that has a positive symmetry connector and
        // (2) the clauses that have a negative symmetry connector.
        Clause posSymClause  = null;
        Clause negSymClause1 = null;
        Clause negSymClause2 = null;
        ArrayList<Clause> newClauses = new ArrayList<Clause>();
        for (Clause clause : clauses) {
          if (clause.find(pred.getID(), true) != null) {
            assert(posSymClause == null);
            posSymClause = clause;
          }
          else if (clause.find(pred.getID(), false) != null) {
            if (negSymClause1 == null)
              negSymClause1 = clause;
            else if (negSymClause2 == null)
              negSymClause2 = clause;
            else
              assert(false);
          }
          else
            newClauses.add(clause);
        }
        assert(posSymClause  != null);
        assert(negSymClause1 != null);
        assert(negSymClause2 != null);

        // If the length of posSymClause + negSymClause{1,2} is more than 20,
        // then cancel the subsumption checking since the checking consumes the large amount of time.
        if (posSymClause.size() + negSymClause1.size() > 20)
          continue;

        // Unify the symmetry connectors
        int posSymClauseVars  = posSymClause.getNumVars();
        int negSymClause1Vars = negSymClause1.getNumVars();
        int negSymClause2Vars = negSymClause2.getNumVars();
        Clause newPosSymClause1 = Clause.newOffset(posSymClause, 0);
        Clause newPosSymClause2 = Clause.newOffset(posSymClause, posSymClauseVars);
        Clause newNegSymClause1 = Clause.newOffset(negSymClause1, posSymClauseVars * 2);
        Clause newNegSymClause2 = Clause.newOffset(negSymClause2, posSymClauseVars * 2 + negSymClause1Vars);

        Literal posSymLit1 = newPosSymClause1.find(pred.getID(), true);
        Literal posSymLit2 = newPosSymClause2.find(pred.getID(), true);
        Literal negSymLit1 = newNegSymClause1.find(pred.getID(), false);
        Literal negSymLit2 = newNegSymClause2.find(pred.getID(), false);
        assert(posSymLit1 != null && posSymLit2 != null && negSymLit1 != null && negSymLit2 != null);

        VarTable varTable = env.getVarTable();
        int totalNumVars = posSymClauseVars * 2 + negSymClause1Vars + negSymClause2Vars;
        varTable.addVars(totalNumVars);

        Subst g1 = posSymLit1.compUnify(negSymLit1);
        Subst g2 = posSymLit2.compUnify(negSymLit2);
        assert(g1 != null && g2 != null);

        ArrayList<Literal> lits1 = new ArrayList<Literal>();
        ArrayList<Literal> lits2 = new ArrayList<Literal>();
        for (Literal lit : newPosSymClause1)
          if (!(lit.isPositive() && lit.getName() == pred.getID()))
            lits1.add(new Literal(lit));
        for (Literal lit : newPosSymClause2)
          if (!(lit.isPositive() && lit.getName() == pred.getID()))

           lits2.add(new Literal(lit));
        for (Literal lit : newNegSymClause1)
          if (!(lit.isNegative() && lit.getName() == pred.getID()))
            lits1.add(new Literal(lit));
        for (Literal lit : newNegSymClause2)
          if (!(lit.isNegative() && lit.getName() == pred.getID()))
            lits2.add(new Literal(lit));

        Clause c1 = new Clause(env, posSymClause.getName(), posSymClause.getType(), lits1, posSymClause.getOrigin(true));
        Clause c2 = new Clause(env, posSymClause.getName(), posSymClause.getType(), lits2, posSymClause.getOrigin(true));

        if (c1.size() < 15 && c1.subsumes(c2)) {
          c1 = c1.instantiate();
          c1.rename();
          newClauses.add(c1);
          varTable.backtrackTo(0);
          varTable.removeVars(totalNumVars);
          if (env.dbg(DBG_MODIFICATION)) {
            if (first) {
              first = false;
              out.println("[Subsumed clause checking for symmetory connectors]");
            }
            out.println(" DEL: " + posSymClause + " + " + negSymClause2 + " by " + posSymClause + " + " + negSymClause1);
            out.println(" CHG: " + posSymClause + " + " + negSymClause1 + " -> " + c1);
          }
        }
        else {
          varTable.backtrackTo(0);
          varTable.removeVars(totalNumVars);
          newClauses.add(posSymClause);
          newClauses.add(negSymClause1);
          newClauses.add(negSymClause2);
        }

        clauses = newClauses;
      }
    }

    // Makes a feature vector trie.
    FVecTrie fvecTrie = new FVecTrie(env, true);

    // Adds input clauses to the trie.
    List<Pair<Clause,Clause>> subsumed = new LinkedList<Pair<Clause,Clause>>();
    for (Clause c : clauses) {

      env.getVarTable().addVars(c.getNumVars());

      if (c.size() <= 15) {
        // Forward subsumption checking.
        Clause subsuming = fvecTrie.findSubsuming(c.getFVec(false), c);
        if (subsuming != null) {
          subsumed.add(new Pair<Clause,Clause>(c, subsuming));
          env.getVarTable().removeVars(c.getNumVars());
          continue;
        }
        // Backward subsumption checking.
        List<Clause> cs = fvecTrie.findSubsumed(c.getFVec(false), c);
        fvecTrie.remove(cs);
        for (Clause cc : cs)
          subsumed.add(new Pair<Clause,Clause>(cc,c));
      }
      // Adds the clause to the database.
      fvecTrie.add(c.getFVec(false), c);
      env.getVarTable().removeVars(c.getNumVars());
    }

    env.getStats().setSuccs(Stats.CLA_SUBSUMP_MIN, fvecTrie.getNumSubsumChecks());
    env.getStats().setTests(Stats.CLA_SUBSUMP_MIN, fvecTrie.getNumSubsumChecksWithoutFiltering());

    if (env.dbg(DBG_PROBLEM) && !subsumed.isEmpty()) {
      out.println("[Subsumed clause checking]");
      for (Pair<Clause,Clause> pair : subsumed)
        out.println(" DEL: " + pair.get1st() + " by " + pair.get2nd());
    }

    // Gets the subsumption minimal clauses.
    for (Pair<Clause,Clause> pair : subsumed)
      clauses.remove(pair.get1st());

    // Find out all top clauses
    topClauses.clear();
    for (Clause c : clauses)
      if (c.getType() == TOP_CLAUSE)
        topClauses.add(c);
  }

  /**
   * Initializes top clauses of this problem.
   */
  public void initTopClauses() {

    // If a production field is empty, then try to find a refutation.
    if (pfield.isEmpty())
      problemType = REFUTATION;

    // Find out all top clauses
    topClauses.clear();
    for (Clause c : clauses)
      if (c.getType() == TOP_CLAUSE)
        topClauses.add(c);

    // If there exists at least one top clause, then choose them.
    if (topClauses.size() >= 1)
      return;

    // If there is no top clause,
    // (a) when theorem proving, select all positive or negative clauses as top clauses.
    // (b) when consequence finding, handle all clauses as top clauses.

    // (a) when theorem proving, generate a new top clause from positive or negative clauses.
    if (pfield.isEmpty()) {

      // Collect all negative and positive clauses.
      ArrayList<Clause> negClauses = new ArrayList<Clause>();
      ArrayList<Clause> posClauses = new ArrayList<Clause>();
      for (Clause c : clauses)
        if (c.isPositive())
          posClauses.add(c);
        else if (c.isNegative())
          negClauses.add(c);

      // If there is no positive or negative clauses, then the problem is trivially satisfiable.
      if (posClauses.isEmpty() || negClauses.isEmpty()) {
        status = TRIVIALLY_SATISFIABLE;
        return;
      }

      // (1) Remove the reflective equality axiom {+eq(X,X)} from the
      // positive clauses (that can not be extendable since the ordering
      // constraint of strong connection rule always fails)
      // (2) Remove clauses which have the literals consists of only the minimal terms.
      if (opt.getEqType() != CFP.EQ_AXIOMS_REQUIRED && useEquality()) {
        ArrayList<Clause> newPosClauses = new ArrayList<Clause>();
        NEXT_CLAUSE:
        for (Clause clause : posClauses) {
          //if (clause.isEqReflextAxiom())
          //  continue;
          for (int i=0; i < clause.size(); i++) {
            Term term = clause.get(i).getTerm();
            if (term.isEqualPred()) {
              Term arg1 = term.getArg(0);
              Term arg2 = term.getArg(1);
              if (arg1.getStartType() == FUNCTION && !arg1.isMaxGeneral())
                continue NEXT_CLAUSE;
              if (arg2.getStartType() == FUNCTION && !arg2.isMaxGeneral())
                continue NEXT_CLAUSE;
            }
            // non equality predicate
            else if (!term.isMaxGeneral())
              continue NEXT_CLAUSE;
          }
          newPosClauses.add(clause);
        }
        posClauses = newPosClauses;
      }

      if (opt.useNegTopClauses() || posClauses.isEmpty()) {
        for (Clause negClause : negClauses) {
          negClause.setType(TOP_CLAUSE);
          topClauses.add(negClause);
        }
      }
      else {
        for (Clause posClause : posClauses) {
          posClause.setType(TOP_CLAUSE);
          topClauses.add(posClause);
        }
      }
    }
    // (b) when consequence finding, handle all clauses as top clauses.
    else {

      problemType = CHARACTERISTIC;

      for (Clause clause : clauses) {
        clause.setType(TOP_CLAUSE);
        topClauses.add(clause);
      }
    }
  }

  /**
   * Converts this problem into the single top clause format.
   * @deprecated
   */
  public void convertToSingleTopClauseFormat() {

    // Find out all top clauses
    topClauses.clear();
    for (Clause c : clauses)
      if (c.getType() == TOP_CLAUSE)
        topClauses.add(c);

    // If a production field is empty, then try to find a refutation.
    if (pfield.isEmpty())
      problemType = REFUTATION;

    // If there is a single top clause, then choose it.
    if (topClauses.size() == 1) {
      return;
    }
    // If there are multiple top clauses, then convert these into a new single top clause.
    else if (topClauses.size() > 1) {
      // (1) Makes the special literal $.
      Term    special    = Term.createPredicate(env, TOP_PRED);
      Literal posSpecial = new Literal(env, true,  special);
      Literal negSpecial = new Literal(env, false, special);

      // (2) Adds $ into each top clause, and converts these to axioms.
      clauses.removeAll(topClauses);
      for (Clause c : topClauses) {
        ArrayList<Literal> lits = new ArrayList<Literal>();
        lits.add(posSpecial);
        lits.addAll(c.getLiterals());
        clauses.add(new Clause(env, c.getName(), AXIOM, lits));
      }
      topClauses.clear();

      // (3) Add the new top clause { -$ }.
      ArrayList<Literal> lits = new ArrayList<Literal>();
      lits.add(negSpecial);
      Clause newtop = new Clause(env, "Generated", TOP_CLAUSE, lits);
      clauses.add(newtop);
      topClauses.add(newtop);
    }
    // If there is no top clause,
    // (a) when theorem proving, generate a new top clause from positive or negative clauses.
    // (b) when consequence finding, handle all clauses as top clauses.
    else {

      // (a) when theorem proving, generate a new top clause from positive or negative clauses.
      if (pfield.isEmpty()) {

        // Collect all negative and positive clauses.
        ArrayList<Clause> negClauses = new ArrayList<Clause>();
        ArrayList<Clause> posClauses = new ArrayList<Clause>();
        for (Clause c : clauses)
          if (c.isPositive())
            posClauses.add(c);
          else if (c.isNegative())
            negClauses.add(c);

        // If there is no positive or negative clauses, then the problem is trivially satisfiable.
        if (posClauses.isEmpty() || negClauses.isEmpty()) {
          status = TRIVIALLY_SATISFIABLE;
          return;
        }

        // If there is a single negative clause, then choose it as a top clause.
        if (negClauses.size() == 1) {
          Clause clause = negClauses.get(0);
          Clause newtop = new Clause(env, clause.getName(), TOP_CLAUSE, clause.getLiterals());
          clauses.remove(clause);
          clauses.add(newtop);
          topClauses.add(newtop);
        }
        // If there are multiple negative clauses, then convert these into a new single top clause.
        else {

          // (1) Make the special literal $.
          Term    special    = Term.createPredicate(env, TOP_PRED);
          Literal posSpecial = new Literal(env, true,  special);
          Literal negSpecial = new Literal(env, false, special);

          // (2) Add $ into each negative clause.
          clauses.removeAll(negClauses);
          for (Clause c : negClauses) {
            if (c.isNecessary()) {
              ArrayList<Literal> lits = new ArrayList<Literal>();
              lits.add(posSpecial);
              lits.addAll(c.getLiterals());
              Clause clause = new Clause(env, c.getName(), c.getType(), lits);
              clauses.add(clause);
            }
            else {
              clauses.add(c);
            }
          }

          // (3) Add the new top clause { -$ }.
          ArrayList<Literal> lits = new ArrayList<Literal>();
          lits.add(negSpecial);
          Clause newtop = new Clause(env, "Generated", TOP_CLAUSE, lits);
          topClauses.add(newtop);
          clauses.add(newtop);
        }
      }
      // (b) when consequence finding, handle all clauses as top clauses.
      else {

        problemType = CHARACTERISTIC;

        // (1) Make the special literal $.
        Term    special    = Term.createPredicate(env, TOP_PRED);
        Literal posSpecial = new Literal(env, true,  special);
        Literal negSpecial = new Literal(env, false, special);

        // (2) Add $ into each clause.
        LinkedList<Clause> cs = new LinkedList<Clause>();
        for (Clause c : clauses) {
          if (c.isNecessary()) {
            ArrayList<Literal> lits = new ArrayList<Literal>();
            lits.add(posSpecial);
            lits.addAll(c.getLiterals());
            cs.add(new Clause(env, c.getName(), c.getType(), lits));
          }
          else {
            cs.add(c);
          }
        }
        clauses.clear();
        clauses.addAll(cs);

        // (3) Add the new top clause { -$ }.
        ArrayList<Literal> lits = new ArrayList<Literal>();
        lits.add(negSpecial);
        Clause newtop = new Clause(env, "Generated", TOP_CLAUSE, lits);
        topClauses.add(newtop);
        clauses.add(newtop);
      }
    }
  }

  /**
   * Initializes the clause properties.
   * @param out the output of debug messages.
   */
  public void initClauseProperties(PrintWriter out) {
    // Updates the number of variables and symbols in the clause.
    for (Clause c : clauses) {
      c.getNumVars();
      c.getNumSyms(false);
    }

    // Updates the number of extendable clauses for each clause.
    ClauseDB clauseDB = new ClauseDB(env, opt, clauses);
    for (Clause c : clauses)
      c.getNumExts(true, clauseDB);

    // Sets the unique identifier to each clause.
    if (opt.use(USE_INC_CARC_COMP) && problemType == CHARACTERISTIC) {
      List<Clause> cc = new ArrayList<Clause>(clauses);
      // Prefers clauses that have smaller number of extensions.
      Collections.sort(cc, new Comparator<Clause>() {
        public int compare(Clause c1, Clause c2) { return c1.getNumExts(false, null) - c2.getNumExts(false, null); }});
      int id = 1;
      for (Clause c : cc) {
        //System.out.println("ID " + id + " : " + c);
        c.setID(id++);
      }
    }
  }

  /**
   * Initializes the incremental characteristic computation.
   * @param out the output of debug messages.
   */
  public void initIncCarcComputation(PrintWriter out) {
    // Sets the unique identifier to each clause.
    if (opt.use(USE_INC_CARC_COMP) && problemType == CHARACTERISTIC) {
      List<Clause> cc = new ArrayList<Clause>(clauses);
      // Prefers clauses that have smaller number of extensions.
      Collections.sort(cc, new Comparator<Clause>() {
        public int compare(Clause c1, Clause c2) { return c1.getNumExts(false, null) - c2.getNumExts(false, null); }});
      int id = 1;
      for (Clause c : cc) {
        //System.out.println("ID " + id + " : " + c);
        c.setID(id++);
      }
    }
  }

  /**
   * Adds the reflexivity axiom to the problem if not exist.
   * @return true if the reflexivity axiom is added.
   */
  public boolean addEqReflexivity() {
    // If this problem contains the reflexivity axiom, then do nothing.
    for (Clause clause : clauses)
      if (clause.isEqReflectAxiom())
        return false;

    // Creates and adds the reflexivity axiom.
    Term var = Term.createVar(env, 0);
    Term eq  = Term.createPredicate(env, EQUAL_PRED, var, var);
    Literal lit = new Literal(env, true, eq);
    Clause axiom = new Clause(env, "reflexivity", AXIOM, lit);
    clauses.add(axiom);
    return true;
  }

  /**
   * Removes the reflexivity axiom from the problem.
   */
  public void removeEqReflexivity() {
    // If this problem contains the reflexivity axiom, then removes it.
    ArrayList<Clause> newClauses = new ArrayList<Clause>();
    for (Clause clause : clauses) {
      if (clause.isEqReflectAxiom())
        continue;
      newClauses.add(clause);
    }
    clauses = newClauses;
  }

  /**
   * Sorts the literals in each clause.
   * @param out the output of debug messages.
   */
  public void convertToSortedClauses(PrintWriter out) {
    // Sorts the literal ordering.
    if (!litOrder.isOrg())
      for (Clause c : clauses)
        c.sort(litOrder);
  }

  /**
   * Calculates literal ordering for order preserving reduction.
   */
  public void calcReductionOrder() {
    // Simple version of the reduction order between literals.
    int order = 0;
    for (Clause c : clauses)
      for (Literal lit : c)
        lit.setReductionOrder(order++);
  }

  /**
   * Initializes the feature vector mapping.
   */
  public void initFVecMap() {
    env.initFVecMap(clauses, pfield);
  }

  /**
   * Calculates the conditions for satisfying the tautology freeness.
   * @param out the output of debug messages.
   */
  public void initTautologyFreeness(PrintWriter out) {
    ArrayList<Clause> tautologies = new ArrayList<Clause>();
    for (Clause c : clauses)
      if (!c.initCompUnifiableLiterals())
        tautologies.add(c);

    if (env.dbg(DBG_TAUTOLOGY_FREE)) {
      out.println("[Tautology free checking]");
      for (Clause c : clauses) {
        List<Pair<Literal,Literal>> pairs = c.getCompUnifiableLiterals();
        if (pairs == null || pairs.isEmpty())
          continue;
        out.println(c);
        for (Pair<Literal,Literal> pair : pairs)
          out.println(" " + pair.get1st() + " <-> " + pair.get2nd());
      }
    }

    if (env.dbg(DBG_PROBLEM) && !tautologies.isEmpty()) {
      out.println("[Tautology checking]");
      for (Clause c : tautologies)
        out.println(" DEL: " + c);
    }

    // Removes tautology clauses.
    clauses.removeAll(tautologies);
    topClauses.removeAll(tautologies);
  }

  /**
   * Calculates the unit subsumption candidates.
   */
  public void initUnitSubsumptionChecking() {
    // Finds out all the unit clauses.
    ArrayList<Clause> units = new ArrayList<Clause>();
    for (Clause c : clauses)
      if (c.isUnit())
        units.add(c);

    for (Clause unit : units) {
      Clause renamed = unit.instantiate();
      renamed.rename(env.getNegVarRenameMap());
      for (Clause c : clauses) {
        if (c == unit) continue;
        c.initUnitSubsumptionCechking(renamed);
      }
    }

    // TEST
    if (opt.use(USE_TEST4)) {
      for (Clause unit : unitsForMatching) {
        Clause renamed = unit.instantiate();
        renamed.rename(env.getNegVarRenameMap());
        for (Clause c : clauses) {
          if (c.getOrigin(true) == unit)
            continue;
          c.initUnitSubsumptionCechking(renamed);
        }
      }
    }
  }

  /**
   * Initialize the skip-minimality checking.
   * @param out the output of debug messages.
 * @throws Exception 
   */
  public void initSkipMinimality(PrintWriter out) throws Exception {
    if (!opt.use(USE_SKIP_MINIMALITY) || problemType == REFUTATION)
      return;

    ConseqSet conqs = getConseqSet();

    if (opt.getCarcFile() != null) {
      List<Conseq> carcs = parser.conseqs(new BufferedReader(new FileReader(opt.getCarcFile())));
      for (Conseq carc : carcs) {
        Clause c = carc.instantiate();         // make a copy.
        c.setType(AXIOM);                      // change the type from CONSEQ to AXIOM.
        conqs.add(c);
      }
    }
    if (carcSet != null) {
      for (Conseq carc : carcSet) {
        Clause c = carc.instantiate();         // make a copy.
        c.setType(AXIOM);                      // change the type from CONSEQ to AXIOM.
        conqs.add(c);
      }
    }

    if (problemType == CHARACTERISTIC)
      return;

    PFieldCheckerCardConstraints checker = PFieldCheckerCardConstraints.createChecker(env, pfield);
    for (Clause c : clauses) {
      if (c.getType() == ClauseTypes.AXIOM)
        if (checker.belongs(c))
          conqs.add(c.instantiate());    // make a copy.
    }
  }

  /**
   * Adds a characteristic clause to this CFP.
   * @param carc  a characteristic clause.
   */
  public void addCarc(Clause carc) {
    Clause c = carc.instantiate();         // make a copy.
    c.setType(AXIOM);                      // change the type from CONSEQ to AXIOM.
    getConseqSet().add(c);
  }

  /**
   * Removes all top clauses.
   */
  public void removeTopClauses() {
    clauses.removeAll(topClauses);
    topClauses = new ArrayList<Clause>();
  }

  /**
   * Returns an iterator over the clauses in this problem.
   * @return an iterator over the clauses in this problem.
   */
  public Iterator<Clause> iterator() {
    return clauses.iterator();
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append("----------------------------------------------------------------------\n");
    if (problemName != null) {
      str.append("Problem:\n");
      str.append(" " + problemName + "\n");
    }

    str.append("Clauses:\n");
    for (Clause clause : clauses)
      str.append(" " + clause + "\n");

    if (!topClauses.isEmpty()) {
      str.append("Top clauses:\n");
      for (Clause clause : topClauses)
        str.append(" " + clause + "\n");
    }

    str.append("Production field:\n");
    str.append(" " + pfield + "\n");

    return str.toString();
  }

  /** Calculates productions. */
  public final static int PRODUCTION = 0;
  /** Calculates characteristic clauses. */
  public final static int CHARACTERISTIC = 1;
  /** Refutation finding. */
  public final static int REFUTATION = 2;

  /** No equality handling. */
  public final static int EQ_AXIOMS_REQUIRED = 0;
  /** Monotonicity modification. */
  public final static int EQ_M = 1;
  /** Paskevich's modification method (applying rules by the following order: S->M->T). */
  public final static int EQ_SMT = 2;
  /** Paskevich's modification method (applying rules by the following order: M->S->T). */
  public final static int EQ_MST = 3;
  /** Simulates positive symmetry using pseudo predicates. */
  public final static int EQ_SNMT = 4;
  /** Simulates positive symmetry using pseudo predicates. */
  public final static int EQ_SGMT = 5;
  /** Simulates positive symmetry using pseudo predicates(applying rules by the following ordering: M->Sn->T ). */
  public final static int EQ_MSNT = 6;
  /** Simulates positive symmetry using general pseudo predicates (applying rules by the following ordering: M->Sg->T ). */
  public final static int EQ_MSGT = 7;
  /** Simulates positive symmetry using pseudo predicates with NEF. */
  public final static int EQ_SNMT2 = 8;
  /** Simulates positive symmetry using pseudo predicates with NEF for all args. */
  public final static int EQ_SNMT2A = 9;
  /** Simulates positive symmetry using pseudo predicates (applying rules by the following ordering: M->Sn->T ). */
  public final static int EQ_MSNT2 = 10;
  /** Uses general S-modification, full M-modification and T-modification. */
  public final static int EQ_NSMT = 11;
  /** Simulates the negative symmetry and negative transitivity. */
  public final static int EQ_SNMTN = 12;
  /** Simulates the negative symmetry and transitivity. */
  public final static int EQ_SNM = 13;

  /** Do not use equality constraints. */
  public final static int EQ_CONSTRAINTS_NONE = 0;
  /** Partial evaluation of equality constraints. */
  public final static int EQ_CONSTRAINTS_PART = 1;
  /** Full evaluation of equality constraints. */
  public final static int EQ_CONSTRAINTS_FULL = 2;
  /** Equality constraints are checked fully and in advance. */
  public final static int EQ_CONSTRAINTS_ADVANCE = 3;

  /** The environment. */
  private Env env= null;
  /** The options. */
  private Options opt = null;
  /** The name of this problem. */
  private String problemName = null;
  /** The type of this problem. */
  private int problemType = PRODUCTION;
  /** The set of clauses in this problem. */
  private ArrayList<Clause> clauses = new ArrayList<Clause>();
  /** The top clause of this problem. */
  private ArrayList<Clause> topClauses = new ArrayList<Clause>();
  /** The set of unit clauses for unit axiom matching. */
  private ArrayList<Clause> unitsForMatching = new ArrayList<Clause>();
  /** The production field in this problem. */
  private PField pfield = null;
  /** The search strategy. */
  private Strategy strategy = null;
  /** The literal ordering in tableau clauses. */
  private LitOrder litOrder = null;
  /** The operator ordering in tableau clauses. */
  private OpOrder opOrder = null;
  /** A set of found consequences. */
  private ConseqSet conseqSet = null;
  /** A set of characteristic clauses. */
  private ConseqSet carcSet = null;
  /** The status of this problem. */
  private int status = UNKNOWN;

  /** The problem description parser. */
  private Parser parser = null;

}
