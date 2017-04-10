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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.nabelab.solar.operator.Operator;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.proof.Proof;

/**
 * SOLAR: A Consequence Finding System
 *  * @author nabesima
 */
public class SOLAR implements ExitStatus, OptionTypes, DebugTypes {

	/**
   * Constructs a SOLAR system solving the specified consequence finding problem.
   * @param env the environment.
   * @param cfp the consequence finding problem.
   */
  public SOLAR(Env env, CFP cfp) {
    this.env    = env;
    this.cfp    = cfp;
    this.opt    = cfp.getOptions();
    this.stats  = env.getStats();
    env.setTimeStep(stats.getInfCounter());
  }

  /**
   * Solves the consequence finding problem.
   * @param startTime the start CPU time in milliseconds of the solving process.
 * @throws Exception 
   */
  public void solve(long startTime) throws Exception {
    this.startTime = startTime;
    solve();
  }

  /**
   * Solves the consequence finding problem.
 * @throws Exception 
   */
  public void solve() throws Exception {
	  if(Thread.currentThread().isInterrupted())
			return;

    stats.setProds(Stats.ORG_CLAUSES,  cfp.getNumClauses());
    stats.setProds(Stats.ORG_LITERALS, cfp.getNumLiterals());

    long time = 0;

    StringWriter str = new StringWriter();
    PrintWriter  out = new PrintWriter(str);

    if (opt.use(USE_BRIDGE_FORMULA_TRANSLATION))
      cfp.convertToStablePField(out);

    cfp.initFVecMap();
    if (opt.use(USE_CLAUSE_SUBSUMP_MINIZING) || opt.use(USE_PURE_LITERAL_ELIMINATION)) {
      time = getCPUTime();
      if (env.dbg(DBG_VERBOSE)) System.out.print(", Minimizing ");
      if (opt.use(USE_CLAUSE_SUBSUMP_MINIZING))
        cfp.convertToSubsumpMinimal(out);
      if (opt.use(USE_PURE_LITERAL_ELIMINATION))
        cfp.applyPureLitElimination(out);
      if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");
    }

    if (opt.use(USE_FREQ_COMMON_LITS_EXTRACTION)) {
      cfp.applyFreqCommonLitsExtraction(out);
      cfp.initFVecMap();
    }

    boolean sorted = false;
    boolean addedEqRef = false;
    if(Thread.currentThread().isInterrupted())
		return;
    if (opt.getEqType() != CFP.EQ_AXIOMS_REQUIRED && cfp.useEquality()) {

      addedEqRef = cfp.addEqReflexivity();  // For checking the unit subsumption etc..

      time = getCPUTime();
      if (env.dbg(DBG_VERBOSE)) System.out.print(", Sorting ");

      cfp.initFVecMap();
      cfp.initClauseProperties(out);
      cfp.convertToSortedClauses(out);
      sorted = true;
      if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");

      time = getCPUTime();
      if (env.dbg(DBG_VERBOSE)) System.out.print(", Modifying ");
      cfp.convertToNoEqualityFormat(out);
      if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");

      cfp.initFVecMap();
      if (opt.use(USE_CLAUSE_SUBSUMP_MINIZING)) {
        time = getCPUTime();
        if (env.dbg(DBG_VERBOSE)) System.out.print(", Minimizing ");
        cfp.convertToSubsumpMinimal(out);
        if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");
      }

      if (opt.use(USE_FREQ_COMMON_LITS_EXTRACTION)) {
        cfp.applyFreqCommonLitsExtraction(out);
        cfp.initFVecMap();
      }
    }

    cfp.initClauseProperties(out);
    if (!sorted) {
      time = getCPUTime();
      if (env.dbg(DBG_VERBOSE)) System.out.print(", Sorting ");
      cfp.convertToSortedClauses(out);
      if (env.dbg(DBG_VERBOSE)) System.out.print((getCPUTime() - time) / 1000.0 + "s");
    }

    cfp.calcReductionOrder();
    cfp.initTautologyFreeness(out);
    cfp.initUnitSubsumptionChecking();

    cfp.initTopClauses();
    cfp.initSkipMinimality(out);
    cfp.initIncCarcComputation(out);

    if (addedEqRef)
      cfp.removeEqReflexivity();

    if (env.dbg(DBG_VERBOSE)) System.out.println(")");
    if (str.getBuffer().length() != 0)
      System.out.println(str);

    stats.setProds(Stats.CLAUSES,  cfp.getNumClauses());
    stats.setProds(Stats.LITERALS, cfp.getNumLiterals());

    if (env.dbg(DBG_PROBLEM)) System.out.println(cfp);

    if (env.dbg(DBG_SYMBOL_TABLE)) {
      System.out.println("[Symbol table]");
      System.out.println(env.getSymTable());
    }

    if (cfp.getStatus() == TRIVIALLY_SATISFIABLE)
      return;

    if(Thread.currentThread().isInterrupted())
		return;
    tableau = new Tableau(env, cfp, stats);

    Strategy strategy = cfp.getStrategy();
    SearchParam param = null;
	while ((param = strategy.getNextSearchParam(stats, getCPUTime(), tableau.getMaxNumSkipped(), param)) != null)
		solve(param);

    if (env.dbg(DBG_VERBOSE))
      System.out.println();

    env.getVarTable().backtrackTo(0);
    env.getVarTable().removeAllVars();
  }

