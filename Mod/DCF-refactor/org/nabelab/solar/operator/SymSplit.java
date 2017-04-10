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
package org.nabelab.solar.operator;

import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.Stats;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.ProofStep;
import org.nabelab.solar.proof.SymSplitStep;

public class SymSplit extends Operator {

  /**
   * Constructs a symmetrical splitting operator which is applied to the specified node.
   * @param env   the environment.
   * @param node  the specified node.
   * @param lit   the literal.
   */
  public SymSplit(Env env, Node node, Literal lit) {
    super(env, node);
    this.lit1 = lit;
    this.lit2 = null;
    this.stats.incProds(Stats.SYMMETRY_SPLITTING);
  }
  
  /**
   * Constructs a symmetrical splitting operator which is applied to the specified node.
   * @param env   the environment.
   * @param node  the specified node.
   * @param lit1  the first literal.
   * @param lit2  the second literal.
   */
  public SymSplit(Env env, Node node, Literal lit1, Literal lit2) {
    super(env, node);
    this.lit1 = lit1;
    this.lit2 = lit2;
    this.stats.incProds(Stats.SYMMETRY_SPLITTING);
  }
  
  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    
    if (lit2 == null) {
      // Applies the operator.
      super.apply();
      node.addSymSplittedNode(lit1);
    }
    else {
      // Applies the operator.
      varTable.addVars(1);
      super.apply();
      node.addSymSplittedNodes(lit1, lit2);
    }
    
    node.addTag(SYMMETRY_SPLITTED);
    stats.incSuccs(Stats.SYMMETRY_SPLITTING);
    return true;
  }  
  
  /**
   * Cancels this operator.
   */
  public void cancel() {
    node.removeTag(SYMMETRY_SPLITTED);
    if (lit2 == null) {
      node.removeSymSplittedNodes();
      super.cancel();
    }
    else {
      node.removeSymSplittedNodes();
      super.cancel();
      varTable.removeVars(1);
    }
  }

  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return new SymSplitStep(env, lit1, lit2);
  }
  
  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    if (lit2 == null)
      return "SPL [" + lit1 + "](" + getNumSyms() + "s)";
    else
      return "SPL [" + lit1 + ", " +lit2 + "](" + getNumSyms() + "s)";
  }

  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[SPL]";
  }
  
  /** The first literal which is used to extend this node. */
  private Literal lit1 = null;
  /** The second literal which is used to extend this node. */
  private Literal lit2 = null;
  
}
