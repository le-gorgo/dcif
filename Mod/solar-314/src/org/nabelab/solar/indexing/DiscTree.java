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

package org.nabelab.solar.indexing;

import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Subst;
import org.nabelab.solar.Term;
import org.nabelab.solar.TermCont;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.Unifiable;
import org.nabelab.solar.VarTable;
import org.nabelab.solar.pfield.PFieldItem;
import org.nabelab.solar.util.ArrayStack;
import org.nabelab.solar.util.Pair;
import org.nabelab.util.LightArrayList;


/**
 * @author nabesima
 *
 */
public class DiscTree<E> implements TermTypes {

  /**
   * Constructs a discrimination tree.
   * @param env the consequence finding problem to which this belongs.
   * @param newNameSpace if true, then all variables in this tree belong to another name space.
   */
  public DiscTree(Env env, boolean newNameSpace) {
    this.env          = env;
    this.varTable     = env.getVarTable();
    this.newNameSpace = newNameSpace;
    this.posRoot      = new DiscNode<E>(env, this);
    this.negRoot      = new DiscNode<E>(env, this);
  }

  /**
   * Adds the specified instantiated literal and the associated objects to this tree.
   * @param lit the instantiated literal to add.
   * @param pclause the object associated with the literal.
   * @return the leaf node to which the object is registered.
   */
  public DiscNode<E> addInstantiated(Literal lit, E object) {
    return addInstantiated(lit.isPositive() ? posRoot : negRoot, lit.getTerm(), object);
  }

  /**
   * Adds the specified term and the associated objects to this tree.
   * @param root    the root node of the tree
   * @param term    the term to add.
   * @param pclause the object associated with the literal.
   * @return the leaf node to which the object is registered.
   */
  private DiscNode<E> addInstantiated(DiscNode<E> root, Term term, E object) {
    DiscNode<E> node = root;
    int         cur  = term.getStart();
    int         end  = term.getNext(cur);
    while (cur != end) {    // Loop for each term.
      int name = term.getName(cur);
      int type = term.getType(cur);
      if (type == VARIABLE) {
        name += term.getOffset();
        if (numVars < name + 1)
          numVars = name + 1;
      }
      DiscNode<E> child = null;
      if ((child = node.findChild(name, type)) == null) {
        child = node.addChild(name, type);
        if (cur >= 2) {
          int         anc = cur - 1;          // The index of an ancestor.
          DiscNode<E> src = node;             // A source candidate of a jump link.
          while (anc > term.getStart()) {     // Predicate symbols have not jump links.
            if (cur + 1 == term.getNext(anc)) {
              Term subterm = Term.newSubTerm(term, anc);
              src.addJumpLink(child, subterm);
            }
            anc--;
            src = src.getParent();
          }
        }
      }
      node = child;
      cur++;
    }
    node.addLeaf(object);
    numClauses++;
    return node;
  }

  /**
   * Adds the specified instantiated literal and the associated objects to this tree.
   * @param lit the instantiated literal to add.
   * @param object the object associated with the literal.
   * @return the leaf node to which the object is registered.
   */
  public DiscNode<E> add(Literal lit, E object) {
    return add(lit.isPositive() ? posRoot : negRoot, lit.getTerm(), object);
  }

