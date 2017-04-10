//-var=_max-4_ld-1--1 -method=DICF-PB-Token-FixedOrder-3-2-1-0 -dist=_kmet4 -verbose glucolysis.sol debugGluc.csv
//-var=_max-4_ld-1--1 -method=DICF-PB-Async -dist=_kmet12 -verbose -t=3000 glucolysis.sol debugGluc.csv
//-method=DICF-PB-Async -dist=_def -verbose toy-pb1.sol debugi.csv
/**
 * 
 */
package launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.ExitStatus;
import org.nabelab.solar.parser.ParseException;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import problemDistribution.DCFProblem;
import problemDistribution.DistributedConsequenceFindingProblem;
import solarInterface.CFSolver;
import solarInterface.IndepPField;
import solarInterface.SolProblem;
import stats.ConsFindingAgentStats;
import stats.CsqHolder;
import stats.ExpeSummary;
import systemStructure.Tree;
import agLib.agentCommunicationSystem.CanalComm;
import cnfPb.VariantProblem;
import distNewCarc.partition.IncConsFindingAgent;
import distNewCarc.partition.asynchronous.PBAsyncIncConsFinding;
import distNewCarc.partition.newAsynch.PBNewAsyncIncConsFinding;
import distNewCarc.partition.starbased.PBStarIncConsFinding;
import distNewCarc.partition.tokenbased.PBTokenIncConsFinding;


/**
 * @author Gauvain Bourgne
 *
 */
public class CFLauncher {

	public static final int DICF_ASYNC=0;
	public static final int DICF_STAR=1;
	public static final int DICF_TOKEN=2;
	public static final int DICF_NewASYNC=3;


	public static DistributedConsequenceFindingProblem<SolProblem> setProblem(String problemRad, String variantSuffix, String distribSuffix) throws Exception{
		//load dcf
		DCFProblem dcf=new DCFProblem(problemRad+distribSuffix);
		//load var if any
		if (variantSuffix!=null && variantSuffix.length()>0){
			VariantProblem var=new VariantProblem(dcf.getEnv(), dcf.getOptions(), problemRad+variantSuffix);
			dcf.setGbPField(IndepPField.copyPField(dcf.getEnv(), dcf.getOptions(), var.variantPField));
			dcf.setMaxDepth(var.depthLimit);
		}
		return dcf;	
	}
	
	public static ExpeSummary partitionBasedNewAsyncIncCF(DistributedConsequenceFindingProblem<SolProblem> problem,
			String pbName, String distribSuffix, String method, boolean useNC, boolean pruneCsq, long deadline, String outputCsqFilename) throws Exception{
		long start = System.currentTimeMillis();
		PBNewAsyncIncConsFinding pb=new PBNewAsyncIncConsFinding(problem, useNC, pruneCsq, deadline);
		long middle = System.currentTimeMillis();
		boolean finished = pb.startExpe(deadline);
		
		long end = System.currentTimeMillis();
		// print some outpur
		Collection<Clause> consequences=pb.getOutput();
		System.out.println(""+consequences.size()+" (NEW) CHARACTERISTIC CLAUSES");
		System.out.println();
		for (Clause c:consequences){
			System.out.println(c);
		}
		System.out.println();
		System.out.println("Total execution time was " + (end - start) + " ms.\n");
		System.out.println("Execution time was " + (end - middle) + " ms.\n");
		if(!finished)
			System.out.println("---System Timeout---");

		System.out.println("DCIF PROBLEM NewASYNC");

		// set result line
		List<ConsFindingAgentStats> agStats=pb.getAllStats();
		ExpeSummary result=new ExpeSummary(pbName, distribSuffix, 0,method, end-start, consequences.size(),agStats);

		//ouput in csq file for checking results
		String stats = ExpeSummary.labels() + "\n" + result.toLine()+"\n";		
		saveConsequences(consequences,outputCsqFilename,!finished,stats);
		return result;
	}	

