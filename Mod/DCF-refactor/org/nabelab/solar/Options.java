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

import java.io.PrintStream;

import org.nabelab.solar.equality.PriorityMap;
import org.nabelab.solar.equality.WeightMap;
import org.nabelab.solar.pfield.PField;

/**
 * @author nabesima
 *
 */
public class Options implements OptionTypes, DebugTypes {

  /**
   * Constructs default options
   * @param env the environment.
   */
  public Options(Env env) {
    this.env = env;
    this.use = new boolean[NUM_OPTION_TYPES];
    for (int i=0; i < use.length; i++)
      use[i] = true;
    use[USE_UNIT_LEMMA_EXTENSION       ] = false;
    use[USE_IDENTICAL_FOLDING_DOWN     ] = false;
    use[USE_NEGATION_AS_FAILURE        ] = false;
    use[USE_NODE_INSTANTIATION         ] = false;
    use[USE_CONSTRAINT_INSTANTIATION   ] = false;
    use[USE_BRIDGE_FORMULA_TRANSLATION ] = false;
    use[USE_INC_CARC_COMP              ] = false;
    //use[USE_PURE_LITERAL_ELIMINATION   ] = false;
    use[USE_FREQ_COMMON_LITS_EXTRACTION] = false;
    use[USE_UNIQUE_NAME_AXIOMS         ] = false;
    use[USE_TEST1] = false;
    use[USE_TEST2] = false;
    use[USE_TEST3] = false;
    use[USE_TEST4] = false;
  }

  /**
   * Prints the help message.
   * @param detailed if true, then prints the detail help message.
   */
  public void printHelp(boolean detailed) {
    PrintStream out = System.out;
    out.println("Usage: solar [OPTION]... FILE");
    out.println("  -df [N]      Depth-First search with depth limit N");
    out.println("  -dfid [N]    DF iterative deepning search with depth limit N (default)");
    out.println("  -dfidr N     DFID with resource redistribution with depth limit N");
    out.println("  -carc        computs characteristic clauses (ignore top clauses)");
    out.println("  -newc [FILE] computs only new characteristic clauses");
    out.println("                FILE specifies characteristic clauses");
    //out.println("  -div         divides a problem into sub problems if possible");
    out.println("  -inc, -INC   consequence iterative lengthening (CIL) search");
    out.println("                -inc runs CIL at each iteration of DF(ID,IDR)");
    out.println("                -INC runs DF(ID,IDR) at each iteration of CIL");
    out.println("  -len N       limit max length of consequences to find (ex. 0, 3, inf)");
    out.println("  -num N       limit num of consequences to find");
    out.println("  -t N         set time limit in second (ex. 10, 10m, 10h)");
    out.println("  -verify      verify found consequences");
    out.println("  -proof       output proofs of found consequences");
    out.println("  -used        output used clauses for each found consequence");
    out.println("  -o FILENAME  output found consequences to the specified file");
    out.println("  -v, -vv      verbose and more verbose mode");
    out.println("  -h           print help");
    out.println("  --h          print more help");
    out.println();

    if (!detailed)
      System.exit(ExitStatus.OPTION_ERROR);

    out.println("  -i N         limit max number of inferences");
    out.println("  -me, -rme    set type of tableau calculus");
    out.println("                -me  model elimination based tableau calculus (default)");
    out.println("                -rme restart model elimination based tableau calculus");
    out.println("  -base DIR    use DIR as a base directory for include directives");
    out.println("  -csv         print inference information in csv format");
    out.println("  -all, -nall  use or unuse all pruning methods");
    out.println("  -ir, -nir    use or unuse identical reduction");
    out.println("  -or, -nor    use or unuse order preserving reduction");
    out.println("  -ua, -nua    use or unuse unit axiom matching");
    out.println("  -ulm, -nulm  use or unuse unit lemma matching");
    out.println("  -ule, -nule  use or unuse unit lemma extension (unuse)");
    out.println("  -sc, -nsc    use or unuse strong contraction");
    out.println("  -ic, -nic    use or unuse identical c-reduction");
    out.println("  -fd, -nfd    use or unuse identical folding down (unuse)");
    out.println("  -rg, -nrg    use or unuse regularity");
    out.println("  -cf, -ncf    use or unuse complement freeness");
    out.println("  -tf, -ntf    use or unuse tautology freeness");
    out.println("  -us, -nus    use or unuse unit subsumption checking");
    out.println("  -lf, -nlf    use or unuse local failure caching");
    out.println("  -sr, -nsr    use or unuse skip-regularity");
    out.println("  -sm, -nsm    use or unuse skip-minimality");
    out.println("  -naf, -nnaf  use or unuse negation-as-failure (unuse)");
    out.println("  -min, -nmin  use or unuse clause subsumption minimizing");
    out.println("  -pe, -npe    use or unuse pure literal elimination");
    out.println("  -fe, -nfe    use or unuse frequent common literals extraction");
    out.println("  -ni, -nni    use or unuse node instantiation (unuse)");
    out.println("  -ci, -nci    use or unuse constraint instantiation (unuse)");
    out.println("  -bf, -nbf    use or unuse bridge formula translation (unuse)");
    out.println("  -icc, -nicc  use or unuse incremental carc computation (unuse)");
    out.println("  -top TYPE    use the specified type as top clauses");
    out.println("                pos  the positive clauses are used for top clauses");
    out.println("                neg  the negative clauses are used for top clauses");
    out.println("  -eq TYPE [C] use the following type of modification method");
    out.println("                none  denotes no equality-handling (required equality axioms(rstfp))");
    out.println("                m     use M-modification only (required rst axioms)");
    //out.println("                snm   Neg-S + pseudo S-preds -> M-modification (required t axiom)");
    out.println("                smt    S -> M -> T-modification");
    out.println("                mst    M -> S -> T-modification");
    out.println("                snmt   Neg-S + pseudo S-preds -> M -> T-modification");
    out.println("                sgmt   Neg-S + general S-pred -> M-mod -> T-modification");
    out.println("                snmt2  Neg-S + pseudo S-preds -> Pos-M + NEF -> T-modification");
    out.println("                snmt2a Neg-S + pseudo S-preds -> Pos-M + NEF to all args -> T-modification");
    out.println("                msnt   M -> Neg-S + pseudo S-preds -> T-modification");
    out.println("                msgt   M -> Neg-S + general S-pred -> T-modification");
    out.println("                msnt2  Pos-M + NEF -> Neg-S + pseudo S-preds -> T-modification");
    //out.println("      nsmt    Neg-S + general S-pred -> M -> T-modification");
    //out.println("      snmtn   Neg-S + S-rules -> M -> Neg-T-mod + Pos-T-rules");
    //out.println("      snm     Neg-S + S-rules -> M -> Pos-T-rule");
    out.println("               if use the ordering constraints, the followings are available");
    out.println("                c    checks constraints partially ");
    out.println("                cc   checks constraints fully");
    out.println("                ccc  checks constraints in advance");
    out.println("  -tw TYPE     use the specified term weight function for reduction ordering");
    out.println("                uni uses the uniform term weight");
    out.println("                lex  uses lexicographic ordering of term names (default)");
    out.println("                occ  high frequency term has low weight");
    out.println("                rocc low frequency term has high weight");
    out.println("  -pr TYPE     use the specified term priority function for reduction ordering");
    out.println("                lex  uses lexicographic ordering of term names (default)");
    out.println("                occ  high frequency term has low priority");
    out.println("                rocc low frequency term has high priority");
    out.println("  -lit O1 [O2] set the literal ordering in tableau clauses (default 'ext sym')");
    out.println("                sym or rsym prefers few or many symbols");
    out.println("                ext or rext prefers few or many extendable clauses");
    out.println("                SYM or RSYM prefers few or many symbols (dynamic checking)");
    out.println("                EXT or REXT prefers few or many extendable clauses (dynamic checking)");
    out.println("                SYM/ext  prefers many symbols and few extendable clauses (dynamic checking for SYM)");
    out.println("                SYM/EXT  prefers many symbols and few extendable clauses (dynamic checking)");
    out.println("                org uses the original ordering");
    out.println("  -op O1 [O2]  set the operation ordering (default 'ext sym')");
    out.println("                sym/rsym prefers few symbols");
    out.println("                ext/rext prefers few extendable clauses (for extension operations)");
    out.println("                org uses the original ordering");
    out.println("  -Dxxx        print debug information");
    out.println("                a  applied operators");
    out.println("                A  all information");
    out.println("                c  constraints");
    out.println("                d  divide and conquer strategy");
    out.println("                D  divide and conquer strategy detail");
    out.println("                e  local failure");
    out.println("                E  local failure detail");
    out.println("                f  folding-up lemmas");
    out.println("                F  folding-up lemmas detail");
    out.println("                g  generated consequences");
    out.println("                G  generated consequences detail");
    out.println("                i  inference information");
    out.println("                l  unit lemmas");
    out.println("                L  unit lemmas detail");
    out.println("                m  modification method");
    out.println("                M  skip minimality");
    out.println("                O  literal ordering");
    out.println("                p  problem");
    out.println("                r  strong contraction");
    out.println("                R  reduction order");
    out.println("                s  inference steps");
    out.println("                S  symbol table");
    out.println("                t  tableaux");
    out.println("                T  solved tableaux");
    out.println("                u  unit axioms");
    out.println("                U  tautology free");
    out.println("                v  verbose mode");
    out.println("                V  feature vector indexing");
    out.println("  -Txx-yy      print debug information from the inference step xx to yy.");
    out.println("  -Ixx         print debug information every xx inferences.");

    System.exit(ExitStatus.OPTION_ERROR);
  }