  /**
   * Adds the specified term and the associated objects to this tree.
   * @param root    the root node of the tree
   * @param term    the term to add.
   * @param object the object associated with the literal.
   * @return the leaf node to which the object is registered.
   */
  private DiscNode<E> add(DiscNode<E> root, Term term, E object) {
    DiscNode<E> node     = root;
    int      cur      = term.getStart();
    int      end      = term.getNext(cur);
    TermCont termCont = null;
    while (true) {            // Loop for unfinished terms.
      while (cur != end) {    // Loop for each term.
        int tname = term.getName(cur);
        int ttype = term.getType(cur);
        if (ttype == VARIABLE) {
          Term tvar = varTable.getTailVar(tname + term.getOffset());
          int tvarname = tvar.getStartName() + tvar.getOffset();
          Term tval = varTable.getValue(tvarname);
          if (tval != null) {
            termCont = new TermCont(term, cur + 1, end, termCont);
            term = tval;
            cur  = term.getStart();
            end  = term.getNext(cur);
            continue;
          }
          if (numVars < tvarname + 1)
            numVars = tvarname + 1;
          tname = tvarname;
        }
        DiscNode<E> child = null;
        if ((child = node.findChild(tname, ttype)) == null) {
          child = node.addChild(tname, ttype);
          // Adds a jump link if the node is the last arguments.
          if (child.getArity() == 0 && !node.isRoot()) {
            int      num = 1;          // The number of arguments.
            int      anc = cur - 1;    // The index of an ancestor.
            DiscNode<E> src = node;    // A source candidate of a jump link.
            TermCont tc  = termCont;
            Term     t   = term;
            while (true) {
              while (anc < t.getStart()) {
                t   = tc.getTerm();
                anc = tc.getCur() - 2;
                tc  = tc.getPrev();
              }
              if (t.getType(anc) == VARIABLE) {
                int n = t.size(anc);
                while (--n > 0) {
                  num -= src.getArity();
                  src = src.getParent();
                  num++;
                }
                //num+=t.size(false, anc);
              }
              if (src.getType() == PREDICATE)
                break;
              else if (src.getArity() > num)
                break;
              else if (src.getArity() < num)
                num -= src.getArity();
              else { // (src.getArity() == num)
                Term subterm = Term.newSubTerm(t, anc);
                src.addJumpLink(child, subterm);
                num -= src.getArity();
              }
              src = src.getParent();
              num++;
              anc--;
            }
          }
        }
        node = child;
        cur++;
      }
      // try the next unfinished term.
      if (termCont == null) break;
      term = termCont.getTerm();
      cur  = termCont.getCur();
      end  = termCont.getEnd();
      termCont = termCont.getPrev();
    }
    node.addLeaf(object);
    numClauses++;
    return node;
  }

  /**
   * Removes the specified literal and the associated objects to this tree.
   * @param lit    the literal to add.
   * @param object the object associated with the literal.
   * @return true if the specified literal and object are removed.
   */
  public boolean remove(Literal lit, E object) {
    return remove(lit.isPositive() ? posRoot : negRoot, lit.getTerm(), object);
  }

  /**
   * Removes the specified term and the associated objects to this tree.
   * @param root   the root node of the tree
   * @param t      the term to add.
   * @param object the object associated with the literal.
   * @return the leaf node to which the object is registered.
   */
  private boolean remove(DiscNode<E> root, Term term, E object) {
    DiscNode<E> node = root;
    int cur = term.getStart();
    int end = term.getNext(cur);
    TermCont termCont = null;
    while (true) {            // Loop for unfinished terms.
      while (cur != end) {    // Loop for each term.
        int tname = term.getName(cur);
        int ttype = term.getType(cur);
        if (ttype == VARIABLE) {
          Term tvar = varTable.getTailVar(tname + term.getOffset());
          int tvarname = tvar.getStartName() + tvar.getOffset();
          Term tval = varTable.getValue(tvarname);
          if (tval != null) {
            //if (cur + 1 != end)
            termCont = new TermCont(term, cur + 1, end, termCont);
            term = tval;
            cur  = term.getStart();
            end  = term.getNext(cur);
            continue;
          }
          tname = tvarname;
        }
        DiscNode<E> child = node.findChild(tname, ttype);
        assert(child != null);
        node = child;
        cur++;
      }
      // try the next unfinished term.
      if (termCont == null) break;
      term = termCont.getTerm();
      cur  = termCont.getCur();
      end  = termCont.getEnd();
      termCont = termCont.getPrev();
    }
    return remove(node, object);
  }

  /**
   * Removes the specified literal and the associated objects to this tree.
   * @param lit    the literal to add.
   * @param object the object associated with the literal.
   * @return true if the specified literal and object are removed.
   */
  public boolean removeInstantiated(Literal lit, E object) {
    return removeInstantiated(lit.isPositive() ? posRoot : negRoot, lit.getTerm(), object);
  }

  /**
   * Removes the specified term and the associated objects to this tree.
   * @param root   the root node of the tree
   * @param t      the term to add.
   * @param object the object associated with the literal.
   * @return the leaf node to which the object is registered.
   */
  private boolean removeInstantiated(DiscNode<E> root, Term term, E object) {
    DiscNode<E> node = root;
    int cur = term.getStart();
    int end = term.getNext(cur);
    while (cur != end) {    // Loop for each term.
      int tname = term.getName(cur);
      int ttype = term.getType(cur);
      DiscNode<E> child = node.findChild(tname, ttype);
      assert(child != null);
      node = child;
      cur++;
    }
    return remove(node, object);
  }

  /**
   * Removes the specified object which is contained in the specified node.
   * @param node   the specified node.
   * @param object the specified object.
   * @return true if the specified object is removed.
   */
  private boolean remove(DiscNode<E> node, E object) {
    boolean removed = node.removeLeaf(object);
    numClauses--;
    if (node.hasLeaves())
      return removed;
    while (!node.isRoot()) {
      DiscNode<E> parent = node.getParent();
      parent.removeChild(node);
      if (parent.hasChildren())
        break;
      node = parent;
    }
    return removed;
  }

