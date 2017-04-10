package partition;

import genLib.io.LoaderTool;
import genLib.tools.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.ExitStatus;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Node;
import org.nabelab.solar.pfield.PField;

import problemDistribution.DCFProblem;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import logicLanguage.WeightedGraph;

import solarInterface.SolProblem;

public class SolPartition {

	private static int computeWeight(Clause c1, Clause c2) {
		int w=0;
		for(Literal l: c1.getLiterals()){			
			for(Literal l2 : c2.getLiterals()){
				if(l.isSubsuming(l2) != null)
					w--;
				if(((l.getTerm().getName(0)) == (l2.getTerm().getName(0))) ){
					w++;
//					if ((l.getSign()!=l2.getSign()) && (l.isCompUnifiable(l2)!=null))
//						w--;
				}
			}
		}
//		if(c1.subsumes(c2))
//			w++;
		
		if(w!=0)
			w=w+ c1.getLiterals().size();
//		else 
//			w=1;
		return w;
	}

	private static void creatGraph(String problemFilename) throws Exception {
		
		int i,j;
		int nbEdges=0;
		
		for (i=0;i<theory.size();i++){
			List<Pair<Integer, Integer>> cl = new ArrayList<Pair<Integer,Integer>>();
			int weight =0;
			Clause c1= theory.get(i);
			for(j=0;j<theory.size();j++){
				if(j!=i){
					Clause c2= theory.get(j);
					weight = computeWeight(c1,c2);
					if(weight>0){
						Pair p = new Pair<Integer, Integer>(j, weight);
						cl.add(p);
						nbEdges++;
					}
				}
			}

			graph.add(cl);
			
		}
		WeightedGraph<Node> g =new WeightedGraph(graph, theory, nbEdges);
		try {
			LoaderTool.save(problemFilename, ".gra", g, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		creatDCF(problemFilename);
		
		
	}
	 public static String getOutPut(List<List<Clause>> dc) throws Exception {
		 String st="";
		 int ag=0;
		 String clause;
		 for (List<Clause> cls : dc){
			 st = st+"agent(ag"+ag+").\n";
			 for(Clause cl : cls){
				 if(cl.getType()==3)
					 clause=IndepClause.toSolFileLine(cl, "top_clause");
				 else
					 clause=IndepClause.toSolFileLine(cl, "axiom");
				 st = st+clause+"\n";
			 }
			 ag++;
		 }
		 st= st+"pf ("+pf.toString()+").";
		 return st;
	}
	

	private static void creatDCF(String problemFilename) throws Exception {
		int nbA[] = {2, 3, 4, 5, 6, 7, 8, 9 ,10};

		int minW[] = new int[graph.size()];

		int i=0;
		for (List<Pair<Integer, Integer>> cl : graph){
			minW[i]= cl.get(0).getRight();
			for(Pair<Integer, Integer> ed : cl){
				if(minW[i] >= ed.getRight())
					minW[i] = ed.getRight();
			}
			i++;
		}

		int j=0;
		int nbAg;
		
		
		for (int n=0; n<nbA.length;n++){
			boolean write =true;
			int k=graph.size();
			List<List<Clause>> dc = new ArrayList();
			nbAg=nbA[n];
			found : while (k>0){

				int indMin=0;
				indMin = getMinClause(minW, dc);
				boolean added = false;
				List<Pair<Integer, Integer>> pcl = graph.get(indMin);
				Clause cl = theory.get(indMin);
				if(nbAg<=0){
					if(!dc.isEmpty()){
						for(List<Clause> listCl : dc)
							if(listCl.contains(cl)){
								added =true;
								k--;
								break found;
							}
					}
					for(Pair<Integer, Integer> pair : pcl){
						int p =0;
						if(pair.getRight()==minW[indMin]){
							for(List<Clause> listCl : dc){
								if(listCl.contains(theory.get(pair.getLeft())))
									break;
								else
									p++;
							}
							if(p+1 <= dc.size()){
								dc.get(p).add(cl);
								k--;
								added=true;
								break;
							}

						}
					}
					//sinon la rajouter à la liste la plus courte
					if(!added){
						int mins = minListe(dc);
						dc.get(mins).add(cl);
						added=true;
					}
				}
				else {
					if(!dc.isEmpty()){
						int p=0;
						for(List<Clause> listCl : dc)
							if(listCl.contains(cl)){
								added =true;
								k--;
								break;
							}
							else p++;
						if(p+1 <= dc.size()){
							for (Pair<Integer, Integer> ed : pcl) {
								if (ed == null)
									break;
								//if(ed.getRight()<= minW[j] && !contains(dc, theory.get(ed.getLeft()))){
								if(ed.getRight()<= minW[j]){
									for(Pair<Integer, Integer> t : graph.get(ed.getLeft())){
										if(t.getLeft()== indMin && t.getRight()<=minW[ed.getLeft()]){
											dc.get(p).add(theory.get(ed.getLeft()));
											added=true;
											break;
										}
									}
										
									
								}
							}
						}
					}
					if(!added){
						List<Clause> l = new ArrayList<Clause>();
						l.add(cl);
						added = true;
						k--;
						nbAg--;
						for (Pair<Integer, Integer> ed : pcl) {
							if(!contains(dc,theory.get(ed.getLeft()))){
								if(ed.getRight()<= minW[indMin]){
									l.add(theory.get(ed.getLeft()));
									

									//upDate(ed.getLeft(), j);
								}
//								else{
//									List<Pair<Integer, Integer>> ncl = graph.get(ed.getLeft());
//									for(Pair<Integer, Integer> np :ncl){
//										if(np != null)
//											if((np.getLeft()==indMin) & (np.getRight()==minW[ed.getLeft()])){
//												l.add(theory.get(ed.getLeft()));
//												
//												break;
//											}
//									}
//								}
							}
						}


						dc.add(l);
					}
				}
				if(!added){
					write =false;
					minW[indMin]++;
					break;
				}
			}
			if(write){
				PrintWriter pr;
				try{

					pr = new PrintWriter(new FileWriter(problemFilename+"_agent"+dc.size()+".dcf"));
					pr.print(getOutPut(dc));
					pr.flush();
					pr.close();
				}
				catch(IOException e){
					e.printStackTrace();
				}
			}

		}

	}

	private static int minListe(List<List<Clause>> dc) {
		int m = 0;
		int i=0;
		int min = dc.get(0).size();
		for(List<Clause> cl : dc)
			if(min> cl.size()){
				min = cl.size();
				m=i;
			}
		i++;
		
		return m;
	}

	private static int getMinClause(int[] minW, List<List<Clause>> dcf2) {
		int min = 3600000;
		int j =0;
//		for(int i=0; i< theory.size(); i++){
//			if (((theory.get(i).size()< min) || (min==3600000)) && (!contains(dcf2, theory.get(i)))){
//				min = theory.get(i).size();
//				j=i;
//			}
//		}
		for(int i=0; i< graph.size(); i++){
			if((min > graph.get(i).size() || (min==3600000)) && (!contains(dcf2, theory.get(i)))){
				min = graph.get(i).size();
				j=i;
			}
		}
//		for(int i=0; i< minW.length; i++){
//			if (((minW[i]< min) || (min==3600000)) && (!contains(dcf2, theory.get(i)))){
//				min=minW[i];
//				j=i;
//			}
//		}
		return j;
	}

	private static boolean contains(List<List<Clause>> dcf2, Clause clause) {
		if(!dcf2.isEmpty()){
			for(List<Clause> listCl : dcf2)
				if(listCl.contains(clause))
					return true;
		}
		return false;
	}

	private static void upDate(Integer left, int j) {
		int i =0;
		for (List<Pair<Integer, Integer>> l : graph){
			int k=0;
			if(l!=null)
				for(Pair<Integer, Integer> p :l){
					if (p==null)
						break;
					if (p.getLeft()==j || p.getLeft()==left){
						graph.get(i).set(k, null);
					}
					k++;	
				}
			i++;
		}

	}

	private static void exec(String problemFilename) throws Exception {
		
		if (problemFilename.endsWith(".sol"))
			problemFilename=problemFilename.substring(0,problemFilename.length()-4);
		SolProblem pb  = new SolProblem(problemFilename);
		pf = pb.getPField();
		theory = pb.listClauses();
		
		creatGraph(problemFilename);
	}
	


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		String problemFilename=args[0].trim();
		if (problemFilename.endsWith(".sol"))
			problemFilename=problemFilename.substring(0,problemFilename.length()-4);
		else System.out.println(problemFilename +" is not a sol file");
		exec(problemFilename);
	}

	private static List<List<Pair<Integer, Integer>>> graph=new ArrayList<List<Pair<Integer,Integer>>>();
	//private static List<List<Clause>> dcf = new ArrayList<List<Clause>>();
	public static CNF theory;
	private static PField pf;


}