  /**
   * Solves the consequence finding problem with the specified search parameter.
   * @param param  the search parameter.
   * @return true when normal exit, false when restart since the axiom set is changed.
 * @throws ParseException 
   */
  private boolean solve(SearchParam param) throws ParseException {
	  if(Thread.currentThread().isInterrupted())
			return false;

    stats.inc(Stats.STAGE);
    stats.setDepth(param.getDepthLimit());

    if (env.dbg(DBG_VERBOSE)) {
      if (stats.get(Stats.STAGE) != 1 && env.dbg(DBG_TABLEAUX))
        System.out.println();
      System.out.printf("Stage %d  (%s inf:%d)\n",
          stats.get(Stats.STAGE), param.toString(), stats.inf());
    }

    tableau.reset();
    tableau.setSearchParam(param);

    while (true) {
    	if(Thread.currentThread().isInterrupted())
			return false;

      // Running time checking (checks each 1024 steps because getCpuTime() is a little bit slow).
      if (stats.inf() % 1024 == 0) {
        if (param.getTimeLimit() != 0 && param.getTimeLimit() < getCPUTime()) {
          param.setExhaustiveness(false);
          break;
        }
      }
      if (param.getMaxNumInfs() != 0 && param.getMaxNumInfs() == stats.inf()) {
        param.setExhaustiveness(false);
        break;
      }

      // If some characteristic clauses are received from other threads, then adds them to CFP.
      //if (!carcQueue.isEmpty())
      	while (!carcQueue.isEmpty())
      		cfp.addCarc(carcQueue.poll());

      stats.incInf();

      Node subgoal = tableau.getNextSubgoal();

      if (env.dbgNow(DBG_TABLEAUX)) {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println(stats.inf());
        System.out.println(tableau);
      }
      else if (env.dbgNow(DBG_STEPS)) {
        System.out.println(stats.inf());
      }
      if (env.dbgNow(DBG_APPLIED_OPS)) {
        //System.out.println(stats.inf());
        //System.out.println(tableau.getAppOps().toSimpleString());
        System.out.println(tableau.getLastOperator());
      }

      if (subgoal == null)
        break;
      Operator op = subgoal.getNextOperator();
      if (op == null) {
        if (tableau.cancel() == false)
          break;
        if (opt.use(USE_NEGATION_AS_FAILURE) && !tableau.removeClosedNAFSubTableau())
          break;
        continue;
      }
      if (tableau.apply(op) == false)
        continue;

      if (opt.use(USE_NEGATION_AS_FAILURE) && !tableau.removeClosedNAFSubTableau())
        break;
      if (!tableau.removeRedundancy())
        break;
      if (opt.divide()) {
        if (cfp.hasEmptyConseq())
          break;
        if (param.getMaxNumConseqs() != 0 && cfp.getConseqSet().size() >= param.getMaxNumConseqs()) {
          param.setExhaustiveness(false);
          break;
        }
      }
      else if (tableau.getNumOpenNodes() == 0) {
        Conseq conseq = tableau.getConseq();

        if (opt.hasVerifyOp()) {
          Proof proof = tableau.getProof(conseq);
          conseq.setProof(proof);
        }
        if (env.dbg(DBG_SOLVED_TABLEAUX)) {
          System.out.println();
          System.out.println("SOLVED");
          System.out.println(stats.inf() + " " + tableau);
        }
        if (tableau.getPFChecker().belongs(conseq) && cfp.addConseq(conseq)) {
          stats.setProds(Stats.CONSEQUENCES, cfp.getConseqSet().size());
          stats.setProds(Stats.CONSEQ_LITS , cfp.getConseqSet().getNumLiterals());
          if (cfp.hasEmptyConseq()) {
            cfp.setStatus(UNSATISFIABLE);
            break;
          }
          if (cfp.getConseqSet().size() == param.getMaxNumConseqs()) {
            param.setExhaustiveness(false);
            break;
          }
          tableau.markAs(Tags.SOLVABLE);
        }
        tableau.cancel();
      }
    }
    return true;
  }