  /**
   * Finds an unifiable literal with the specified literal and returns the associated objects.
   * @param lit the literal.
   * @return the associated objects.
   */
  public List<Unifiable<E>> findUnifiable(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? posRoot : negRoot;
    Term     term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    if (node == null)
      return null;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    List<Unifiable<E>> out = findUnifiable(term, node);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return out;
  }

  /**
   * Finds an unifiable literal with the complement of the specified literal and returns the associated objects.
   * @param lit the literal.
   * @return the associated objects.
   */
  public List<Unifiable<E>> findCompUnifiable(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? negRoot : posRoot;
    Term     term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    if (node == null)
      return null;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    List<Unifiable<E>> out = findUnifiable(term, node);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return out;
  }

  private List<Unifiable<E>> findUnifiable(Term term, DiscNode<E> node) {
    List<Unifiable<E>> out = null;
    int state = varTable.state();
    int cur   = term.getStart();
    int end   = term.getNext(cur);
    JumpLink<E>    link      = null;
    int            linkNo    = 0;
    TermCont       termCont  = null;
    SubstList      glist     = null;
    candStack.clear();
    while (true) {        // Loop for each candidate.
      MID_LOOP:
      while (true) {      // Loop for unfinished terms.
        while (true) {    // Loop for each term.
          if (term.getType(cur) == VARIABLE) {
            Term tval = varTable.getTailValue(term.getName(cur) + term.getOffset());
            if (tval != null) {
              if (node.getType() != VARIABLE) {
                node = node.getParent().findChild(tval.getStartName(), tval.getStartType());
                if (node == null)
                  break MID_LOOP;
              }
              if (cur + 1 != end)
                termCont = new TermCont(term, cur + 1, end, termCont);
              term = tval;
              cur  = term.getStart();
              end  = term.getNext(cur);
              continue;
            }
            // Find the next candidate.
            if (linkNo + 1 < node.getNumJumpLinks()) {
              candStack.push(new Cand<E>(
                  new TermCont(term, cur, end, termCont),
                  node, linkNo + 1, glist, varTable.state()));
            }
            else if (node.getRight() != null) {
              candStack.push(new Cand<E>(
                  new TermCont(term, cur, end, termCont),
                  node.getRight(), 0, glist, varTable.state()));
            }
            // Try to unify.
            Term nterm = null;
            if (linkNo < node.getNumJumpLinks()) {
              link = node.getJumpLink(linkNo);
              nterm = (treeOffset == 0) ? link.getTerm() : Term.newOffset(link.getTerm(), treeOffset);
            }
            else if (node.getType() == VARIABLE) {
              //nterm = (treeOffset == 0) ? node.getTerm() : Term.newOffset(node.getTerm(), treeOffset);
              nterm = varTable.getTailVar(node.getName() + treeOffset);
            }
            else {
              nterm = node.getTerm();
            }
            Subst g = null;
            if ((g = Term.unify(term, cur, nterm, nterm.getStart())) == null)
              break MID_LOOP;
            if (!g.isEmpty())
              glist = new SubstList(g, glist);
          }
          else {    // term.getType(cur) != VARIABLE
            if (node.getType() == VARIABLE) {
              // Find the next candidate.
              DiscNode<E> next = node.getRight();
              if (next != null && next.getType() != VARIABLE)
                next = node.getParent().findChild(term.getName(cur), term.getType(cur));
              if (next != null)
                candStack.push(new Cand<E>(
                    new TermCont(term, cur, end, termCont),
                    next, 0, glist, varTable.state()));
              // Try to unify.
              Term nvar = varTable.getTailVar(node.getName() + treeOffset);
              Subst g = null;
              if ((g = Term.unify(term, cur, nvar, nvar.getStart())) == null)
                break MID_LOOP;
              if (!g.isEmpty())
                glist = new SubstList(g, glist);
            }
          }
          // Go to the next symbol.
          if (node.getType() == VARIABLE)
            cur = term.getNext(cur);
          else
            cur++;
          if (link != null) {
            node = link.getDest();
            link = null;
          }
          if (cur == end)
            break;
          DiscNode<E> c = node.getFirstChild();
          if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
            c = node.findChild(term.getName(cur), term.getType(cur));
            if (c == null) break MID_LOOP;
          }
          node = c;
          linkNo = 0;
        }
        // Try the next unfinished term.
        if (termCont == null) {
          // Reached to the leaf.
          if (out == null)
            out = new ArrayList<Unifiable<E>>();
          Subst g = (glist != null) ? glist.convert() : new Subst();
          LightArrayList<E> leaves = node.getLeaves();
          for (int i=0; i < leaves.size(); i++)
            out.add(new Unifiable<E>(new Subst(g), leaves.get(i), treeOffset));
          break;
        }
        term = termCont.getTerm();
        cur  = termCont.getCur();
        end  = termCont.getEnd();
        termCont = termCont.getPrev();
        DiscNode<E> c = node.getFirstChild();
        if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
          c = node.findChild(term.getName(cur), term.getType(cur));
          if (c == null) break MID_LOOP;
        }
        node = c;
        linkNo = 0;
      }
      // Try to the next candidate.
      if (candStack.isEmpty()) {
        varTable.backtrackTo(state);
        return out;
      }
      Cand<E> cand = candStack.pop();
      termCont = cand.getTermCont();
      term     = termCont.getTerm();
      cur      = termCont.getCur();
      end      = termCont.getEnd();
      termCont = termCont.getPrev();
      node     = cand.getNode();
      linkNo   = cand.getLinkNo();
      glist    = cand.getSubstPair();
      link     = null;
      varTable.backtrackTo(cand.getState());
    }
  }

  /**
   * Returns a clause which contains a literal that subsumes the specified literal.
   * @param lit the literal to check.
   * @return a clause which contains a literal that subsumes the specified literal.
   */
  public E isSubsumed(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? posRoot : negRoot;
    Term     term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    if (node == null)
      return null;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    E ret = isSubsumed(term, node);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return ret;
  }

  /**
   * Returns a clause which contains a complement literal that subsumes the specified literal.
   * @param lit the literal to check.
   * @return a clause which contains a complement literal that subsumes the specified literal.
   */
  public E isCompSubsumed(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? negRoot : posRoot;
    Term     term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    if (node == null)
      return null;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    E ret = isSubsumed(term, node);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return ret;
  }

  private E isSubsumed(Term term, DiscNode<E> node) {
    int  state = varTable.state();
    Term  torg = term;
    int    cur = term.getStart();
    int    end = term.getNext(cur);
    TermCont  termCont = null;
    SubstList    glist = null;
    candStack.clear();
    while (true) {        // Loop for each candidate.
      MID_LOOP:
      while (true) {      // Loop for unfinished terms.
        while (true) {    // Loop for each term.
          if (term.getType(cur) == VARIABLE) {
            Term tvar = varTable.getTailVar(term.getName(cur) + term.getOffset());
            int tvarname = tvar.getStartName() + tvar.getOffset();
            Term tval = varTable.getValue(tvarname);
            if (tval != null) {
              if (node.getType() != VARIABLE) {
                node = node.getParent().findChild(tval.getStartName(), tval.getStartType());
                if (node == null)
                  break MID_LOOP;
              }
              if (cur + 1 != end)
                termCont = new TermCont(term, cur + 1, end, termCont);
              term = tval;
              cur  = term.getStart();
              end  = term.getNext(cur);
              continue;
            }
            if (node.getType() != VARIABLE)
              break MID_LOOP;
            // ********************************
            // Find the next candidate.
            DiscNode<E> next = node.getRight();
            if (next != null && next.getType() == VARIABLE)
              candStack.push(new Cand<E>(
                  new TermCont(term, cur, end, termCont),
                  next, 0, glist, varTable.state()));
            // ********************************
            // Check to be subsumed.
            Term nterm = varTable.getTailVar(node.getName() + treeOffset);
            Subst g = null;
            if ((g = Term.subsumes(nterm, nterm.getStart(), term, cur, torg)) == null)
              break MID_LOOP;
            if (!g.isEmpty())
              glist = new SubstList(g, glist);
          }
          else {    // term.getType(cur) != VARIABLE
            if (node.getType() == VARIABLE) {
              // Find the next candidate.
              DiscNode<E> next = node.getRight();
              if (next != null && next.getType() != VARIABLE)
                next = node.getParent().findChild(term.getName(cur), term.getType(cur));
              if (next != null)
                candStack.push(new Cand<E>(
                    new TermCont(term, cur, end, termCont),
                    next, 0, glist, varTable.state()));
              // Check to be subsumed.
              Term nvar = varTable.getTailVar(node.getName() + treeOffset);
              Subst g = null;
              if ((g = Term.subsumes(nvar, nvar.getStart(), term, cur, torg)) == null)
                break MID_LOOP;
              if (!g.isEmpty())
                glist = new SubstList(g, glist);
            }
          }
          // Go to the next symbol.
          if (node.getType() == VARIABLE)
            cur = term.getNext(cur);
          else
            cur++;
          if (cur == end)
            break;
          DiscNode<E> c = node.getFirstChild();
          if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
            c = node.findChild(term.getName(cur), term.getType(cur));
            if (c == null) break MID_LOOP;
          }
          node = c;
        }
        // Try the next unfinished term.
        if (termCont == null) {
          // Reached to the leaf.
          LightArrayList<E> leaves = node.getLeaves();
          assert(!leaves.isEmpty());
          varTable.backtrackTo(state);
          return leaves.get(0);
        }
        term = termCont.getTerm();
        cur  = termCont.getCur();
        end  = termCont.getEnd();
        termCont = termCont.getPrev();
        DiscNode<E> c = node.getFirstChild();
        if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
          c = node.findChild(term.getName(cur), term.getType(cur));
          if (c == null) break MID_LOOP;
        }
        node = c;
      }
      // Try to the next candidate.
      if (candStack.isEmpty()) {
        varTable.backtrackTo(state);
        return null;
      }
      Cand<E> cand = candStack.pop();
      termCont = cand.getTermCont();
      term     = termCont.getTerm();
      cur      = termCont.getCur();
      end      = termCont.getEnd();
      termCont = termCont.getPrev();
      node     = cand.getNode();
      glist    = cand.getSubstPair();
      varTable.backtrackTo(cand.getState());
    }
  }

  /**
   * Returns an associated object  if this tree contains the specified literal.
   * @param lit the literal.
   * @return an associated object if this tree contains the specified literal.
   */
  public E contains(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? posRoot : negRoot;
    Term        term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    if (node == null)
      return null;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    E ret = contains(term, node);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return ret;
  }

  /**
   * Returns an associated object  if this tree complementary contains the specified literal.
   * @param lit the literal.
   * @return an associated object if this tree complementary contains the specified literal.
   */
  public E compContains(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? negRoot : posRoot;
    Term        term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    if (node == null)
      return null;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    E ret = contains(term, node);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return ret;
  }

  private E contains(Term term, DiscNode<E> node) {
    int state = varTable.state();
    int   cur = term.getStart();
    int   end = term.getNext(cur);
    TermCont termCont = null;
    candStack.clear();
    while (true) {        // Loop for each candidate.
      MID_LOOP:
      while (true) {      // Loop for unfinished terms.
        while (true) {    // Loop for each term.
          if (term.getType(cur) == VARIABLE) {
            Term tvar = varTable.getTailVar(term.getName(cur) + term.getOffset());
            int tvarname = tvar.getStartName() + tvar.getOffset();
            Term tval = varTable.getValue(tvarname);
            if (tval != null) {
              if (node.getType() != VARIABLE) {
                node = node.getParent().findChild(tval.getStartName(), tval.getStartType());
                if (node == null)
                  break MID_LOOP;
              }
              if (cur + 1 != end)
                termCont = new TermCont(term, cur + 1, end, termCont);
              term = tval;
              cur  = term.getStart();
              end  = term.getNext(cur);
              continue;
            }
            if (node.getType() != VARIABLE)
              break MID_LOOP;
            Term nvar = varTable.getTailVar(node.getName() + treeOffset);
            // Same variable name?
            if (tvar.getStartName() + tvar.getOffset() != nvar.getStartName() + nvar.getOffset())
              break MID_LOOP;
          }
          else {    // term.getType(cur) != VARIABLE
            if (node.getType() == VARIABLE) {
              // Find the next candidate.
              DiscNode<E> next = node.getRight();
              if (next != null && next.getType() != VARIABLE)
                next = node.getParent().findChild(term.getName(cur), term.getType(cur));
              if (next != null)
                candStack.push(new Cand<E>(
                    new TermCont(term, cur, end, termCont),
                    next, 0, null, varTable.state()));
              // Check to be equals.
              Term nvar = varTable.getTailVar(node.getName() + treeOffset);
              int nvarname = nvar.getStartName() + nvar.getOffset();
              Term nval = varTable.getValue(nvarname);
              if (nval == null)
                break MID_LOOP;
              if (!Term.equals(nval, nval.getStart(), term, cur))
                break MID_LOOP;
            }
          }
          // Go to the next symbol.
          if (node.getType() == VARIABLE)
            cur = term.getNext(cur);
          else
            cur++;
          if (cur == end)
            break;
          DiscNode<E> c = node.getFirstChild();
          if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
            c = node.findChild(term.getName(cur), term.getType(cur));
            if (c == null) break MID_LOOP;
          }
          node = c;
        }
        // Try the next unfinished term.
        if (termCont == null) {
          // Reached to the leaf.
          LightArrayList<E> leaves = node.getLeaves();
          assert(!leaves.isEmpty());
          varTable.backtrackTo(state);
          return leaves.get(0);
        }
        term = termCont.getTerm();
        cur  = termCont.getCur();
        end  = termCont.getEnd();
        termCont = termCont.getPrev();
        DiscNode<E> c = node.getFirstChild();
        if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
          c = node.findChild(term.getName(cur), term.getType(cur));
          if (c == null) break MID_LOOP;
        }
        node = c;
      }
      // Try to the next candidate.
      if (candStack.isEmpty()) {
        varTable.backtrackTo(state);
        return null;
      }
      Cand<E> cand = candStack.pop();
      termCont = cand.getTermCont();
      term     = termCont.getTerm();
      cur      = termCont.getCur();
      end      = termCont.getEnd();
      termCont = termCont.getPrev();
      node     = cand.getNode();
      varTable.backtrackTo(cand.getState());
    }
  }

  /**
   * Finds an unifiable literal with the specified literal and returns the associated objects.
   * @param lit the literal.
   * @return the associated objects.
   */
  public List<Pair<DiscNode<E>,E>> findSubsumed(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? posRoot : negRoot;
    Term     term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    List<Pair<DiscNode<E>,E>> out = new ArrayList<Pair<DiscNode<E>,E>>();
    if (node == null)
      return out;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    findSubsumed(term, node, out);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return out;
  }

  /**
   * Finds an unifiable literal with the complement of the specified literal and returns the associated objects.
   * @param lit the literal.
   * @return the associated objects.
   */
  public List<Pair<DiscNode<E>,E>> findCompSubsumed(Literal lit) {
    DiscNode<E> root = lit.isPositive() ? negRoot : posRoot;
    Term        term = lit.getTerm();
    DiscNode<E> node = root.findChild(term.getStartName(), term.getStartType());

    List<Pair<DiscNode<E>,E>> out = new ArrayList<Pair<DiscNode<E>,E>>();
    if (node == null)
      return out;
    if (newNameSpace) {
      treeOffset = varTable.getNumVars();
      varTable.addVars(numVars);
    }
    else
      treeOffset = 0;
    findSubsumed(term, node, out);
    if (newNameSpace) {
      varTable.removeVars(numVars);
    }
    return out;
  }

  private void findSubsumed(Term term, DiscNode<E> node, List<Pair<DiscNode<E>,E>> out) {
    int state = varTable.state();
    int cur   = term.getStart();
    int end   = term.getNext(cur);
    JumpLink<E>   link = null;
    int         linkNo = 0;
    TermCont  termCont = null;
    SubstList    glist = null;
    candStack.clear();
    while (true) {        // Loop for each candidate.
      MID_LOOP:
      while (true) {      // Loop for unfinished terms.
        while (true) {    // Loop for each term.
          if (term.getType(cur) == VARIABLE) {
            Term tval = varTable.getTailValue(term.getName(cur) + term.getOffset());
            if (tval != null) {
              if (node.getType() != VARIABLE) {
                node = node.getParent().findChild(tval.getStartName(), tval.getStartType());
                if (node == null)
                  break MID_LOOP;
              }
              if (cur + 1 != end)
                termCont = new TermCont(term, cur + 1, end, termCont);
              term = tval;
              cur  = term.getStart();
              end  = term.getNext(cur);
              continue;
            }
            // Find the next candidate.
            if (linkNo + 1 < node.getNumJumpLinks()) {
              candStack.push(new Cand<E>(
                  new TermCont(term, cur, end, termCont),
                  node, linkNo + 1, glist, varTable.state()));
            }
            else if (node.getRight() != null) {
              candStack.push(new Cand<E>(
                  new TermCont(term, cur, end, termCont),
                  node.getRight(), 0, glist, varTable.state()));
            }
            // Try to be subsumed.
            Term nterm = null;
            if (linkNo < node.getNumJumpLinks()) {
              link = node.getJumpLink(linkNo);
              nterm = (treeOffset == 0) ? link.getTerm() : Term.newOffset(link.getTerm(), treeOffset);
            }
            else if (node.getType() == VARIABLE) {
              //nterm = (treeOffset == 0) ? node.getTerm() : Term.newOffset(node.getTerm(), treeOffset);
              nterm = varTable.getTailVar(node.getName() + treeOffset);
            }
            else {
              nterm = node.getTerm();
            }
            Subst g = null;
            if ((g = Term.subsumes(term, cur, nterm, nterm.getStart(), nterm)) == null)
              break MID_LOOP;
            // Occur checking.
            assert(g.size() <= 1);
            if (g.size() == 1) {
              int varname = g.getVar(0);
              DiscNode<E> n = node.getParent();
              while (!n.isRoot()) {
                if (n.getType() == VARIABLE) {
                  Term nvar = varTable.getTailVar(n.getName() + treeOffset);
                  if (nvar.containsVar(varname) == -1)
                    break MID_LOOP;
                }
                n = n.getParent();
              }
            }
            if (!g.isEmpty())
              glist = new SubstList(g, glist);
          }
          else {    // term.getType(cur) != VARIABLE
            if (node.getType() == VARIABLE) {
              // Find the next candidate.
              DiscNode<E> next = node.getRight();
              if (next != null && next.getType() != VARIABLE)
                next = node.getParent().findChild(term.getName(cur), term.getType(cur));
              if (next != null)
                candStack.push(new Cand<E>(
                    new TermCont(term, cur, end, termCont),
                    next, 0, glist, varTable.state()));
              // Check to be subsumed.
              Term nvar = varTable.getTailVar(node.getName() + treeOffset);
              Subst g = null;
              if ((g = Term.subsumes(term, cur, nvar, nvar.getStart(), nvar)) == null)
                break MID_LOOP;
              // Occur checking.
              assert(g.size() <= 1);
              if (g.size() == 1) {
                int varname = g.getVar(0);
                DiscNode<E> n = node.getParent();
                while (!n.isRoot()) {
                  if (n.getType() == VARIABLE) {
                    Term v = varTable.getTailVar(n.getName() + treeOffset);
                    if (v.containsVar(varname) == -1)
                      break MID_LOOP;
                  }
                  n = n.getParent();
                }
              }
              if (!g.isEmpty())
                glist = new SubstList(g, glist);
            }
          }
          // Go to the next symbol.
          if (node.getType() == VARIABLE)
            cur = term.getNext(cur);
          else
            cur++;
          if (link != null) {
            node = link.getDest();
            link = null;
          }
          if (cur == end)
            break;
          DiscNode<E> c = node.getFirstChild();
          if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
            c = node.findChild(term.getName(cur), term.getType(cur));
            if (c == null) break MID_LOOP;
          }
          node = c;
          linkNo = 0;
        }
        // Try the next unfinished term.
        if (termCont == null) {
          // Reached to the leaf.
          LightArrayList<E> leaves = node.getLeaves();
          for (int i=0; i < leaves.size(); i++)
            out.add(new Pair<DiscNode<E>,E>(node, leaves.get(i)));
          break;
        }
        term = termCont.getTerm();
        cur  = termCont.getCur();
        end  = termCont.getEnd();
        termCont = termCont.getPrev();
        DiscNode<E> c = node.getFirstChild();
        if (c.getType() != VARIABLE && term.getType(cur) != VARIABLE) {
          c = node.findChild(term.getName(cur), term.getType(cur));
          if (c == null) break MID_LOOP;
        }
        node = c;
        linkNo = 0;
      }
      // Try to the next candidate.
      if (candStack.isEmpty()) {
        varTable.backtrackTo(state);
        return;
      }
      Cand<E> cand = candStack.pop();
      termCont = cand.getTermCont();
      term     = termCont.getTerm();
      cur      = termCont.getCur();
      end      = termCont.getEnd();
      termCont = termCont.getPrev();
      node     = cand.getNode();
      linkNo   = cand.getLinkNo();
      glist    = cand.getSubstPair();
      link     = null;
      varTable.backtrackTo(cand.getState());
    }
  }

  /**
   * Removes the set of clauses subsumed by the specified literal (backward subsumption checking).
   * @param lit the literal.
   * @return true if some clauses are removed.
   */
  public boolean removeSubsumed(Literal lit) {
    List<Pair<DiscNode<E>,E>> pairs = findSubsumed(lit);
    if (pairs.isEmpty())
      return false;
    for (Pair<DiscNode<E>,E> pair : pairs)
      remove(pair.get1st(), pair.get2nd());
    return true;
  }

  /**
   * Returns the number of clauses in this tree.
   * @return the number of clauses in this tree.
   */
  public int getNumClauses() {
    return numClauses;
  }

  /**
   * Returns the number of variables contained in this tree.
   * @return the number of variables contained in this tree.
   */
  public int getNumVars() {
    return numVars;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object.
   */
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("posRoot\n");
    str.append(toString(posRoot));
    str.append("\n");
    str.append("negRoot\n");
    str.append(toString(negRoot));
    return str.toString();
  }

  /**
   * Returns a string representation of this object.
   * @param root the root node.
   * @return a string representation of this object.
   */
  public String toString(DiscNode<E> root) {
    StringBuilder str = new StringBuilder();
    DiscNode<E> node = root.getFirstChild();
    while (node != null) {

      // Make indents in proportion to the number of ancestors.
      StringBuilder indent = new StringBuilder();
      DiscNode<E> n = node;
      while (n.getParent() != root) {
        n = n.getParent();
        if (n.getRight() != null)
          indent.insert(0, "| ");
        else
          indent.insert(0, "  ");
      }
      indent.insert(0, " ");

      // Print the node.
      str.append(indent);
      str.append("+ ");
      str.append(node.toString());

      // If the node has jump links, then print it.
      if (node.hasJumpLinks()) {
        str.append(" jmp:");
        str.append(node.getJumpLinks().toString());
      }
      str.append("\n");

      // If the node has leaves, then print it.
      if (node.hasLeaves()) {
        if (node.getRight() != null)
          str.append(indent + "| ");
        else
          str.append(indent + "  ");
        str.append(node.getLeaves().toString());
        str.append("\n");
      }

      node = node.getNext(root);
    }
    return str.toString();
  }

  /** The environment. */
  @SuppressWarnings("unused")
  private Env env = null;
  /** The tree to which all positive literals belong. */
  private DiscNode<E> posRoot = null;
  /** The tree to which all negative literals belong. */
  private DiscNode<E> negRoot = null;
  /** The number of variables in this tree. */
  private int numVars = 0;
  /** The variable offset. */
  private int treeOffset = 0;
  /** The number of clauses. */
  private int numClauses = 0;
  /** The variable table. */
  private VarTable varTable = null;
  /** Whether all variables in this tree belong to another name space or not. */
  private boolean newNameSpace = false;
  /** The stack for handling candidates. */
  private ArrayStack<Cand<E>> candStack = new ArrayStack<Cand<E>>();


  private final static class Cand<E> {
    public Cand(TermCont termCont, DiscNode<E> node, int linkNo, SubstList glist, int state) {
      this.termCont = termCont;
      this.node     = node;
      this.linkNo   = linkNo;
      this.glist    = glist;
      this.state    = state;
    }
    public TermCont    getTermCont()  { return termCont; }
    public DiscNode<E> getNode()      { return node;     }
    public int         getLinkNo()    { return linkNo;   }
    public SubstList   getSubstPair() { return glist;    }
    public int         getState()     { return state;    }
    // Instance fields.
    private TermCont    termCont = null;
    private DiscNode<E> node     = null;
    private int         linkNo   = 0;
    private SubstList   glist    = null;
    private int         state    = 0;
  }

  private final static class SubstList {
    public SubstList(Subst head, SubstList rest) {
      this.head = head;
      this.rest = rest;
    }
    public Subst convert() {
      Subst     gg   = new Subst();
      SubstList list = this;
      while (list != null) {
        Subst g = list.head;
        gg.add(g);
        list = list.rest;
      }
      return gg;
    }
    private Subst     head = null;
    private SubstList rest = null;
  }

public E containsOtherForm(Literal lit) {
	// TODO Auto-generated method stub
		  DiscNode<E> root = lit.isPositive() ? posRoot : negRoot;
		  Term        term = lit.getTerm();
		  DiscNode<E> nodeLit = root.findChild(term.getStartName(), term.getStartType());
		  if (nodeLit != null){
			  if (lit.isPositive()){
				  DiscNode<E> node = posRoot.getFirstChild();
				  if ( nodeLit.equals(node) ){
					  LightArrayList<E> leaves = node.getLeaves();
					  while (leaves == null){
						  node = node.getFirstChild();
						  leaves = node.getLeaves();
					  }
					  return leaves.get(0);
				  }
				  else 
					  return null;
			  }
			  else {
				  DiscNode<E> node = negRoot.getFirstChild();
				  if ( nodeLit.equals(node) ){
					  LightArrayList<E> leaves = node.getLeaves();
					  while (leaves == null){
						  node = node.getFirstChild();
						  leaves = node.getLeaves();
					  }
					  return leaves.get(0);
				  }
				  else 
					  return null;

			  }
		  }
		  else{
			  return null;
		  }
}
}
