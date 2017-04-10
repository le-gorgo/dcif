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

package org.nabelab.solar.pfield;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import org.nabelab.solar.CFP;
import org.nabelab.solar.Clause;
import org.nabelab.solar.ClauseTypes;
import org.nabelab.solar.Conseq;
import org.nabelab.solar.ConseqSet;
import org.nabelab.solar.DebugTypes;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.SymTable;
import org.nabelab.solar.Term;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.parser.Parser;

/**
 * @author nabesima
 *
 */
public class PField implements TermTypes, DebugTypes {

  /**
   * Constructs a empty production field.
   * @param env the environment.
   * @param opt the options.
   */
  public PField(Env env, Options opt) {
    this.env = env;
    this.opt = opt;
  }
  
  /**
   * Constructs a production field literal from the string.
   * @param env     the environment.
   * @param opt     the options.
   * @param literal the string representation of a production field literal (ex. "-p(a)")
   */
  public static PField parse(Env env, Options opt, String pfield) throws ParseException {
    return new Parser(env, opt).pfield(new BufferedReader(new StringReader(pfield)));
  }  

  /**
   * Adds an allowed literal.
   * @param literal the added literal.
   */
  public void add(PLiteral literal) {
    ListIterator<PLiteral> i = null;
    if (literal.isSpecial()) {
      switch (literal.getSign()) {
      case PLiteral.POS:
        // Removes all specified positive predicates.
        i = literals.listIterator();
        while (i.hasNext())
          if (i.next().getSign() == PLiteral.POS)
            i.remove();
        break;        
      case PLiteral.NEG:
        // Removes all specified negative predicates.
        i = literals.listIterator();
        while (i.hasNext())
          if (i.next().getSign() == PLiteral.POS)
            i.remove();
        break;        
      case PLiteral.BOTH:
        // Removes all specified predicates.
        literals.clear();
        break;
      default:
        assert(false);
      }
    }
    literals.add(literal);
  }
  
  /**
   * Adds a group cardinality constraint
   * @param constraint the added constraint
   */
  public void addConstraint(PFCardConstraint constraint){
	  addConstraints.add(constraint);
  }
  
  /**
   * get all the additional cardinality constraints (by reference). 
   */
  public List<PFCardConstraint> getAddConstraints(){
	  return addConstraints;
  }
  
  /**
   * Returns the maximum term depth limitation.
   * @return the maximum term depth limitation.
   */
  public int getMaxTermDepth() {
    return maxTermDepth;
  }
  
  /**
   * Sets the maximum term depth limitation.
   * @param depth the maximum term depth.
   */
  public void setMaxTermDepth(int depth) {
    maxTermDepth = depth;
  }

  /**
   * Returns the maximum length of consequences.
   * @return the maximum length of consequences.
   */
  public int getMaxLength() {
    return maxLength;
  }
  
  /**
   * Sets the consequence length limitation.
   * @param length the maximum length of consequences.
   */
  public void setMaxLength(int length) {
    maxLength = length;
  }

  /**
   * Returns the set of literals belongs to the production field.
   * @return the set of literals belongs to the production field.
   */
  public List<PLiteral> getPLiterals() {
    return literals;
  }
  
  /**
   * Converts the unstable production field into the stable one.
   * @param cfp the consequence finding problem which contains this production field. 
   * @param out the output of debug messages. 
   */
  public void convertToStablePField(CFP cfp, PrintWriter out) {

    // Finds out non-maximally general literals.
    List<PLiteral> targets= new LinkedList<PLiteral>();
    for (PLiteral plit : literals) 
      if (!plit.isMaxGeneral())
        targets.add(plit);

    // Removes all non-maximally general literals
    literals.removeAll(targets);

    if (env.dbg(DBG_BRIDGE) && !targets.isEmpty())
      out.println("[Bridge formula translation]");

    // Converts each non-maximally general literal into the general one.
    for (PLiteral plit : targets) {

      // Generates a new literal instead of plit.

      // Generates a new special name of the new literal.
      String newName = plit.getTerm().toString();
      newName = newName.replace('(', '[');
      newName = newName.replace(')', ']');

      // Registers the new name to the symbol table.
      int newArity = plit.getNumVars();
      int newNameID = env.getSymTable().putPredicate(newName, newArity, BRIDGE);

      // Generates a new term.
      int[] nameArray = new int[newArity + 1];
      int[] typeArray = new int[newArity + 1];
      int[] nextArray = new int[newArity + 1];
      nameArray[0] = newNameID;
      typeArray[0] = PREDICATE;
      nextArray[0] = nextArray.length;
      for (int i = 1; i < nextArray.length; i++) {
        nameArray[i] = i - 1; // variable name
        typeArray[i] = VARIABLE;
        nextArray[i] = i + 1;
      }
      Term newTerm = new Term(env, nameArray, typeArray, nextArray);

      // Generates a new pliteral
      PLiteral newPLit = new PLiteral(plit);
      newPLit.setTerm(newTerm);

      // Registers the new pliteral to the production field.
      literals.add(newPLit);

      if (env.dbg(DBG_BRIDGE)) {
        out.println(" DEL LIT: " + plit );
        out.println(" ADD LIT: " + newPLit);
      }
      
      // Generates a bridge formula for plit and newPLit.
      Term orgTerm = plit.getTerm();
      ArrayList<Literal> lits = new ArrayList<Literal>();
      Clause clause = null;
      switch (plit.getSign()) {
      case PLiteral.POS: 
        lits.add(new Literal(env, false, orgTerm));
        lits.add(new Literal(env, true, newTerm));
        cfp.addClause(clause = new Clause(env, "bridge", ClauseTypes.AXIOM, lits));
        if (env.dbg(DBG_BRIDGE))
          out.println(" ADD CLS: " + clause);
        break;
      case PLiteral.NEG:
        lits.add(new Literal(env, true, orgTerm));
        lits.add(new Literal(env, false, newTerm));
        cfp.addClause(clause = new Clause(env, "bridge", ClauseTypes.AXIOM, lits));
        if (env.dbg(DBG_BRIDGE))
          out.println(" ADD CLS: " + clause);
        break;
      case PLiteral.BOTH:
        lits.add(new Literal(env, false, orgTerm));
        lits.add(new Literal(env, true, newTerm));
        cfp.addClause(clause = new Clause(env, "bridge", ClauseTypes.AXIOM, lits));
        if (env.dbg(DBG_BRIDGE))
          out.println(" ADD CLS: " + clause);        
        lits = new ArrayList<Literal>();
        lits.add(new Literal(env, true, orgTerm));
        lits.add(new Literal(env, false, newTerm));
        cfp.addClause(clause = new Clause(env, "bridge", ClauseTypes.AXIOM, lits));
        if (env.dbg(DBG_BRIDGE))
          out.println(" ADD CLS: " + clause);
        break;
      default:
        assert (false);
        break;
      }
    }
  }
  