	public static ExpeSummary partitionBasedAsyncIncCF(DistributedConsequenceFindingProblem<SolProblem> problem,
			String pbName, String distribSuffix, String method, boolean useNC, boolean pruneCsq, long deadline, String outputCsqFilename) throws Exception{
		long start = System.currentTimeMillis();
		PBAsyncIncConsFinding pb=new PBAsyncIncConsFinding(problem, useNC, pruneCsq, deadline);
		long middle = System.currentTimeMillis();
		boolean finished = pb.startExpe(deadline);
		long end = System.currentTimeMillis();
		// print some outpur
		Collection<Clause> consequences=pb.getOutput();
		System.out.println(""+consequences.size()+" (NEW) CHARACTERISTIC CLAUSES");
		System.out.println();
		for (Clause c:consequences){
			System.out.println(c);
		}
		System.out.println();
		System.out.println("Total execution time was " + (end - start) + " ms.\n");
		System.out.println("Execution time was " + (end - middle) + " ms.\n");
		if(!finished)
			System.out.println("---System Timeout---");

		System.out.println("DCIF PROBLEM ASYNC");

		// set result line
		List<ConsFindingAgentStats> agStats=pb.getAllStats();
		ExpeSummary result=new ExpeSummary(pbName, distribSuffix, 0,method, end-start, consequences.size(),agStats);

		//ouput in csq file for checking results
		String stats = ExpeSummary.labels() + "\n" + result.toLine()+"\n";		
		saveConsequences(consequences,outputCsqFilename,!finished,stats);
		return result;
	}	

	public static ExpeSummary partitionBasedStarIncCF(DistributedConsequenceFindingProblem<SolProblem> problem,
			String pbName, String distribSuffix, String method, String methodRoot, boolean useNC, boolean pruneCsq, long deadline,String outputCsqFilename) throws Exception{
		long start = System.currentTimeMillis();
		//root
		int root=0;
		if (methodRoot==null || methodRoot.length()==0)
			methodRoot="FixedRoot-0";
		if (methodRoot.startsWith("FixedRoot-")){
			root=Integer.parseInt(methodRoot.substring(methodRoot.indexOf("-")+1));
		}

		PBStarIncConsFinding pb=new PBStarIncConsFinding(problem, useNC, pruneCsq, root, deadline);
		long middle = System.currentTimeMillis();
		boolean finished = pb.startExpe(deadline);
		long end = System.currentTimeMillis();

		// print some outpur
		Collection<Clause> consequences=pb.getOutput();
		System.out.println(""+consequences.size()+" (NEW) CHARACTERISTIC CLAUSES");
		System.out.println();
		for (Clause c:consequences){
			System.out.println(c);
		}
		System.out.println();
		System.out.println("Total execution time was " + (end - start) + " ms.\n");
		System.out.println("Execution time was " + (end - middle) + " ms.\n");
		if(!finished)
			System.out.println("---System Timeout---");
		System.out.println("DCIF PROBLEM STAR");


		// set result line
		List<ConsFindingAgentStats> agStats=pb.getAllStats();
		ExpeSummary result=new ExpeSummary(pbName, distribSuffix, 0,method, end-start, consequences.size(),agStats);


		//ouput in csq file for checking results
		String stats = ExpeSummary.labels() + "\n" + result.toLine()+"\n";		
		saveConsequences(consequences,outputCsqFilename,!finished,stats);
		return result;
	}	
	public static void saveConsequences(Collection<Clause> consequences, String name,boolean timeOut, String stats) throws Exception{
		CsqHolder csqH = new CsqHolder(consequences, timeOut);
		csqH.setStats(stats);
		csqH.save(name, true);
	}

	public static ExpeSummary partitionBasedTokenIncCF(DistributedConsequenceFindingProblem<SolProblem> problem,
			String pbName, String distribSuffix, String method, String methodOrder, boolean useNC, boolean pruneCsq, long deadline,String outputCsqFilename) throws Exception{
		long start = System.currentTimeMillis();
		//root
		List<Integer> order=new ArrayList<Integer>();

		if (methodOrder==null || methodOrder.length()==0 || methodOrder.equalsIgnoreCase("DefaultOrder")){
			methodOrder="DefaultOrder";
			for (int i=0;i<problem.getNbAgents();i++)
				order.add(i);
		}
		if (methodOrder.startsWith("FixedOrder-")){
			List<String> parseOrder=getMethodOptions(methodOrder);
			for (int i=1;i<parseOrder.size();i++){
				int num=Integer.parseInt(parseOrder.get(i));
				order.add(num);
			}
		}
		System.out.println("ORDER is: ");
		for (int n : order)
			System.out.print(n+"-");
		System.out.println("");

		PBTokenIncConsFinding pb=new PBTokenIncConsFinding(problem, useNC, pruneCsq, order, deadline);
		long middle = System.currentTimeMillis();
		boolean finished = pb.startExpe(deadline);
		long end = System.currentTimeMillis();

		// print some outpur
		Collection<Clause> consequences=pb.getOutput();
		System.out.println(""+consequences.size()+" (NEW) CHARACTERISTIC CLAUSES");
		System.out.println();
		for (Clause c:consequences){
			System.out.println(c);
		}
		System.out.println();
		System.out.println("Total execution time was " + (end - start) + " ms.\n");
		System.out.println("Execution time was " + (end - middle) + " ms.\n");
		if(!finished)
			System.out.println("---System Timeout---");
		System.out.println("DCIF PROBLEM TOKEN");
		

		System.out.println("ORDER is: ");
		for (int n : order)
			System.out.print(n+"-");
		System.out.println("");
		System.out.println("len of order is " + String.valueOf(order.size()));

		// set result line
		List<ConsFindingAgentStats> agStats=pb.getAllStats();
		ExpeSummary result=new ExpeSummary(pbName, distribSuffix, 0,method, end-start, consequences.size(),agStats);

		//ouput in csq file for checking results
		String stats = ExpeSummary.labels() + "\n" + result.toLine()+"\n";		
		saveConsequences(consequences,outputCsqFilename,!finished,stats);
		return result;
	}	