  /**
   * Parses the command line arguments
   * @param args the command line arguments
   */
  public void parse(String[] args) throws IllegalArgumentException {
    for (int i = 0; i < args.length; i++) {
      String op     = args[i];
      String oparg1 = (i + 1 < args.length) ? args[i + 1] : null;
      String oparg2 = (i + 2 < args.length) ? args[i + 2] : null;
      String oparg3 = (i + 3 < args.length) ? args[i + 3] : null;
      if (op.equals("-df")) {
        strategy = Strategy.DF;
        if (oparg1 != null && oparg1.matches("[0-9]+")) {
          depthLimit = Integer.parseInt(oparg1);
          if (depthLimit == 0)
            throw new IllegalArgumentException("Depth must be one or more.");
          i++;
        }
      }
      else if (op.equals("-dfid")) {
        strategy = Strategy.DFID;
        if (oparg1 != null && oparg1.matches("[0-9]+")) {
          depthLimit = Integer.parseInt(oparg1);
          if (depthLimit == 0)
            throw new IllegalArgumentException("Depth must be one or more.");
          i++;
        }
      }
      else if (op.equals("-dfidr")) {
        strategy = Strategy.DFIDR;
        if (oparg1 != null && oparg1.matches("[0-9]+")) {
          depthLimit = Integer.parseInt(oparg1);
          if (depthLimit == 0)
            throw new IllegalArgumentException("Depth must be one or more.");
          i++;
        }
        else
          throw new IllegalArgumentException(
              "DFIDR requires the depth limit N.");
      }
      else if (op.equals("-me")) {
        calcType = CALC_ME;
      }
      else if (op.equals("-rme")) {
        calcType = CALC_RME;
      }
      else if (op.equals("-carc"))
        carc = true;
      else if (op.equals("-newc") || op.equals("-newcarc")) {
        newcarc = true;
        if (oparg1 != null && oparg1.charAt(0) != '-') {
          carcfile = oparg1;
          i++;
        }
      }
      else if (op.equals("-div")) {
    	  //modified here for proper argument processing
        divide = true;
        if (oparg1 != null && oparg1.matches("(-?[0-9]+([.][0-9]+)?)|all|half")) {
          if (oparg1.matches("-?[0-9]+([.][0-9]+)?")) {
            divDepth = Float.parseFloat(oparg1);
            if (divDepth <= 0.0 || 1.0 <= divDepth)
              divDepth = (int)divDepth;
          }
          else if (oparg1.equals("all"))
            divDepth = 0;
          else if (oparg1.equals("half"))
            divDepth = 0.5f;
          else
            throw new IllegalArgumentException("-div requires the depth limit N.");
          i++;
          if (oparg2 != null && oparg2.charAt(0) != '-') {
              if (oparg2.matches("[0-9]+"))
                divMaxSuccs = Integer.parseInt(oparg2);
              else
                throw new IllegalArgumentException("2nd arg of -div must be the maximum number of local successes to be stored.");
              i++;
              if (oparg3 != null && oparg3.charAt(0) != '-') {
                  if (oparg2.matches("[0-9]+([.][0-9]+)?"))
                    divCommonRatio = Float.parseFloat(oparg3);
                  else
                    throw new IllegalArgumentException("3rd arg of -div must be the common ratio to assign the maximum number of local success to each operator.");
                  i++;
                }
            }
        }
      }
      else if (op.equals("-inc"))
        lengthening = Strategy.CIL_SLAVE;
      else if (op.equals("-INC"))
        lengthening = Strategy.CIL_MASTER;
      else if (op.equals("-len") && oparg1 != null && oparg1.matches("[0-9]+")) {
        maxLenConseqs = Integer.parseInt(oparg1);
        i++;
      }
      else if (op.equals("-len") && oparg1 != null && oparg1.equals("inf")) {
        maxLenConseqs = PField.UNLIMITED;
        i++;
      }
      else if (op.equals("-num") && oparg1 != null && oparg1.matches("[0-9]+")) {
        maxNumConseqs = Integer.parseInt(oparg1);
        i++;
      }
      else if (op.equals("-t") && oparg1 != null) {
        setTimeLimit(oparg1);
        i++;
      }
      else if (op.equals("-i") && oparg1 != null) {
        maxNumInfs = Integer.parseInt(oparg1);
        i++;
      }
      else if (op.equals("-verify")) {
        setVerifyOp(true);
      }
      else if (op.equals("-proof")) {
        setProofOp(true);
      }
      else if (op.equals("-used")) {
        setUsedClausesOp(true);
      }
      else if (op.equals("-o") && oparg1 != null) {
        outputFile = oparg1;
        i++;
      }
      else if (op.equals("-v"))
        setVerboseLv(1);
      else if (op.equals("-vv"))
        setVerboseLv(2);
      else if (op.equals("-base")) {
        if (oparg1 != null && oparg1.charAt(0) != '-') {
          baseDir = oparg1;
          i++;
        }
        else
          throw new IllegalArgumentException(
            "-base requires the base directory specification.");
      }
      else if (op.equals("-csv")) {
        csv = true;
        env.setDebug(DBG_INFERENCE_INFO, true);
      }
      else if (op.equals("-all")) {
        for (int j=0; j < use.length; j++)
          use[j] = true;
      }
      else if (op.equals("-nall")) {
        for (int j=0; j < use.length; j++)
          use[j] = false;
      }
      else if (op.equals("-ir"))
        use[USE_IDENTICAL_REDUCTION] = true;
      else if (op.equals("-nir"))
        use[USE_IDENTICAL_REDUCTION] = false;
      else if (op.equals("-or"))
        use[USE_ORDER_PRESERVING_REDUCTION] = true;
      else if (op.equals("-nor"))
        use[USE_ORDER_PRESERVING_REDUCTION] = false;
      else if (op.equals("-ua"))
        use[USE_UNIT_AXIOM_MATCHING] = true;
      else if (op.equals("-nua"))
        use[USE_UNIT_AXIOM_MATCHING] = false;
      else if (op.equals("-ulm"))
        use[USE_UNIT_LEMMA_MATCHING] = true;
      else if (op.equals("-nulm"))
        use[USE_UNIT_LEMMA_MATCHING] = false;
      else if (op.equals("-ule"))
        use[USE_UNIT_LEMMA_EXTENSION] = true;
      else if (op.equals("-nule"))
        use[USE_UNIT_LEMMA_EXTENSION] = false;
      else if (op.equals("-ic"))
        use[USE_IDENTICAL_C_REDUCTION] = true;
      else if (op.equals("-nic"))
        use[USE_IDENTICAL_C_REDUCTION] = false;
      else if (op.equals("-fd"))
        use[USE_IDENTICAL_FOLDING_DOWN] = true;
      else if (op.equals("-nfd"))
        use[USE_IDENTICAL_FOLDING_DOWN] = false;
      else if (op.equals("-rg"))
        use[USE_REGULARITY] = true;
      else if (op.equals("-nrg"))
        use[USE_REGULARITY] = false;
      else if (op.equals("-cf"))
        use[USE_COMPLEMENT_FREE] = true;
      else if (op.equals("-ncf"))
        use[USE_COMPLEMENT_FREE] = false;
      else if (op.equals("-tf"))
        use[USE_TAUTOLOGY_FREE] = true;
      else if (op.equals("-ntf"))
        use[USE_TAUTOLOGY_FREE] = false;
      else if (op.equals("-us"))
        use[USE_UNIT_SUBSUMPTION] = true;
      else if (op.equals("-nus"))
        use[USE_UNIT_SUBSUMPTION] = false;
      else if (op.equals("-sc"))
        use[USE_STRONG_CONTRACTION] = true;
      else if (op.equals("-nsc"))
        use[USE_STRONG_CONTRACTION] = false;
      else if (op.equals("-lf"))
        use[USE_LOCAL_FAILURE_CACHE] = true;
      else if (op.equals("-nlf"))
        use[USE_LOCAL_FAILURE_CACHE] = false;
      else if (op.equals("-sr"))
        use[USE_SKIP_REGULARITY] = true;
      else if (op.equals("-nsr"))
        use[USE_SKIP_REGULARITY] = false;
      else if (op.equals("-sm"))
        use[USE_SKIP_MINIMALITY] = true;
      else if (op.equals("-nsm"))
        use[USE_SKIP_MINIMALITY] = false;
      else if (op.equals("-naf"))
        use[USE_NEGATION_AS_FAILURE] = true;
      else if (op.equals("-nnaf"))
        use[USE_NEGATION_AS_FAILURE] = false;
      else if (op.equals("-min"))
        use[USE_CLAUSE_SUBSUMP_MINIZING] = true;
      else if (op.equals("-nmin"))
        use[USE_CLAUSE_SUBSUMP_MINIZING] = false;
      else if (op.equals("-pe"))
        use[USE_PURE_LITERAL_ELIMINATION] = true;
      else if (op.equals("-npe"))
        use[USE_PURE_LITERAL_ELIMINATION] = false;
      else if (op.equals("-fe"))
        use[USE_FREQ_COMMON_LITS_EXTRACTION] = true;
      else if (op.equals("-nfe"))
        use[USE_FREQ_COMMON_LITS_EXTRACTION] = false;
      else if (op.equals("-ni"))
        use[USE_NODE_INSTANTIATION] = true;
      else if (op.equals("-nni"))
        use[USE_NODE_INSTANTIATION] = false;
      else if (op.equals("-ci"))
        use[USE_CONSTRAINT_INSTANTIATION] = true;
      else if (op.equals("-nci"))
        use[USE_CONSTRAINT_INSTANTIATION] = false;
      else if (op.equals("-bf"))
        use[USE_BRIDGE_FORMULA_TRANSLATION] = true;
      else if (op.equals("-nbf"))
        use[USE_BRIDGE_FORMULA_TRANSLATION] = false;
      else if (op.equals("-icc"))
        use[USE_INC_CARC_COMP] = true;
      else if (op.equals("-nicc"))
        use[USE_INC_CARC_COMP] = false;
      else if (op.equals("-top")) {
        if (oparg1 != null) {
          if (oparg1.equals("pos"))
            useNegTopClauses = false;
          else if (oparg1.equals("neg"))
            useNegTopClauses = true;
          else
            throw new IllegalArgumentException(
                "Invalid option '" + oparg1 + "' for -top.");
          i++;
        }
        else
          throw new IllegalArgumentException(
            "-top requires the type of top clauses.");
      }
      else if (op.equals("-eq")) {
        if (oparg1 != null) {
          if (oparg1.equals("none"))
            eqType = CFP.EQ_AXIOMS_REQUIRED;
          else if (oparg1.equals("m"))
            eqType = CFP.EQ_M;
          else if (oparg1.equals("smt"))
            eqType = CFP.EQ_SMT;
          else if (oparg1.equals("mst"))
            eqType = CFP.EQ_MST;
          else if (oparg1.equals("snmt"))
            eqType = CFP.EQ_SNMT;
          else if (oparg1.equals("sgmt"))
            eqType = CFP.EQ_SGMT;
          else if (oparg1.equals("snmt2"))
            eqType = CFP.EQ_SNMT2;
          else if (oparg1.equals("snmt2a"))
            eqType = CFP.EQ_SNMT2A;
          else if (oparg1.equals("msnt"))
            eqType = CFP.EQ_MSNT;
          else if (oparg1.equals("msgt"))
            eqType = CFP.EQ_MSGT;
          else if (oparg1.equals("msnt2"))
            eqType = CFP.EQ_MSNT2;
          else if (oparg1.equals("nsmt"))
            eqType = CFP.EQ_NSMT;
          else if (oparg1.equals("snmtn"))
            eqType = CFP.EQ_SNMTN;
          else if (oparg1.equals("snm"))
            eqType = CFP.EQ_SNM;
          else
            throw new IllegalArgumentException(
                "Invalid option '" + oparg1 + "' for -eq.");
          i++;
        }
        else
          throw new IllegalArgumentException(
            "-eq requires the equality axioms that should be removed.");

        if (oparg2 != null) { //  && eqType != CFP.EQ_AXIOMS_REQUIRED && eqType != CFP.EQ_M) {
          if (oparg2.equals("c"))
            eqConstraintType = CFP.EQ_CONSTRAINTS_PART;
          else if (oparg2.equals("cc"))
            eqConstraintType = CFP.EQ_CONSTRAINTS_FULL;
          else if (oparg2.equals("ccc"))
            eqConstraintType = CFP.EQ_CONSTRAINTS_ADVANCE;

          if (eqConstraintType != CFP.EQ_CONSTRAINTS_NONE)
            i++;
        }
      }
      else if (op.equals("-tw")) {
        if (oparg1 != null) {
          if (oparg1.equals("uni"))
            termWightFunc = WeightMap.UNIFORM_FUNC;
          else if (oparg1.equals("lex"))
            termWightFunc = WeightMap.LEX_ORDER_FUNC;
          else if (oparg1.equals("occ"))
            termWightFunc = WeightMap.OCC_ORDER_FUNC;
          else if (oparg1.equals("rocc"))
            termWightFunc = WeightMap.ROCC_ORDER_FUNC;
          else
            throw new IllegalArgumentException("Invalid option '" + oparg1 + "' for -rw.");
          i++;
        }
        else
          throw new IllegalArgumentException("-rw requires the weight function specification.");
      }
      else if (op.equals("-pr")) {
        if (oparg1 != null) {
          if (oparg1.equals("lex"))
            termPriorityFunc = PriorityMap.LEX_ORDER_FUNC;
          else if (oparg1.equals("occ"))
            termPriorityFunc = PriorityMap.OCC_ORDER_FUNC;
          else if (oparg1.equals("rocc"))
            termPriorityFunc = PriorityMap.ROCC_ORDER_FUNC;
          else
            throw new IllegalArgumentException("Invalid option '" + oparg1 + "' for -rp.");
          i++;
        }
        else
          throw new IllegalArgumentException("-rw requires the priority function specification.");
      }
      else if (op.equals("-lit")) {
        if (oparg1 != null) {
          if (oparg1.equals("sym"))
            firstLitOrder = LitOrder.FIX_FEW_SYMS;
          else if (oparg1.equals("SYM"))
            firstLitOrder = LitOrder.DYN_FEW_SYMS;
          else if (oparg1.equals("ext"))
            firstLitOrder = LitOrder.FIX_FEW_EXTS;
          else if (oparg1.equals("EXT"))
            firstLitOrder = LitOrder.DYN_FEW_EXTS;
          else if (oparg1.equals("rsym"))
            firstLitOrder = LitOrder.FIX_MANY_SYMS;
          else if (oparg1.equals("RSYM"))
            firstLitOrder = LitOrder.DYN_MANY_SYMS;
          else if (oparg1.equals("rext"))
            firstLitOrder = LitOrder.FIX_MANY_EXTS;
          else if (oparg1.equals("REXT"))
            firstLitOrder = LitOrder.DYN_MANY_EXTS;
          else if (oparg1.equals("SYM/ext"))
            firstLitOrder = LitOrder.DYN_SYMS_PER_EXTS;
          else if (oparg1.equals("SYM/EXT"))
            firstLitOrder = LitOrder.DYN_SYMS_PER_DYN_EXTS;
          else if (oparg1.equals("org"))
            firstLitOrder = LitOrder.ORG_ORDER;
          else
            throw new IllegalArgumentException("Invalid option '" + oparg1 + "' for -lit.");
          i++;
          secondLitOrder = LitOrder.ORG_ORDER;
        }
        else
          throw new IllegalArgumentException("-lit requires the literal ordering specification.");

        if (oparg2 != null && firstLitOrder != LitOrder.DYN_SYMS_PER_EXTS && firstLitOrder != LitOrder.DYN_SYMS_PER_DYN_EXTS) {
          if (oparg2.equals("sym"))
            secondLitOrder = LitOrder.FIX_FEW_SYMS;
          else if (oparg2.equals("SYM"))
            secondLitOrder = LitOrder.DYN_FEW_SYMS;
          else if (oparg2.equals("ext"))
            secondLitOrder = LitOrder.FIX_FEW_EXTS;
          else if (oparg2.equals("EXT"))
            secondLitOrder = LitOrder.DYN_FEW_EXTS;
          else if (oparg2.equals("rsym"))
            secondLitOrder = LitOrder.FIX_MANY_SYMS;
          else if (oparg2.equals("RSYM"))
            secondLitOrder = LitOrder.DYN_MANY_SYMS;
          else if (oparg2.equals("rext"))
            secondLitOrder = LitOrder.FIX_MANY_EXTS;
          else if (oparg2.equals("REXT"))
            secondLitOrder = LitOrder.DYN_MANY_EXTS;
          else if (oparg2.equals("org"))
            secondLitOrder = LitOrder.ORG_ORDER;
          else
            continue;

          if (firstLitOrder == LitOrder.ORG_ORDER && secondLitOrder != LitOrder.ORG_ORDER)
            throw new IllegalArgumentException("Cannot specify the second literal order if the first is org");

          i++;
        }
      }
      else if (op.equals("-op")) {
        if (oparg1 != null) {
          if (oparg1.equals("sym"))
            firstOpOrder = OpOrder.FEW_SYMS;
          else if (oparg1.equals("rsym"))
            firstOpOrder = OpOrder.MANY_SYMS;
          else if (oparg1.equals("ext"))
            firstOpOrder = OpOrder.FEW_EXTS;
          else if (oparg1.equals("rext"))
            firstOpOrder = OpOrder.MANY_EXTS;
          else if (oparg1.equals("org"))
            firstOpOrder = OpOrder.ORG_ORDER;
          else
            throw new IllegalArgumentException("Invalid option '" + oparg1 + "' for -op.");
          i++;
          secondOpOrder = OpOrder.ORG_ORDER;
        }
        else
          throw new IllegalArgumentException("-op requires the operator ordering specification.");

        if (oparg2 != null) {
          if (oparg2.equals("sym"))
            secondOpOrder = OpOrder.FEW_SYMS;
          else if (oparg2.equals("rsym"))
            secondOpOrder = OpOrder.MANY_SYMS;
          else if (oparg2.equals("ext"))
            secondOpOrder = OpOrder.FEW_EXTS;
          else if (oparg2.equals("rext"))
            secondOpOrder = OpOrder.MANY_EXTS;
          else if (oparg2.equals("org"))
            secondOpOrder = OpOrder.ORG_ORDER;
          else
            continue;

          if (firstOpOrder == OpOrder.ORG_ORDER && secondOpOrder != OpOrder.ORG_ORDER)
            throw new IllegalArgumentException("Cannot specify the second operator order if the first is org");

          i++;
        }
      }
      else if (op.startsWith("-D")) {
        for (int j=2; j < op.length(); j++)
          env.setDebug(op.charAt(j), true);
      }
      else if (op.startsWith("-T")) {
        env.setDbgPeriod(op.substring(2));
      }
      else if (op.startsWith("-I")) {
        env.setDbgInterval(op.substring(2));
      }
      else if (op.equals("-una")) {
    	  use[USE_UNIQUE_NAME_AXIOMS] = true;
        if (oparg1 != null && oparg1.matches("[0-9]+")) {
        	unaTermDepth = Integer.parseInt(oparg1);
        	i++;
        }
      }
      else if (op.equals("-test1"))   // Hidden option for testing.
        use[USE_TEST1] = true;
      else if (op.equals("-ntest1"))  // Hidden option for testing.
        use[USE_TEST1] = false;
      else if (op.equals("-test2"))   // Hidden option for testing.
        use[USE_TEST2] = true;
      else if (op.equals("-ntest2"))  // Hidden option for testing.
        use[USE_TEST2] = false;
      else if (op.equals("-test3"))   // Hidden option for testing.
        use[USE_TEST3] = true;
      else if (op.equals("-ntest3"))  // Hidden option for testing.
        use[USE_TEST3] = false;
      else if (op.equals("-test4"))   // Hidden option for testing.
        use[USE_TEST4] = true;
      else if (op.equals("-ntest4"))  // Hidden option for testing.
        use[USE_TEST4] = false;
      else if (op.equals("-h") || op.equals("-help"))
        printHelp(false);
      else if (op.equals("--h") || op.equals("--help"))
        printHelp(true);
      else if (op.startsWith("-"))
        throw new IllegalArgumentException(
            "Unknown command line argument '" + op + "'");
      else {
        if (problemFile != null)
          throw new IllegalArgumentException(
              "Cannot accept multiple problem files '" + op + "'");
        problemFile = op;
      }
    }

    // Checks the semantics of options
    if (problemFile == null)
      printHelp(false);
    if (strategy == Strategy.DFIDR && maxNumConseqs == 0 && timeLimit == 0)
      throw new IllegalArgumentException(
          "DFIDR requires `-num N' and/or 1-t N' options.");
    if (carc && newcarc)
      throw new IllegalArgumentException(
      "Can not use -carc and -newc at once.");
    if (divide && (carc || newcarc))
      throw new IllegalArgumentException(
      "Can not use -divide and `-carc or -newc' at once.");
//    if (divide && outputFile == null)
//      throw new IllegalArgumentException(
//      "-divide requires `-o FILE' options.");
  }