  /**
   * Returns the statistics information.
   * @return the statistics information.
   */
  public Stats getStats() {
    return stats;
  }

  /**
   * Prints the statistics information.
   * @param out the stream.
   * @return the statistics information.
   */
  public void printStats(PrintStream out) {
    printStats(out, getCPUTime());
  }

  /**
   * Prints the statistics information.
   * @param out the stream.
   * @param cpu the CPU time.
   * @return the statistics information.
   */
  public void printStats(PrintStream out, long cpu) {
    stats.print(out, cfp, cpu, opt.csv());
  }

  /**
   * Returns the CPU time in milliseconds.
   * @return the CPU time in milliseconds.
   */
  public long getCPUTime() {
    return (threadMxBean.getThreadCpuTime(Thread.currentThread().getId()) / 1000000) - startTime;
  }

  /**
   * Executes SOLAR system.
 * @throws Exception 
   */
  public void exec() throws Exception {
    exec(0);
  }

  /**
   * Executes SOLAR system.
   * @param startTime the start CPU time in milliseconds of the solving process.
 * @throws Exception 
   */
  private void exec(long startTime) throws Exception {
    isSolving = true;
  	Runtime runtime = Runtime.getRuntime();
    SHook shook = new SHook(this, cfp);
    runtime.addShutdownHook(shook);

    solve(startTime);                        // Solve the problem!

    runtime.removeShutdownHook(shook);

    if (opt.hasVerifyOp())                   // Verifies the found consequences.
      cfp.getConseqSet().validate();

    if (opt.use(USE_BRIDGE_FORMULA_TRANSLATION))
      cfp.convertConqsToOrgFmt();

    int status = cfp.getStatus();
    List<Conseq> conqs = cfp.getConseqSet().get();

    switch (status) {
    case UNSATISFIABLE:
      System.out.println("UNSATISFIABLE");
      if (opt.hasProofOp()) {
        System.out.println();
        System.out.println("PROOF:");
        System.out.println(conqs.get(0).getProof());
      }
      if (opt.hasUsedClausesOp()) {
        if (!opt.hasProofOp())
          System.out.println();
        System.out.println("USED CLAUSES:");
        for (Clause used : conqs.get(0).getUsedClauses())
          System.out.println(used);
      }

      break;
    case SATISFIABLE:
      System.out.println("SATISFIABLE");
      System.out.println();
      if (conqs.isEmpty())
        System.out.println("NO CONSEQUENCES");
      else {
        if (cfp.getProblemType() == CFP.CHARACTERISTIC)
          System.out.println(conqs.size() + " CHARACTERISTIC CLAUSES");
        else
          System.out.println(conqs.size() + " FOUND CONSEQUENCES");
        if (opt.getOutputFile() == null) {
          for (Conseq c : conqs) {
            System.out.println(c);
            if (opt.hasProofOp()) {
              System.out.println();
              System.out.println("PROOF:");
              System.out.println(c.getProof());
            }
            if (opt.hasUsedClausesOp()) {
              if (!opt.hasProofOp())
                System.out.println();
              System.out.println("USED CLAUSES:");
              for (Clause used : c.getUsedClauses())
                System.out.println(used);
              System.out.println();
            }
          }
        }
      }
      break;
    case TRIVIALLY_SATISFIABLE:
      System.out.println("TRIVIALLY SATISFIABLE");
      break;
    case UNKNOWN:
      if (conqs.isEmpty())
        System.out.println("NOT FOUND");
      else {
        if (cfp.getProblemType() == CFP.CHARACTERISTIC)
          System.out.println(conqs.size() + " CHARACTERISTIC CLAUSES");
        else
          System.out.println(conqs.size() + " FOUND CONSEQUENCES");
        if (opt.getOutputFile() == null) {
          for (Conseq c : conqs) {
            System.out.println(c);
            if (opt.hasProofOp()) {
              System.out.println();
              System.out.println("PROOF:");
              System.out.println(c.getProof());
            }
            if (opt.hasUsedClausesOp()) {
              if (!opt.hasProofOp())
                System.out.println();
              System.out.println("USED CLAUSES:");
              for (Clause used : c.getUsedClauses())
                System.out.println(used);
              System.out.println();
            }
          }
        }
      }
      break;
    }

    if (env.dbg(DBG_INFERENCE_INFO)) {
      System.out.println();
      printStats(System.out);
      System.out.println();
    }

    if (env.getSOLARListers() != null)
    	for (SOLARListener listener : env.getSOLARListers())
    		listener.solarFinished(new SOLAREvent(this, SOLAREvent.FINISHED));

    isSolving = false;
    if (!carcQueue.isEmpty())
    	synchronized (cfp) {
    		while (!carcQueue.isEmpty())
    			cfp.addCarc(carcQueue.poll());
    	}
  }

