package problemDistribution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import logicLanguage.CNF;
import logicLanguage.IndepClause;
import solarInterface.CFSolver;
import solarInterface.IndepPField;
import solarInterface.SolProblem;
import logicLanguage.UnitClauseCNF;
import mars.reasoning.LocalTheory;

import org.jdom.*;
import org.jdom.input.*;
import org.nabelab.solar.CFP;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.parser.ParseException;

import agLib.linkingGraph.LinkingGraph;



import genLib.tools.ToolsJDomElement;



public class XMLDistributedProblem implements DistributedConsequenceFindingProblem<SolProblem>{

	static boolean verboseXMLloading=false;
	
	public int nbAgents;
	public CFSolver solver;
	public LocalTheory[] agentTheories;
	public List<String>[][] agLanguages;
	public LinkingGraph graph;
	/** current rule redundancy */
	private double currentRuleRedundancy;
	/** current observations redundancy */
	private double currentObsRedundancy;
	private int sizeInitialTheory;
	private int nbInitialObs;
	public boolean indivLanguages;
	
	public XMLDistributedProblem(String filenameNoExt, boolean indivLanguages){
		solver=new CFSolver();
		this.indivLanguages=indivLanguages;
	    try {
			loadXMLdoc(filenameNoExt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public XMLDistributedProblem(String filenameNoExt, LinkingGraph g, boolean indivLanguages){
		solver=new CFSolver();
		this.indivLanguages=indivLanguages;
	    try {
			loadXMLdoc(filenameNoExt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		graph=g;
		adaptCommonLanguagesToGraph(graph);
		try {
			initLanguage();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public XMLDistributedProblem(String filenameNoExt, String grapheNameRadical, 
			boolean indivLanguages){
		solver=new CFSolver();
		this.indivLanguages=indivLanguages;
	    try {
			loadXMLdoc(filenameNoExt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		graph = new LinkingGraph(grapheNameRadical+nbAgents);
	    adaptCommonLanguagesToGraph(graph);
		try {
			initLanguage();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	
	private void xmlParseComment(String c){
		if (verboseXMLloading)
			System.out.println(c);
	}
	
	@SuppressWarnings("unchecked")
	public void initAgentTheories(int n){
		//ds=new DiagStats(nbAgents);
		agentTheories=new LocalTheory[n];
		agLanguages=new ArrayList[n][n];
		for (int i=0;i<n;i++){
			agentTheories[i]=new LocalTheory(solver,i);
			for (int j=0;j<=i;j++){
				agLanguages[i][j]=new ArrayList<String>(); 
				agLanguages[j][i]=agLanguages[i][j];
			}
		}
	}
	
	protected Document loadFile(String filenameNoExt) throws JDOMException, IOException{
		//	Element root;
	    //On crée une instance de SAXBuilder
	    SAXBuilder sxb = new SAXBuilder();
	    File file=new File(filenameNoExt+".xml"); //"t08r1-dist1-6ag.xml"
    	//  System.out.println(file.getAbsoluteFile());
    	return (sxb.build(file));
	}

	protected void countRulesAndObs(Element root,Element common){
		Element centralized=common;
		for (Element ag:ToolsJDomElement.typeAsElement(root.getChildren("Agent"))){
			centralized=ToolsJDomElement.fusionContent(centralized,ag,true);
		}
		LocalTheory wholeTheory=new LocalTheory(solver,-1);
		try {
			initProgram(centralized,wholeTheory);
			initObservations(centralized,wholeTheory);
		} catch (ParseException e) {e.printStackTrace();}
		sizeInitialTheory=wholeTheory.getTheory().size();
		nbInitialObs=wholeTheory.getManifestations().size();
		centralized=null;
		wholeTheory=null;
	}
	protected Element setAgentElement(Element ag, Element common){
		  Element agent=ToolsJDomElement.fusionContent(common,ag,false);
	  	  return agent;
	}
	
	protected void initProgram(Element agent,LocalTheory agTheory) throws ParseException{
	  String program=agent.getChildText("Program");
  	  xmlParseComment("Program \n "+program);
  	  Env env=new Env();
  	  CFP cfp = new CFP(env, new Options(env));
	  cfp.parse(program);
	  xmlParseComment("Translation to CFP: \n "+cfp);
	  CNF prog=new CNF(cfp.getClauses());
	  agTheory.addToTheory(prog,null,true);
	  agTheory.setHypothesesField(IndepPField.parse(cfp.getPField().toString()));
	}
	
	protected void initObservations(Element agent,LocalTheory agTheory) throws ParseException{
		String manifs=agent.getChildText("Observations");
		if (manifs==null) manifs="";
	    xmlParseComment("Observations \n "+manifs);
	    Env env=new Env();
	    CFP cfp = new CFP(env, new Options(env));
        cfp.parse(manifs);
		xmlParseComment("Translation to CFP: \n "+cfp);
		agTheory.addManifestations(new UnitClauseCNF(cfp.getClauses()),null);
		xmlParseComment("AgTheory \n"+agTheory);
	}
	
	protected void initOrdering(Element agent,LocalTheory agTheory) {
		Element weighting=agent.getChild("Weighting");
		Comparator<IndepClause> order=new WeightTable(weighting);
		agTheory.setHypOrdering(order);
	}
		
	
	protected List<String> formLanguage(Element agent) {
		  Element language=agent.getChild("CommLanguage");			  
		  Element formats=language.getChild("Format");
		  Element typeVar=language.getChild("ConstantTypes");
		  Element varDomain=language.getChild("ConstantsDomain");
		  List<String> pLiterals=new ArrayList<String>();
		  List<String> toAdd=new ArrayList<String>();
		  List<String> toRemove=new ArrayList<String>();			  
		  for (Element pLitElem:ToolsJDomElement.typeAsElement(formats.getChildren("pLitGroup")))				  
			  pLiterals.add(pLitElem.getTextTrim());
		  for (Element varEl:ToolsJDomElement.typeAsElement(typeVar.getChildren())){
			  String vName=varEl.getName();
			  String vType=varEl.getTextTrim();
			  toAdd.clear(); toRemove.clear();				  
			  for (String pLit:pLiterals){
				  if (pLit.contains(vName)) {
					  toRemove.add(pLit);
					  for (Element valEl:ToolsJDomElement.typeAsElement(varDomain.getChildren(vType))){
						  String newPLit=pLit.replace(vName,valEl.getTextTrim());							
						  toAdd.add(newPLit);
					  }
				  }
			  }
			  pLiterals.removeAll(toRemove);
			  pLiterals.addAll(toAdd);
		  }
		  return pLiterals;
	}
		  
	protected void initLanguage() throws ParseException{
	   for (int i=0;i<nbAgents;i++){
		  List<String> pLiterals=agLanguages[i][i];
		  LocalTheory agTheory=agentTheories[i];
		  IndepPField p = IndepPField.parse(pLiterals.toString());
		//  agTheory.addToExtAbduciblesField(p.addPrefix("p_", false)); 
		  agTheory.setContextLanguage(p);
		  xmlParseComment("ExtHypField (ag"+i+") : "+p);
	   } 
	}
	
	public void loadXMLdoc(String filenameNoExt) throws JDOMException, IOException, ParseException{
	    Document document=loadFile(filenameNoExt);
	    Element root = document.getRootElement();
	    xmlParseComment("Parsing....");
	    nbAgents=Integer.parseInt(root.getChild("Infos").getChildTextTrim("NbAgents"));
	    countRulesAndObs(root,root.getChild("Common"));
	    xmlParseComment("NbAgents : "+nbAgents);
	    initAgentTheories(nbAgents);        
	      for (Element ag:ToolsJDomElement.typeAsElement(root.getChildren("Agent"))){
	    	  Element agent=setAgentElement(ag,root.getChild("Common"));
	    	  int i=Integer.parseInt(agent.getAttributeValue("i"));
	      	  xmlParseComment("AGENT "+i); 
	    	  //Local Theory
	    	  initProgram(agent,agentTheories[i]);
	    	  //Manifestations
	    	  initObservations(agent,agentTheories[i]);	    	  
	    	  //ordering : weighting
	    	  initOrdering(agent,agentTheories[i]);
	    	  //Language
	    	  agLanguages[i][i]=this.formLanguage(agent);
	      }
	    setCommonLanguages();
	    initLanguage();
	}
	
public double getCurrentRuleRedundancy() {
		return currentRuleRedundancy;
	}
	public double getCurrentObsRedundancy() {
		return currentObsRedundancy;
	}
	/*	public static String concatNoNull(String s1, String s2){
		String result="";
		if (s1!=null) result+=s1;
		if (s2!=null) result+=s2;
		return result;
	}*/
     
    public void setCommonLanguages() throws ParseException{
    	Env env=new Env();
    	Options opt=new Options(env);
    	int back=env.getVarTable().state();
    	//compute intersection of languages
    	for (int i=0;i<nbAgents;i++){
    		for (int j=0;j<i;j++){
    			List<String> l1=agLanguages[i][i];
    			List<String> l2=agLanguages[j][j];
    			for (String s1:l1){
    				Literal lit1=Literal.parse(env,opt, s1);
    				for (String s2:l2){
    					env.getVarTable().backtrackTo(back);
						lit1=Literal.parse(env, opt, s1);
    					Literal lit2=Literal.newOffset(Literal.parse(env, opt, s2),lit1.getNumVars());
    					if (lit1.isUnifiable(lit2)!=null) {
    						lit1.getTerm().unify(lit2.getTerm());
    						agLanguages[i][j].add(lit1.toString());
    					}				
    				}
    			}
    		}
    	}
    	if (indivLanguages)
    		reduceCommLanguages();
    	else
    		reduceToFullCommonLanguage();
    }
    
    public void reduceCommLanguages(){
    	//reduce agLanguages[i][i] to the full comm. language of agent i 
    	//(ie the union of all its comm. languages with other agents)
    	for (int i=0;i<nbAgents;i++){
    		agLanguages[i][i].clear();
    		for (int j=0;j<nbAgents;j++)
    			if (j!=i) for (String s:agLanguages[i][j])
    					if (!agLanguages[i][i].contains(s))
    						agLanguages[i][i].add(s);    		
    	}
    }
    
    public void reduceToFullCommonLanguage(){
    	//reduce agLanguages[i][i] to the full comm. language of agent i 
    	//(ie the union of all the comm. languages of the agents)
    	List<String> common=new ArrayList<String>();
    	for (int i=0;i<nbAgents;i++){
    		agLanguages[i][i].clear();
    		for (int j=0;j<nbAgents;j++)
    			if (j!=i) for (String s:agLanguages[i][j])
    					if (!common.contains(s))
    						common.add(s);    		
    	}
    	for (int i=0;i<nbAgents;i++)
    		agLanguages[i][i].addAll(common);
    }
    
    
    public void adaptCommonLanguagesToGraph(LinkingGraph graph){
    	int[] path;
    	for (int i=1;i<nbAgents;i++)
    		for (int j=0;j<i;j++){
    			// test if language must be expanded and do through the shortest path it if needed
    			if (!agLanguages[i][j].isEmpty() && !graph.existeLien(i, j)){
    				path=graph.getShortestPath(i, j);
    				for (int k=1;k<path.length;k++){
    					agLanguages[path[k-1]][path[k]].addAll(agLanguages[i][j]);
    					agLanguages[path[k]][path[k]].addAll(agLanguages[i][j]);
    				}
    				agLanguages[i][j].clear();
    			}    			
    	}
    }
    
    public List<String>[] getCommonLanguages(int i){
    	return this.agLanguages[i];
    }
   
    public List<String> getCommonLanguage(){
    	return this.agLanguages[0][0];
    }
 
    public List<SolProblem> getDistTheory(){
    	List<SolProblem> res=new ArrayList<SolProblem>();
    	for(int i=0;i<nbAgents;i++)
    		res.add(new SolProblem(agentTheories[i].getTheory(), new CNF(), getGbPField()));
    	return res;
    }
    
    
	public void computeRedundancy(){			
		int nbTotRules=0;
		int nbTotObs=0;
		for (int i=0;i<nbAgents;i++){
			nbTotRules+=agentTheories[i].getTheory().size();
			nbTotObs+=agentTheories[i].getManifestations().size();
		}
		
		currentRuleRedundancy=(double)nbTotRules/sizeInitialTheory;
		currentObsRedundancy=(double)nbTotObs/nbInitialObs;
	}
    
	public static void main(String[] args) throws ParseException
	{
		
		XMLDistributedProblem pb=new XMLDistributedProblem("t08r1-dist1-6ag",true);
		pb.setCommonLanguages();
		for (int i=0;i<pb.nbAgents;i++){
			for (int j=0;j<i;j++){
				pb.xmlParseComment("language ag"+i+" & ag"+j+": \n"+pb.agLanguages[i][j]);
			}
		}
		LinkingGraph g=new LinkingGraph("Ligne_6");
		pb.adaptCommonLanguagesToGraph(g);
		for (int i=0;i<pb.nbAgents;i++){
			for (int j=0;j<i;j++){
				if (!pb.agLanguages[i][j].isEmpty()) pb.xmlParseComment("New language ag"+i+" & ag"+j+": \n"+pb.agLanguages[i][j]);
			}
		}


	      /*
	       * TODO
	       * 0.recuperer nbagent et initialiser les agents
	       * A. Puis, pour chaque agent :
	       ** 1.recuperer pour chaque agent le program, et 
	       ** 1.b le concatener avec le program commun
	       ** 1.c stocker les clauses ainsi trouvee dans la theorie de chaque agent
	       ** 1.d stocker aussi le production field (attention a la negation)
	       ** 2.idem pour les observations
	       ** 3.recuperer les infos sur le langage dans common et agent
	       ** 3.b Pour chaque elements du format, et identifier les types variable present dans cet element, 
	       ** 3.b.2 pour chaque combinaison de variable, generer tous les pLit correspondant
	       ** 3.c contruire le pfield a partir de la liste de tous ces elements
	       ** B. Ensuite, generer les langages de comm
	       ** 0. Initialiser a vide tous les languages d'agents
	       ** 1. Pour chaque couple d'agents,reperer les elements communs
	       ** 2. Si les 2 agents ne sont pas connectés, trouver le plus court chemin entre eux
	       ** 2.b reporter les éléments communs dans les langages de ce chemin
	       * 
	       * 
	       */

	      //Méthode définie dans la partie 3.2. de cet article
	     
	}
	public int getNbAgents() {
		return nbAgents;
	}
	public IndepPField getGbPField() {
		if (agentTheories.length>0)
			return agentTheories[0].getHypothesesField();
		return new IndepPField();
	}
	public void setMaxLength(int maxLength) {
		for (LocalTheory lt:agentTheories){
			lt.setHypothesesField(lt.getHypothesesField().setMaxLength(maxLength));
		}
	}
	public void setGbPField(IndepPField pf) {
		for (int i=0;i<agentTheories.length;i++){
			agentTheories[i].setHypothesesField(pf);
		}
		
	}
	public int getMaxDepth() {
		// TODO Auto-generated method stub
		return -1;
	}
	public void setMaxDepth(int maxDepth) {
		// TODO Auto-generated method stub
		
	}
}