	public static SolProblem setMonoProblem(String problemRad, String variantSuffix, boolean turnToCarc) throws Exception{
		//load dcf
		SolProblem pb=new SolProblem(problemRad);
		//load var if any
		if (variantSuffix!=null && variantSuffix.length()>0){
			//Still don't know what these variant suffixes do
			VariantProblem var=new VariantProblem(pb.getEnv(), pb.getOptions(), problemRad+variantSuffix);
			pb.setPField(var.variantPField);
			pb.setDepthLimit(var.depthLimit);
		}
		if (turnToCarc){
			//Hmmm I think I get this
			List<Clause> top_clauses=pb.getTopClauses();
			pb.getAxioms().addAll(top_clauses);
			top_clauses.clear();
		}
		return pb;	
	}

	public static ExpeSummary solarCF(SolProblem pb, boolean incremental, boolean trueNewC, String pbName, String method, long deadline,String outputCsqFilename) throws Exception{
		long start = System.currentTimeMillis();
		ConsFindingAgentStats stat=new ConsFindingAgentStats();
		Collection<Clause> resultingCons=new ArrayList<Clause>();
		long middle = System.currentTimeMillis();
		int status = CFSolver.solveToClause(pb, deadline, stat.getSolarCtrList(), resultingCons, incremental, trueNewC, null);
		long end = System.currentTimeMillis();
		if (incremental){
			CNF reducedCons=new CNF();
			for (Clause cl:resultingCons)
				reducedCons.addAndReduce(cl);
			resultingCons=reducedCons;
		}

		System.out.println(""+resultingCons.size()+" CHARACTERISTIC CLAUSES");
		System.out.println();
		for (Clause c:resultingCons){
			System.out.println(c);
		}
		System.out.println();
		System.out.println("\nTotal execution time was " + (end - start) + " ms.\n");
		System.out.println("\nExecution time was " + (end - middle) + " ms.\n");

		boolean finished = true;
		System.out.println("status "+Integer.toString(status));
		if (status == ExitStatus.UNKNOWN){
			finished = false;
			System.out.println("---System Timeout---");
		}
		System.out.println("SOLAR PROBLEM");
		// set result line
		List<ConsFindingAgentStats> agStats=new ArrayList<ConsFindingAgentStats>();
		agStats.add(stat);
		ExpeSummary result=new ExpeSummary(pbName, "Mono", 0,method, end-start, resultingCons.size(),agStats);

		//ouput in csq file for checking results
		String stats = ExpeSummary.labels() + "\n" + result.toLine()+"\n";		
		saveConsequences(resultingCons,outputCsqFilename,!finished,stats);	
		
		return result;		
	}

