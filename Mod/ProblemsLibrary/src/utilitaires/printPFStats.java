package utilitaires;

import solarInterface.SolProblem;
import cnfPb.SolProblemWithHeader;
import cnfPb.VariantProblem;

public class printPFStats {
	public static void printHelp(){
		System.out.println("Usage :");
		System.out.println("    printPFStats filename.var");
		System.out.println("Options");
		System.out.println("-h print this help.");	
	}
	
	
	public static void exec(String problemFilename){
		
		String filename=problemFilename.trim();
		if (filename.endsWith(".var"))
			filename=filename.substring(0,filename.length()-4);
		
		try {
			VariantProblem pb=new VariantProblem(filename);
			int s=pb.getVariantPField().getLiterals().size();
			System.out.println("Size of production field : "+s);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i=0;
		boolean verbose=true;
		while (i<args.length && args[i].startsWith("-")) {
			if (args[i].trim().equals("-verbose")){
				verbose=true;
				i++;
				continue;
			}
			else{
				printHelp();
				return;
			}
		}
		if (args.length<=i){
			printHelp();
			return;			
		}
		String problemFilename=args[i].trim();
		
		exec(problemFilename);
	}

}