  /**
   * Sets the problem file name.
   * @param problemFile the problemFile to set
   */
  public void setProblemFile(String problemFile) {
    this.problemFile = problemFile;
  }

  /**
   * Returns the problem file name.
   * @return the output file name.
   */
  public String getProblemFile() {
    return problemFile;
  }

  /**
   * Returns the type of search strategy.
   * @return the type of search strategy.
   */
  public int getStrategy() {
    return strategy;
  }

  /**
   * Sets the type of search strategy.
   * @param strategy the type of search strategy to set.
   */
  public void setStrategy(int strategy) {
    this.strategy = strategy;
  }

  /**
   * Returns the type of tableau calculus.
   * @return the type of tableau calculus.
   */
  public int getCalcType() {
    return calcType;
  }

  /**
   * Sets the type of tableau calculus.
   * @param type the type of tableau calculus to set.
   */
  public void setCalcType(int type) {
    this.calcType = type;
  }

  /**
   * Return true if the negative clauses are used as the top clauses.
   * @return true if the negative clauses are used as the top clauses.
   */
  public boolean useNegTopClauses() {
    return useNegTopClauses;
  }

  /**
   * Returns the depth limit of a tableau.
   * @return the depth limit of a tableau.
   */
  public int getDepthLimit() {
    return depthLimit;
  }

