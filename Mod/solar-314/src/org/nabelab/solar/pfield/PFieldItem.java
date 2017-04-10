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

import org.nabelab.solar.PLiteral;

/**
 * @author nabesima
 *
 */
public class PFieldItem {
  
  /**
   * Construct a production field literal specification.
   * @param pliteral       a production field literal.
   * @param maxLenCounter  a counter to check the maximum length.
   */
  public PFieldItem(PLiteral pliteral, PFieldCounter maxLenCounter) {
    this.pliteral = pliteral;    
    this.counter = new PFieldCounter(pliteral.getMaxLength());
    this.maxLenCounter = maxLenCounter;
    if (!pliteral.isSpecial())
      this.numVars = pliteral.getTerm().getNumVars();    
    this.occu = new PFieldCounter(0);
  }
  
  /**
   * Add a reference to a group counter that this PFieldItem belongs to, so that skipping this literal affects the group constraint
   * @param groupCounter
   */
  public void addToGroup(PFieldCounter groupCounter) {
	  maxLenGroupCounters.add(groupCounter);
	  occu.inc();

  }
  
  /**
   * Remove a reference to a group counter
   * @param groupCounter
   */
  public void removeFromGroup(PFieldCounter groupCounter) {
	  maxLenGroupCounters.remove(groupCounter);
  }
  /**
   * clear references to group counters
   */
  public void clearGroups() {
	  maxLenGroupCounters.clear();
  }
  
  
  /**
   * Returns true if this item can be skipped.
   * @return true if this item can be skipped.
   */
  public boolean isSkippable() {
    if (maxLenCounter.get() == 0)
      return false;
    if (counter.get() > 0 || counter.get() == PField.UNLIMITED)
      return true;
    return false;    
  }

  /**
   * Updates the counters by skipping.
   */
  public void skip() {
    maxLenCounter.dec();
    counter.dec();
    for (PFieldCounter ctr:maxLenGroupCounters)
    	ctr.dec();
  }

  /**
   * Updates the counters by unskipping.
   */
  public void unskip() {
    maxLenCounter.inc();
    counter.inc();
    for (PFieldCounter ctr:maxLenGroupCounters)
    	ctr.inc();
  }

  /**
   * Returns the production field literal.
   * @return the production field literal.
   */
  public PLiteral getPLiteral() {
    return pliteral;
  }
  
  /**
   * Returns the number of variables in the production field item.
   * @return the number of variables in the production field item.
   */
  public int getNumVars() {
    return numVars;
  }
  
  public int getOcc(){
	  return occu.get();
  }
  
  public List<PFieldCounter> getMaxLEnGroup(){
	  return maxLenGroupCounters;
  }
  
  /**
   * Returns a string representation of this object.
   * @return a string representation of this object.
   */
  public String toString() {
    return pliteral + ":" + counter;
  }

  /** The production field literal. */
  private PLiteral pliteral = null;
  /** The number of variables in the production field literal. */
  private int numVars = 0;
  /** The number of occurrences. */
  private PFieldCounter counter = null;
  /** The counter to check the maximum length */
  private PFieldCounter maxLenCounter = null;
  /** The counters for group cardinality constraint the literal belongs to */
  private List<PFieldCounter> maxLenGroupCounters = new ArrayList<PFieldCounter>();
  
  private PFieldCounter occu = null;
    
}