	public static ExpeSummary runExpe(String method, String pbBaseName, String variantSuffix, String distributionSuffix, long timeLimitMillis,String outputCsqFilename) throws Exception{
		if (outputCsqFilename.equals(""))
			outputCsqFilename = pbBaseName + variantSuffix+"_" + method;

		ExpeSummary result=null;
		long deadline=-1;
		List<String> methodOptions=getMethodOptions(method);
		//Turns the method string into a list
		if (methodOptions.get(0).equalsIgnoreCase("SOLAR")){
			//If the method is SOLAR
			boolean inc=false;
			boolean trueNC=false;
			boolean turnToCarc=false;
			methodOptions.remove(0);
			for (String option:methodOptions){
				//Finds specific method used (need to find out what they all do)
				if (option.equalsIgnoreCase("Inc"))
					inc=true;
				if (option.equalsIgnoreCase("TrueNC"))
					trueNC=true;
				if (option.equalsIgnoreCase("Carc"))
					turnToCarc=true;				
			}
			SolProblem problem=setMonoProblem(pbBaseName, variantSuffix, turnToCarc);				//Set the problem
			if (timeLimitMillis!=-1) deadline=System.currentTimeMillis()+timeLimitMillis;
			result=solarCF(problem, inc, trueNC,pbBaseName+variantSuffix, method, deadline, outputCsqFilename);
			if (deadline>0 && deadline<System.currentTimeMillis()){
				System.out.println("------------------  TIME OUT --------------------");
			}
		}
		else
		if (methodOptions.get(0).equalsIgnoreCase("DICF")){
			//If the method is Distributed Incremental Consequence Finding
			DistributedConsequenceFindingProblem<SolProblem> problem=setProblem(pbBaseName, variantSuffix, distributionSuffix);	//Set the problem
			if (methodOptions.size()>2 && methodOptions.get(1).equalsIgnoreCase("PB")){
				//method
				boolean useNC=true;
				boolean pruneCsq=false;
				int pbMethod=DICF_ASYNC;
				if (methodOptions.get(2).equalsIgnoreCase("Star"))
					pbMethod=DICF_STAR;
				if (methodOptions.get(2).equalsIgnoreCase("Token"))
					pbMethod=DICF_TOKEN;
				if (methodOptions.get(2).equalsIgnoreCase("Async"))
					pbMethod=DICF_ASYNC;
				if (methodOptions.get(2).equalsIgnoreCase("NewAsync"))
					pbMethod=DICF_NewASYNC;
				//param heuristic and other optional param
				String pHeuristic=null;//"FixedRoot-0";
				if (methodOptions.size()>3){
					int i=3;
					boolean hasParamHeuristic=false;
					while (i<methodOptions.size()){
						if (methodOptions.get(i).equalsIgnoreCase("UseNC"))
							useNC=true;
						else if (methodOptions.get(i).equalsIgnoreCase("UseRC"))
							useNC=false;
						else if (methodOptions.get(i).equalsIgnoreCase("PruneSubs"))
							pruneCsq=false;
						else if (methodOptions.get(i).equalsIgnoreCase("PruneCsq"))
							pruneCsq=true;
						else {
							hasParamHeuristic=true;
							break;
						}
						i++;
					}
					if (hasParamHeuristic)
						pHeuristic=method.substring(method.lastIndexOf(methodOptions.get(i)));
				}
				//RUN
				if (timeLimitMillis!=-1) deadline=System.currentTimeMillis()+timeLimitMillis;
				switch(pbMethod){
				case DICF_ASYNC:
					result=partitionBasedAsyncIncCF(problem,pbBaseName+variantSuffix, distributionSuffix, method, useNC, pruneCsq, deadline, outputCsqFilename);
					break;
				case DICF_NewASYNC:
					result=partitionBasedNewAsyncIncCF(problem,pbBaseName+variantSuffix, distributionSuffix, method, useNC, pruneCsq, deadline, outputCsqFilename);
					break;
				case DICF_STAR:
					result=partitionBasedStarIncCF(problem,pbBaseName+variantSuffix, distributionSuffix, method, pHeuristic, useNC, pruneCsq, deadline, outputCsqFilename);
					break;
				case DICF_TOKEN:
					result=partitionBasedTokenIncCF(problem,pbBaseName+variantSuffix, distributionSuffix, method, pHeuristic, useNC, pruneCsq, deadline, outputCsqFilename);
					break;

				}


			}
		}
		return result;
	}


	private static List<String> getMethodOptions(String method){
		List<String> result=new ArrayList<String>();
		String head="";
		String tail=method;
		int ind=tail.indexOf('-');
		while (ind>=0){
			head=tail.substring(0,ind);
			result.add(head);
			tail=tail.substring(ind+1);
			ind=tail.indexOf('-');
		}
		result.add(tail);
		return result;
	}

	//Methods
	//  SOLAR
	//  SOLAR-Carc
	//  SOLAR-TrueNC
	//  SOLAR-Inc
	//  SOLAR-Inc-Carc
	//  SOLAR-Inc-TrueNC
	//  DCF-PB-Seq-FixRoot0
	//  DCF-PB-Par-MaxClSize


	////////////////////////////////////////////////////////////


