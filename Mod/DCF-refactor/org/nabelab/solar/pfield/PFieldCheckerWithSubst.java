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

import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.SymTable;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.Unifiable;
import org.nabelab.solar.indexing.DiscTree;

/**
 * @author nabesima
 *
 */
public class PFieldCheckerWithSubst extends PFieldChecker implements TermTypes {

  // TODO: The current implementation does not support the term-depth limitation.

  /**
   * Constructs a production field checker.
   * @param env a consequence finding problem.
   */
  public PFieldCheckerWithSubst(Env env, PField pfield) {
    super(env, pfield);

    List<PLiteral> specials   = new ArrayList<PLiteral>();
    List<PLiteral> generals   = new ArrayList<PLiteral>();
    List<PLiteral> specifieds = new ArrayList<PLiteral>();
    for (PLiteral plit : pfield.getPLiterals()) {
      if (plit.isSpecial())
        specials.add(plit);
      else if (plit.isMaxGeneral())
        generals.add(plit);
      else
        specifieds.add(plit);        
    }
    
    SymTable symTable = env.getSymTable();
    positives = new PFieldItem[symTable.getNumSyms(PREDICATE)];
    negatives = new PFieldItem[symTable.getNumSyms(PREDICATE)];
    
    for (PLiteral plit : specials) {
      for (int name=0; name < symTable.getNumSyms(PREDICATE); name++) {
        PFieldItem item = new PFieldItem(plit, maxLenCounter);
        switch (plit.getSign()) {
        case PLiteral.POS:
          positives[name] = item; 
          break;
        case PLiteral.NEG:
          negatives[name] = item; 
          break;
        case PLiteral.BOTH:        // Shares the same item.
          positives[name] = item;
          negatives[name] = item; 
          break;
        default:
          assert(false);
        }
      }      
    }
    
    for (PLiteral plit : generals) {
      int name = plit.getName();
      int sign = plit.getSign();
      PFieldItem item = new PFieldItem(plit, maxLenCounter);
      switch (sign) {
      case PLiteral.POS:
        positives[name] = item; 
        break;
      case PLiteral.NEG:
        negatives[name] = item; 
        break;
      case PLiteral.BOTH:        // Shares the same item.
    	PLiteral plitPos = new PLiteral(PLiteral.POS, plit.getTerm());
    	PLiteral plitNeg = new PLiteral(PLiteral.NEG, plit.getTerm());
        positives[name] = new PFieldItem(plitPos, maxLenCounter);
        negatives[name] = new PFieldItem(plitNeg, maxLenCounter); 
        break;
      default:
        assert(false);
      }
    }    
    
    pfieldItems = new DiscTree<PFieldItem>(env, true);    
    for (PLiteral plit : specifieds) {
      switch (plit.getSign()) {
      case PLiteral.POS:      
      {
        Literal lit = new Literal(env, true, plit.getTerm());
        PFieldItem item = new PFieldItem(plit, maxLenCounter);
        pfieldItems.add(lit, item);
        break;
      }
      case PLiteral.NEG:
      {
        Literal lit = new Literal(env, false, plit.getTerm());
        PFieldItem item = new PFieldItem(plit, maxLenCounter);
        pfieldItems.add(lit, item);
        break;
      }
      case PLiteral.BOTH:
      {
        Literal lit1 = new Literal(env, true,  plit.getTerm());
        Literal lit2 = new Literal(env, false, plit.getTerm());
        PFieldItem item = new PFieldItem(plit, maxLenCounter);
        pfieldItems.add(lit1, item);
        pfieldItems.add(lit2, item);
        break;
      }
      }
    }
  }

  /**
   * Returns the list of unifiable production field items.
   * @param lit the specified literal.
   * @return the list of unifiable production field items.
   */
  public List<Unifiable<PFieldItem>> getUnifiableItems(Literal lit) {
    if (maxLenCounter.get() == 0)
      return null;
    
    List<Unifiable<PFieldItem>> unifs = null; 
    PFieldItem item = (lit.isPositive()) ? positives[lit.getName()] : negatives[lit.getName()];
    if (item != null && item.isSkippable()) {
      unifs = new ArrayList<Unifiable<PFieldItem>>();
      unifs.add(new Unifiable<PFieldItem>(null, item, 0));      
    }
    
    List<Unifiable<PFieldItem>> cands = pfieldItems.findUnifiable(lit);
    if (cands == null)
      return unifs;
     
    for (Unifiable<PFieldItem> unif : cands) {
      item = unif.getObject();
      if (item.isSkippable()) {
        if (unifs == null)
          unifs = new ArrayList<Unifiable<PFieldItem>>();
        unifs.add(unif);
      }
    }
    
    return unifs;
  }
  
  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();
    
    str.append("Max length     : " + maxLenCounter    + "\n");
    str.append("Max term depth : " + maxTermDepthCounter + "\n");
    
    str.append("Items : \n" + pfieldItems);
    
    return str.toString();
  }

  /** The allowed number of positive literals. */
  protected PFieldItem[] positives = null;
  /** The allowed number of negative literals. */
  protected PFieldItem[] negatives = null;
  /** The items of a production field. */
  protected DiscTree<PFieldItem> pfieldItems = null;

}