  /**
   * Sets the depth limit of a tableau.
   * @param depth the depth limit to set
   */
  public void setDepthLimit(int depth) {
    this.depthLimit = depth;
  }

  /**
   * Returns true if computes only new characteristic clauses.
   * @return true if computes only new characteristic clauses.
   */
  public boolean carc() {
    return carc;
  }

  /**
   * Returns true if computes only new characteristic clauses.
   * @return true if computes only new characteristic clauses.
   */
  public boolean newcarc() {
    return newcarc;
  }

  /**
   * Returns the characteristic clauses file.
   * @return the characteristic clauses file.
   */
  public String getCarcFile() {
    return carcfile;
  }

  /**
   * Returns true if divides a consequence finding problem into sub problems.
   * @return true if divides a consequence finding problem into sub problems.
   */
  public boolean divide() {
    return divide;
  }

  /**
   * Returns the maximum division depth.
   * @return the maximum division depth (0 means unlimited).
   */
  public float getMaxDivDepth() {
    return divDepth;
  }

  /**
   * Returns the maximum number of local successes to be stored.
   * @return the maximum number of local successes to be stored.
   */
  public int getMaxSuccs() {
    return divMaxSuccs;
  }

  /**
   * Returns the common ratio to assign the maximum number of local successes to each operator.
   * @return the common ratio to assign the maximum number of local successes to each operator.
   */
  public float getDivCommonRatio() {
    return divCommonRatio;
  }