	private static void exec(String resultFilename, String method, String pbBaseName, String variantSuffix, String distributionSuffix, long timeLimitMillis,String outputCsqFilename) throws Exception{
		boolean label=false;
		System.out.println("resultFilename :"+resultFilename);
		System.out.println("nethod:"+method);

		System.out.println("pbBasename :"+pbBaseName);

		System.out.println("variantSuffix:"+variantSuffix);

		System.out.println("distr suff:"+distributionSuffix);
		System.out.println("timout :"+ String.valueOf(timeLimitMillis));

		ExpeSummary res=runExpe(method, pbBaseName, variantSuffix, distributionSuffix, timeLimitMillis, outputCsqFilename);
		//pbName = pbBaseName+variantSuffix
		//resultfilename = pbName + "_" + method
		//tout est passé dans le result filename
		File accesFichier = new File(resultFilename+".csv") ;  	
		if (!accesFichier.exists()){
			//Creates output file if it doesn't already exist
			accesFichier.createNewFile();
			label=true;											//I do not know what this does
		}	 		
		PrintStream fileOut = new PrintStream(new FileOutputStream(accesFichier, true));
		if (label) fileOut.println(ExpeSummary.labels());		//prints something
		fileOut.println(res.toLine());							//prints something else
		fileOut.close();
	}

	public static void printHelp(){
		System.out.println("Launch an expe with the given parameters and append the result line to the given outpur file");
		System.out.println("Usage :");
		System.out.println("    CFLaucher [Options] baseProblem[.sol] output[.csv]");
		System.out.println("Options");
		System.out.println("-method=xxx use method xxx.");
		System.out.println("          xxx=SOLAR-Carc (default), SOLAR-Inc-Carc, DCF-PB-Seq-FixedRoot-0, DCF-PB-Par-MaxClSize,...");
		System.out.println("          	DICF-PB-Async");
		System.out.println("-t=N  set time limit.");
		System.out.println("-var=varSuffix  use the variant with given suffix (should begin by \"_\").");
		System.out.println("-dist=distSuffix  use the distribution with given suffix (should begin by \"_\").");
		System.out.println("-csq=outputCsq  output the consequences in this file\"_\").");

	}

	public static void main(String[] args){
		String resultFilename="resultIndiv";
		String method="SOLAR-Carc";
		String variantSuffix="";
		String distributionSuffix="";
		long timeLimitMillis=-1;
		String outputCsqFilename = "";

		CFSolver.verbose=false;
		Tree.verbose=false;
		CanalComm.verbose=false;

		int i=0;
		//Starts processing arguments
		while (args[i].startsWith("-")) {
			if (args[i].startsWith("-method=")){
				//Methods, need to find out what they do
				//  SOLAR
				//  SOLAR-Carc
				//  SOLAR-TrueNC
				//  SOLAR-Inc
				//  SOLAR-Inc-Carc
				//  SOLAR-Inc-TrueNC
				//  DCF-PB-Seq-FixRoot0
				//  DCF-PB-Par-MaxClSize
				method=args[i].substring(args[i].indexOf("=")+1).trim();
				i++;
				continue;
			}
			if (args[i].startsWith("-verbose")){
				//I expect this to make the program explain everything it does
				CFSolver.verbose=true;
				Tree.verbose=true;
				CanalComm.verbose=true;
				IncConsFindingAgent.verbose=true;
				i++;
				continue;
			}
			if (args[i].startsWith("-var=")){
				//No idea what a variant suffix is, need to find out
				variantSuffix=args[i].substring(args[i].indexOf("=")+1).trim();
				i++;
				continue;
			}
			if (args[i].startsWith("-dist=")){
				//Also no idea what distribution suffix does
				distributionSuffix=args[i].substring(args[i].indexOf("=")+1).trim();
				i++;
				continue;
			}
			if (args[i].startsWith("-t=")){
				//Sets the time limit in milliseconds
				timeLimitMillis=Long.parseLong(args[i].substring(args[i].indexOf("=")+1));
				i++;
				continue;
			}
			if (args[i].startsWith("-csq=")){
				//Sets output file for consequences
				outputCsqFilename=args[i].substring(args[i].indexOf("=")+1).trim();
				i++;

				continue;
			}
			if (args[i].startsWith("-depth")){
				//Sets the term Depths checker to true
				CFSolver.depthterm = true;
				i++;
				continue;
			}
			else{
				//Prints help in case no allowed argument was detected
				printHelp();
				return;
			}
		}

		String problemFilename=args[i].trim();											//Gets input filename
		if (problemFilename.endsWith(".sol"))
			problemFilename=problemFilename.substring(0,problemFilename.length()-4);	//Removes file extension
		if (args.length>i+1){															//Checks for output filename
			resultFilename=args[i+1].trim();											//Gets it
			if (resultFilename.endsWith(".csv"))
				resultFilename=resultFilename.substring(0,resultFilename.length()-4);	//Removes file extension
		}			
		resultFilename=resultFilename.trim();
		try {
			exec(resultFilename, method, problemFilename, variantSuffix, distributionSuffix, timeLimitMillis,outputCsqFilename);	//Starts execution
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