  /**
   * Converts the set of consequences which may contain bridge formula for unstable production field, into the original format.
   * @param conseqSet the set of consequences to be converted.
   * @return the set of original consequences.
   */
  public ConseqSet convertToOrgFmt(ConseqSet conseqSet) throws ParseException {
    if (conseqSet == null)
      return null;
    
    ConseqSet newConseqSet = new ConseqSet(env);
    SymTable  symTable = env.getSymTable();

    for (Conseq conseq : conseqSet) {

      if (!conseq.hasBridgePred()) {
        newConseqSet.add(conseq);
        continue;
      }
      
      if (env.dbg(DBG_BRIDGE)) 
        System.out.println("BRG: " + conseq);

      List<Literal> newLits = new ArrayList<Literal>();
      for (Literal lit : conseq) {
        Term term = lit.getTerm();
        int name = term.getStartName();
        int type = term.getStartType();
      
        if (!symTable.hasTag(name, type, BRIDGE))
          newLits.add(lit);
        else {
          String bridge = symTable.get(name, type);
          int begin = bridge.indexOf('[');
          int end   = bridge.indexOf(']');
          assert(begin != -1 && end != -1);
          String   orgName = bridge.substring(0, begin);
          String[] orgArgs = bridge.substring(begin + 1, end).split(",");
          
          
          List<Term> newArgs = new ArrayList<Term>();
          for (int i=0, j=0; i < orgArgs.length; i++) {
            // Variable?
            if (orgArgs[i].startsWith("_"))
              newArgs.add(term.getArg(j++));
            else
              newArgs.add(Term.parse(env, opt, orgArgs[i]));
          }
          
          Term newTerm = Term.createPredicate(env, orgName, newArgs);
          Literal newLit = new Literal(env, lit.getSign(), newTerm);

          newLits.add(newLit);
          
          if (env.dbg(DBG_BRIDGE)) 
            System.out.println(" " + lit + " -> " + newLit);
        }        
      }
      
      Conseq newConseq = new Conseq(env, newLits);

      // If the new consequence is subsumed by an axiom, then remove it.
      Clause subsuming = conseqSet.findSubsuming(newConseq.getFVec(true), newConseq);
      if (subsuming instanceof Clause)
        continue;      
      
      List<Clause> usedClauses = conseq.getUsedClauses();
      if (usedClauses != null) {
        List<Clause> newUsedClauses = new ArrayList<Clause>();
        for (Clause usedClause : usedClauses)
          if (!usedClause.hasBridgePred())
            newUsedClauses.add(usedClause);
        newConseq.setUsedClauses(newUsedClauses);            
      }
      newConseq.setProof(conseq.getProof());

      newConseqSet.add(newConseq);
    }    
    
    return newConseqSet;
  }

  /**
   * Returns true if this production field is empty.
   */
  public boolean isEmpty() {
    return literals.isEmpty();
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();
    
    str.append(literals.toString());
    
    if (maxTermDepth != UNLIMITED)
      str.append(":" + maxTermDepth);
    if (maxLength != UNLIMITED)
      str.append(" <= " + maxLength);
    
    return str.toString();
  }
  
  /** Depth or length is not limited. */
  public final static int UNLIMITED = -1;
  /** Depth or length is not defined. */
  public final static int NOT_DEFINED = -2;
  
  /** The environment. */
  private Env env = null;
  /** The options. */
  private Options opt = null;
  /** Allowed literals */
  private List<PLiteral> literals = new LinkedList<PLiteral>();
  /** The maximum term depth limitation */
  private int maxTermDepth = UNLIMITED;
  /** The maximum length limitation */
  private int maxLength = UNLIMITED;
  /** The additional cardinality constraints on groups */
  private List<PFCardConstraint> addConstraints= new ArrayList<PFCardConstraint>();
}