  /**
   * Add characteristic clauses to SOLAR.
   * @param clauses  characteristic clauses.
   */
	public void addCarcs(List<Clause> clauses) {
		if (isSolving) {
			for (Clause c : clauses) {
				c = new Clause(env, c);   // clone the clause with the new environment which has a independent variable table.
				carcQueue.add(c);
			}
		}
		else
			synchronized (cfp) {
				for (Clause c : clauses) {
				c = new Clause(env, c);   // clone the clause with the new environment which has a independent variable table.
				cfp.addCarc(c);
				}
			}
	}

  /**
   * The main method of SOLAR
   * @param args command line arguments
   */
  public static void main(String[] args) {

    try {
      System.out.println(VERSION);

      Env env = new Env();
      Options opt = new Options(env);             // Create the default options.
      opt.parse(args);                            // Analyze the command line arguments.

      if (env.dbg(DBG_VERBOSE)) {
        System.out.print("Command: " + SOLAR.class.getName());
        for (String arg : args)
          System.out.print(" " + arg);
        System.out.println();
      }

      File file = new File(opt.getProblemFile());

      if (opt.newcarc() && opt.getCarcFile() == null && opt.getTimeLimit() != 0)
        opt.setTimeLimit(opt.getTimeLimit() / 2);

      // Computes characteristic clauses.
      ConseqSet carcSet = null;
      if (opt.newcarc() && opt.getCarcFile() == null) {
        long start = time();
        if (env.dbg(DBG_VERBOSE))
          System.out.print("(Parsing ");
        CFP carcfp = new CFP(env, opt);
        carcfp.parse(file, opt.getBaseDir());
        carcfp.removeTopClauses();
        if (env.dbg(DBG_VERBOSE))
          System.out.print((time() - start) / 1000.0 + "s");
        SOLAR solar = new SOLAR(env, carcfp);
        solar.exec();
        env.initStats();										//Reset environment stats
        carcSet = carcfp.getConseqSet();
      }

      long start = time();
      if (env.dbg(DBG_VERBOSE))
        System.out.print("(Parsing ");
      CFP cfp = new CFP(env, opt);
      cfp.parse(file, opt.getBaseDir());          // Load the consequence finding problem.
      cfp.setCarcSet(carcSet);
      if (env.dbg(DBG_VERBOSE))
        System.out.print((time() - start) / 1000.0 + "s");

      if (opt.carc())
        cfp.removeTopClauses();

      // Comment out to develop the recursive divide and conquer strategy.
      // if (!(opt.divide() && cfp.hasSingleDivisibleTopClause())) {
      if (true) {

        SOLAR solar = new SOLAR(env, cfp);          // Create a SOLAR system.

        solar.exec(solar.getCPUTime());

        ConseqSet prods = cfp.getConseqSet();

//        if (opt.newcarc() && opt.getCarcFile() == null) {
//          System.out.println();
//          cfp = new CFP(env, opt);
//          cfp.parse(file, opt.getBaseDir());
//          cfp.removeTopClauses();
//          solar = new SOLAR(env, cfp);
//          solar.exec(solar.getCPUTime());
//          ConseqSet carc = cfp.getConseqSet();
//
//          ConseqSet newcarc = new ConseqSet(env);
//          for (Conseq conseq : prods) {
//            conseq.getFVec(true);    // updates feature vector.
//            newcarc.add(conseq);
//          }
//          for (Conseq conseq : carc) {    // backward subsumption checking.
//            newcarc.remove(conseq);
//          }
//          prods = newcarc;
//          System.out.println();
//          if (newcarc.isEmpty())
//            System.out.println("NO NEW CHARACTERISTIC CLAUSES");
//          else {
//            System.out.println(newcarc.size() + " NEW CHARACTERISTIC CLAUSES");
//            if (opt.getOutputFile() == null)
//              for (Conseq c : newcarc)
//                System.out.println(c);
//          }
//
//          if (env.dbg(DBG_INFERENCE_INFO)) {
//            System.out.println();
//            System.out.printf("Total CPU time: %.2fs\n", time() / 1000.0);
//            System.out.println();
//          }
//        }

        if (opt.newcarc() && opt.getCarcFile() == null) {
          if (prods.isEmpty())
            System.out.println("NO NEW CHARACTERISTIC CLAUSES");
          else {
            System.out.println(prods.size() + " NEW CHARACTERISTIC CLAUSES");
            if (opt.getOutputFile() == null)
              for (Conseq c : prods)
                System.out.println(c);
          }
          if (env.dbg(DBG_INFERENCE_INFO)) {
            System.out.println();
            System.out.printf("Total CPU time: %.2fs\n", time() / 1000.0);
            System.out.println();
          }
        }

        if (opt.getOutputFile() != null)
          prods.output(opt.getOutputFile());

        System.exit(cfp.getStatus());
      }
      else {
        // Divides the single ground top clause into unit top clauses and solves separately.
        Clause   topClause = cfp.getTopClause(0);
        List<Clause> units = topClause.divideIntoUnitClauses();
        String  outputFile = opt.getOutputFile();

        int no = 1;
        for (Clause top : units) {
          cfp.removeTopClauses();
          cfp.addClause(top);

          SOLAR solar = new SOLAR(env, cfp);          // Create a SOLAR system.

          solar.exec();

          ConseqSet prod = cfp.getConseqSet();
          prod.output(String.format("%s.%03d", outputFile, no++));
          cfp.setStatus(UNKNOWN);
          cfp.clearConseqs();
          cfp.setStatus(UNKNOWN);
        }

      }

    } catch (IllegalArgumentException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(OPTION_ERROR);
    } catch (FileNotFoundException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(FILE_NOT_FOUND);
    } catch (ParseException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(PARSE_ERROR);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace(System.err);
      System.exit(UNKNOWN_ERROR);
    }

  }

