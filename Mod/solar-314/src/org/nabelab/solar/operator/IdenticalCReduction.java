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
import org.nabelab.solar.proof.IdenticalCReductionStep;
import org.nabelab.solar.proof.ProofStep;

/**
 * @author nabesima
 *
 */
public class IdenticalCReduction extends Operator {

  /**
   * Constructs an identical c-reduction operator which is applied to the specified node.
   * @param env    the environment.
   * @param node   the specified node.
   * @param target the ancestor node that has a folding-up lemma that subsumes the node.
   * @param solved the already solved node.
   */
  public IdenticalCReduction(Env env, Node node, Node target, Node solved) {
    super(env, node);
    this.holder = target;
    this.solved = solved;
    this.mandatory = true;
  }
  
  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
 * @throws ParseException 
   */
  public boolean apply() throws ParseException {
    super.apply();
    node.setReductionTarget(holder);
    node.addTag(IDENTICAL_C_REDUCED);
    tableau.stats().incSuccs(Stats.IDENTICAL_C_REDUCTION);
    return true;
  }

  /**
   * Cancels this operator.
   * @Returns true if the cancellation succeeded.
   */
  public void cancel() {
    node.clearReductionTarget();
    node.removeTag(IDENTICAL_C_REDUCED);
    super.cancel();
  }

  /**
   * Returns the target node of the identical c-reduction.
   * @return the target node of the identical c-reduction.
   */
  public Node getHolderNode() {
    return holder;
  }
 
  /**
   * Returns the already solved node which is a descendant of the holder.
   * @return the already solved node which is a descendant of the holder.
   */
  public Node getSolvedNode() {
    return solved;
  }
  
  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return new IdenticalCReductionStep(env, node.getDepth() - holder.getDepth());
  }
  
  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    Literal lit = node.getLiteral();
    if (lit.isPositive())
      return "FLD [-" + lit.getTerm() + "]";
    else
      return "FLD [+" + lit.getTerm() + "]";
  }
  
  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[FLD]";
  }
  
  /** The ancestor node that has a folding-up lemma that subsumes the node. */
  private Node holder = null;
  /** The already solved node which is a descendant of the holder. */
  private Node solved = null;
}