  /**
   * Returns the type of consequence iterative lengthening (CIL) strategy.
   * @return the type of CIL strategy.
   */
  public int getLengthening() {
    return lengthening;
  }

  /**
   * @param lengthening the lengthening to set
   */
  public void setLengthening(int lengthening) {
    this.lengthening = lengthening;
  }

  /**
   * Returns the maximum length of consequences to find.
   * @return the maximum length of consequences to find.
   */
  public int getMaxLenConseqs() {
    return maxLenConseqs;
  }

  /**
   * Sets the maximum length of consequences to find.
   * @param maxNumConseqs the maximum length of consequences to find.
   */
  public void setMaxLenConseqs(int len) {
    maxLenConseqs = len;
  }

  /**
   * Returns the maximum number of consequences to find.
   * @return the maximum number of consequences to find.
   */
  public int getMaxNumConseqs() {
    return maxNumConseqs;
  }

  /**
   * Sets the maximum number of consequences to find.
   * @param maxNumConseqs the maximum number of consequences to find.
   */
  public void setMaxNumConseqs(int num) {
    maxNumConseqs = num;
  }

  /**
   * Returns the time limit in milliseconds
   * @return the time limit in milliseconds
   */
  public long getTimeLimit() {
    return timeLimit;
  }

  /**
   * Sets the time limit in the string format (ex. "3600", "60m", "1h" means one hour.
   * @param time the time limit in the string format.
   */
  public void setTimeLimit(String time) {
    time = time.trim();
    int milli = 1000;  // msec

    if (time.endsWith("s")) {
      time = time.substring(0, time.length() - 1);
    }
    else if (time.endsWith("m")) {
      milli *= 60;
      time = time.substring(0, time.length() - 1);
    }
    else if (time.endsWith("h")) {
      milli *= 60 * 60;
      time = time.substring(0, time.length() - 1);
    }

    try {
      timeLimit = Integer.parseInt(time) * milli;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid time format '" + time + "'");
    }
  }