  static long time() {
    return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 1000000;
  }

  private final static class SHook extends Thread {
    public SHook(SOLAR solar, CFP cfp) {
      this.solar = solar;
      this.cfp   = cfp;
      this.opt   = cfp.getOptions();
      this.tid   = Thread.currentThread().getId();
    }
    public void run() {
      System.out.println();
      System.out.println("-------- INTERRUPTED --------");
      if (opt.getOutputFile() != null)
        cfp.getConseqSet().output(opt.getOutputFile());
      else {
        List<Conseq> conqs = cfp.getConseqSet().get();
        System.out.println(conqs.size() + " FOUND CONSEQUENCES");
        for (Conseq c : conqs) {
          System.out.println(c);
          if (opt.hasProofOp()) {
            System.out.println();
            System.out.println("PROOF:");
            System.out.println(c.getProof());
          }
          if (opt.hasUsedClausesOp()) {
            if (!opt.hasProofOp())
              System.out.println();
            System.out.println("USED CLAUSES:");
            for (Clause used : conqs.get(0).getUsedClauses())
              System.out.println(used);
            System.out.println();
          }
        }
      }
      System.out.println();
      solar.printStats(System.out, solar.threadMxBean.getThreadCpuTime(tid) / 1000000);
      System.out.println("-------- INTERRUPTED --------");
      System.out.flush();
    }
    private SOLAR   solar = null;
    private CFP     cfp   = null;
    private Options opt   = null;
    private long    tid   = 0;
  }

  /** The environment. */
  protected Env env = null;
  /** The consequence finding problem. */
  protected CFP cfp = null;
  /** The tableau. */
  protected Tableau tableau = null;
  /** The statistics information. */
  protected Stats stats = null;
  /** The options. */
  protected Options opt = null;
  /** Whether SOLAR is solving or not. */
  protected boolean isSolving;
  /** Characteristic clauses to be added during search process. */
  protected Queue<Clause> carcQueue = new ConcurrentLinkedQueue<Clause>();
  /** The start time of the SOLAR process. */
  protected long startTime = 0;
  /** For getting CPU time. */
  protected ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();

  public final static String VERSION = "SOLAR (SOL for Advanced Reasoning) 2.0 alpha (build 314)";

}

