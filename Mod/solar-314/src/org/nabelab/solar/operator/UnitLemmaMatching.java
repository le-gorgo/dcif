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

import org.nabelab.solar.Clause;
import org.nabelab.solar.Conseq;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.Stats;
import org.nabelab.solar.proof.ProofStep;
import org.nabelab.solar.proof.UnitLemmaMatchingStep;

/**
 * @author nabesima
 *
 */
public class UnitLemmaMatching extends Operator {

  /**
   * Constructs a unit lemma matching operator which is applied to the specified node.
   * @param env    the environment.
   * @param node   the specified node.
   * @param ulemma the unit lemma.
   */
  public UnitLemmaMatching(Env env, Node node, Conseq ulemma) {
    super(env, node);
    this.ulemma = ulemma;
    this.mandatory = true;
  }
  
  /**
   * Applies this operator.
   * @return true if the application of this operator succeeds.
   */
  public boolean apply() {
    super.apply();
    node.addTag(UNIT_LEMMA_MATCHED);
    tableau.stats().inc(Stats.UNIT_LEMMA_MATCHING);
    return true;
  }

  /**
   * Cancels this operator.
   * @Returns true if the cancellation succeeded.
   */
  public void cancel() {
    node.removeTag(UNIT_LEMMA_MATCHED);
    super.cancel();
  }

  /**
   * Returns the unit lemma.
   * @return the unit lemma.
   */
  public Clause getClause() {
    return ulemma;
  }
  
  /**
   * Converts this operator to the proof step.
   * @return the proof step.
   */
  public ProofStep convert() {
    return new UnitLemmaMatchingStep(env, ulemma);
  }

  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    Literal lit = node.getLiteral();
    if (lit.isPositive())
      return "ULM [-" + lit.getTerm() + "]";
    else
      return "ULM [+" + lit.getTerm() + "]";
  }
  
  /**
   * Returns a simple string representation of this object.
   * @return a simple string representation of this object.
   */
  public String toSimpleString() {
    return "[ULM]";
  }
  
  /** The unit lemma  which complementary subsumes the node. */
  private Conseq ulemma = null;
}