  /**
   * Sets the time limit in milliseconds.
   * @param time the time limit in milliseconds.
   */
  public void setTimeLimit(long time) {
    timeLimit = time;
  }

  /**
   * Returns the maximum number of inferences.
   * @return the maximum number of inferences.
   */
  public int getMaxNumInfs() {
    return maxNumInfs;
  }

  /**
   * Sets the maximum number of inferences.
   * @param maxNumConseqs the maximum number of inferences.
   */
  public void setMaxNumInfs(int num) {
    maxNumInfs = num;
  }

  /**
   * Returns true if verifies found consequences.
   * @return true if verifies found consequences.
   */
  public boolean hasVerifyOp() {
    return verify;
  }

  /**
   * Sets whether verifies found consequences or not.
   * @param on true if verifies found consequences.
   */
  public void setVerifyOp(boolean on) {
    verify = on;
  }

  /**
   * Returns true if outputs proofs of found consequences.
   * @return true if outputs proofs of found consequences.
   */
  public boolean hasProofOp() {
    return proof;
  }

  /**
   * Sets whether outputs proofs of found consequences or not.
   * @param on true if outputs proofs of found consequences.
   */
  public void setProofOp(boolean on) {
    proof = on;
    if (proof)
      setVerifyOp(true);
  }

  /**
   * Returns true if outputs used clauses for each found consequence.
   * @return true if outputs used clauses for each found consequence.
   */
  public boolean hasUsedClausesOp() {
    return usedClauses;
  }

  /**
   * Sets whether outputs used clauses for each found consequence or not.
   * @param on true if outputs used clauses for each found consequence.
   */
  public void setUsedClausesOp(boolean on) {
    usedClauses = on;
    if (usedClauses)
      setVerifyOp(true);
  }

  /**
   * Returns the problem file name.
   * @return the problem file name.
   */
  public String getOutputFile() {
    return outputFile;
  }

  /**
   * Sets the output file name.
   * @param outputFile the output file name to set
   */
  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }

  /**
   * Returns the base directory for include directives.
   * @return the base directory.
   */
  public String getBaseDir() {
    return baseDir;
  }

  /**
   * Sets the base directory for include directives.
   * @param baseDir the base directory to set.
   */
  public void setBaseDir(String baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * Returns true if outputs inference information in the csv format.
   * @return true if outputs inference information in the csv format.
   */
  public boolean csv() {
    return csv;
  }

  /**
   * Returns true if the specific option is on.
   * @param type the specific pruning option.
   * @return true if the specific option is on.
   */
  public boolean use(int type) {
    return use[type];
  }

  /**
   * Sets the specified option.
   * @param type the specified option.
   * @param on   true if the option is on.
   */
  public void set(int type, boolean on) {
    use[type] = on;
  }

  /**
   * Returns the type of equality-handling.
   * @return the type of equality-handling.
   */
  public int getEqType() {
    return eqType;
  }

  /**
   * Sets the type of equality-handling.
   * @param type the type of equality-handling.
   */
  public void setEqType(int type) {
    eqType = type;
  }

  /**
   * Returns the type of equality-constraints.
   * @return the type of equality-constraints.
   */
  public int getEqConstraintType() {
    return eqConstraintType;
  }

  /**
   * Sets the type of equality-constraints.
   * @param type  the type of equality-constraints.
   */
  public void setEqConstraintType(int type) {
    eqConstraintType = type;
  }

  /**
   * Returns true if uses equality constraints.
   * @return true if uses equality constraints.
   */
  public boolean useEqConstraint() {
    return eqConstraintType != CFP.EQ_CONSTRAINTS_NONE;
  }

  /**
   * Returns the term weight function.
   * @return the term weight function.
   */
  public int getTermWeightFunc() {
    return termWightFunc;
  }

  /**
   * Returns the term priority function.
   * @return the term priority function.
   */
  public int getTermPriorityFunc() {
    return termPriorityFunc;
  }

  /**
   * Returns the first literal ordering.
   * @return the first literal ordering.
   */
  public int get1stLitOrder() {
    return firstLitOrder;
  }

  /**
   * Sets the first literal ordering.
   * @param first the first literal ordering.
   */
  public void set1stLitOrder(int first) {
    this.firstLitOrder = first;
  }

  /**
   * Returns the second literal ordering.
   * @return the second literal ordering.
   */
  public int get2ndLitOrder() {
    return secondLitOrder;
  }

  /**
   * Sets the second literal ordering.
   * @param second the second literal ordering.
   */
  public void set2ndLitOrder(int second) {
    this.secondLitOrder = second;
  }

  /**
   * Returns the first operator ordering.
   * @return the first operator ordering.
   */
  public int get1stOpOrder() {
    return firstOpOrder;
  }

  /**
   * Sets the first operator ordering.
   * @param first the first operator ordering.
   */
  public void set1stOpOrder(int first) {
    this.firstOpOrder = first;
  }

  /**
   * Returns the second operator ordering.
   * @return the second operator ordering.
   */
  public int get2ndOpOrder() {
    return secondOpOrder;
  }

  /**
   * Sets the second operator ordering.
   * @param second the second operator ordering.
   */
  public void set2ndOpOrder(int second) {
    this.secondOpOrder = second;
  }

  /**
   * Returns the term depth of unique name axioms.
   * @return the term depth of unique name axioms.
   */
  public int getUNATermDepth() {
    return unaTermDepth;
  }

  /**
   * Returns the verbose level.
   * @return the verbose level {0, 1, 2}.
   */
  public int getVerboseLv() {
    return verboseLv;
  }

  /**
   * Sets the verbose level.
   * @param level the verbose level {0, 1, 2}.
   */
  public void setVerboseLv(int level) {
    verboseLv = level;
    switch (level) {
    case 2:
      env.setDebug(DBG_BRIDGE,          true);
      env.setDebug(DBG_CONSTRAINT,      true);
      env.setDebug(DBG_TABLEAUX,        true);
      env.setDebug(DBG_SOLVED_TABLEAUX, true);
    case 1:
      env.setDebug(DBG_PROBLEM,         true);
      env.setDebug(DBG_INFERENCE_INFO,  true);
      env.setDebug(DBG_VERBOSE,         true);
    }
  }

  /** The environment. */
  private Env env = null;
  /** The problem file name */
  private String problemFile = null;
  /** The type of tableau calculus. */
  private int calcType = CALC_ME;
  /** Type of search strategy. */
  private int strategy = Strategy.DFID;
  /** The depth limit of a tableau. */
  private int depthLimit = 0;
  /** Whether computes only characteristic clauses or not. */
  private boolean carc = false;
  /** Whether computes only new characteristic clauses or not. */
  private boolean newcarc = false;
  /** The characteristic clauses file. */
  private String carcfile = null;
  /** Whether divides a consequence finding problem into sub problems. */
  private boolean divide = false;
  /** The maximum depth limit for a divide-and-conquer strategy to be applied. */
  private float divDepth = 1;
  /** The maximum number of local successes to be stored. */
  private int divMaxSuccs = 0;
  /** The common ratio to assign the maximum number of local successes to each operator. */
  private float divCommonRatio = 1.0f;
  /** Whether uses consequence iterative lengthening search strategy or not. */
  private int lengthening = Strategy.CIL_NONE;
  /** The maximum length of consequences to find. */
  private int maxLenConseqs = PField.NOT_DEFINED;
  /** The maximum number of consequences to find. */
  private int maxNumConseqs = 0;
  /** The time limit in milliseconds */
  private long timeLimit = 0;
  /** The maximum number of inferences. */
  private int maxNumInfs = 0;
  /** Whether verifies found consequences. */
  private boolean verify = false;
  /** Whether outputs proofs of found consequences. */
  private boolean proof = false;
  /** Whether outputs used clauses for each found consequence. */
  private boolean usedClauses = false;
  /** The output file for found consequences. */
  private String outputFile = null;

  /** The base directory for include directives. */
  private String baseDir = null;
  /** Whether outputs inference information in the csv format or not. */
  private boolean csv = false;
  /** The use of pruning methods. */
  private boolean[] use = null;
  /** The type of top clauses. */
  private boolean useNegTopClauses = true;
  /** The type of equality handling. */
  private int eqType = CFP.EQ_AXIOMS_REQUIRED;
  /** The type of equality constraints. */
  private int eqConstraintType = CFP.EQ_CONSTRAINTS_NONE;
  /** The term weight function for reduction ordering. */
  private int termWightFunc = WeightMap.LEX_ORDER_FUNC;
  /** The term priority function for reduction ordering. */
  private int termPriorityFunc = PriorityMap.LEX_ORDER_FUNC;
  /** The first literal ordering in tableau clauses. */
  private int firstLitOrder = LitOrder.FIX_FEW_EXTS;
  /** The second literal ordering in tableau clauses. */
  private int secondLitOrder = LitOrder.FIX_FEW_SYMS;
  /** The first operator ordering. */
  private int firstOpOrder = OpOrder.FEW_EXTS;
  /** The second operator ordering. */
  private int secondOpOrder = OpOrder.FEW_SYMS;
  /** The term depth of unique name axioms. */
  private int unaTermDepth = 0;
  /** The verbose level. */
  private int verboseLv = 0;

}
