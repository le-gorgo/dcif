package problemDistribution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import logicLanguage.IndepClause;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.nabelab.solar.Clause;
import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Term;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.parser.ParseException;

import genLib.tools.ToolsJDomElement;



public class WeightTable implements Comparator<IndepClause>, TermTypes {
	
	// aim : be able to attribute weight to
	// (symbolName, SymbolType)
	// should arity matter ? decision : NO
	
	public WeightTable(){
		super();
		makeInitialDefauts();
	}
	
	public WeightTable(Element weightingElement){
		this();
		if (weightingElement!=null)
			loadFromXmlElement(weightingElement);
	}
	
	public WeightTable(String filename) throws JDOMException, IOException{
		this();
		Document doc=loadFile(filename);
		Element root=doc.getRootElement();
		if (!root.getName().equals("Weighting")){
			root=root.getChild("Weighting");
		}
		if (root==null){
			root=doc.getRootElement().getChild("Common");
			if (root!=null)
				root=root.getChild("Weighting");
		}
		if (root!=null)
			loadFromXmlElement(root);
	}
	
	
	protected Document loadFile(String filenameNoExt) throws JDOMException, IOException{
		//	Element root;
	    //On crée une instance de SAXBuilder
	    SAXBuilder sxb = new SAXBuilder();
	    File file=new File(filenameNoExt+".xml"); //"t08r1-dist1-6ag.xml"
    	//  System.out.println(file.getAbsoluteFile());
    	return (sxb.build(file));
	}

	// if the symbol is unknown, return 0, which is the index of the defaults values
	public int getInd(String symbol, int type){
		int res;
		switch (type){
		case CONSTANT:
			res= constantsName.indexOf(symbol);
			break;
		case FUNCTION:
			res= functionsName.indexOf(symbol);
			break;
		case PREDICATE:
			res= predicatesName.indexOf(symbol);
			break;
		case INTEGER:
		case VARIABLE:
		default:
			return 0;
		}
		if (res<0) res=0;
		return res;
	}
	
	public String getSymbol(Term t, Env env){
		return env.getSymTable().get(t.getStartName(), t.getStartType());
	}
	
	public int getArity(Term t, Env env){
		return env.getSymTable().getArity(t.getStartName(), t.getStartType());
	}
	
	public double getFullWeight(Term t,Term parent,Env env){
		int type=t.getStartType();
		String symbol=getSymbol(t,env);
		int ind=getInd(symbol, type);
		int typeParent=parent.getStartType();
		String symbolParent=getSymbol(parent,env);
		switch(type){
		case INTEGER:
		case VARIABLE:
		case CONSTANT:
			return this.getOwnWeight(symbol, type, symbolParent, typeParent);
		case FUNCTION:
		case PREDICATE:
			int aggregator=getAggregator(type).get(ind);
			int useParam=getUseParam(type).get(ind);
			double w=getOwnWeight(symbol, type, symbolParent, typeParent);
			return w*computeAggregatedWeight(t,env,aggregator,useParam);
		default:
			return -1;		
		}
	}
	
	//computed the aggregatedWeight of param
	// redirect to submethods for different aggregators
	private double computeAggregatedWeight(Term t, Env env, int aggregator,
			int useParam) {
		int arity=getArity(t, env);
		if (arity==0) return 1;
		List<Double> argWeights=getUsedArgWeight(t,env,useParam);
		return aggregate(argWeights,aggregator);
	}
	private double aggregate(List<Double> weightList, int aggregator){
		switch(aggregator){
		case AGG_PRODUCT: 
			return productAggregate(weightList);
		case AGG_MAX:
			return maxAggregate(weightList);
		case AGG_MIN:
			return minAggregate(weightList);
		case AGG_AVERAGE:
			return avgAggregate(weightList);
		default:
			return 1;
		}
	}
	
	private boolean usingParam(Term param, Env env, int useParam, int pos){
		int typeArg=param.getStartType();
		switch(useParam){
		case USE_NONVAR:
			return typeArg!=VARIABLE;
		case USE_ONLYVAR:
			return typeArg==VARIABLE;
		case USE_ALLPARAM:
		default:
			return true;
		}
	}
	
	private List<Double> getUsedArgWeight(Term t, Env env, int useParam){
		List<Double> res=new ArrayList<Double>();
		for (int i=0; i<getArity(t,env); i++){
			Term arg=t.getArg(i);
			if (usingParam(arg,env,useParam,i))
				res.add(getFullWeight(arg, t, env));
		}
		return res;
		
	}
	private double productAggregate(List<Double> weights){
		double w=1;
		for (Double d:weights) w*=d;
		return w;
	}
	private double maxAggregate(List<Double> weights){
		double w=0;
		for (Double d:weights) 
			if (w<d) w=d;
		if (w==0) return 1;
		return w;
	}
	private double minAggregate(List<Double> weights){
		double w=1;
		for (Double d:weights) 
			if (w>d && d>=0) w=d;
		return w;
	}
	private double avgAggregate(List<Double> weights){
		double w=1;
		for (Double d:weights) w+=d;
		if (weights.isEmpty()) return 1;
		return w/weights.size();
	}

	public double computeWeight(Term t, Env env){
		return getFullWeight(t,t,env);		
	}
	
	public double computeWeight(Literal lit, Env env){
		return computeWeight(lit.getTerm(),env);
	}

	public double computeWeight(Clause cl, Env env){
		List<Double> weights=new ArrayList<Double>();
		for (Literal lit:cl.getLiterals()){
			weights.add(computeWeight(lit, env));
		}
		if (weights.isEmpty()) return 1;
		return aggregate(weights, clauseAggregator);
	}	
	
	public double computeWeight(IndepClause cl) throws ParseException{
		Env env=new Env();
		return computeWeight(cl.toClause(env),env);
	}
	
	
	
	public double getOwnWeight(String symbol, int type,String parentSymbol, int parentType){
		switch (type){
		case CONSTANT:
		case FUNCTION:
		case PREDICATE:
			return getWeightList(type).get(getInd(symbol,type)).doubleValue();
		case INTEGER:
		case VARIABLE:
			return getIntVarWeight(parentType,type).get(getInd(parentSymbol,parentType)).doubleValue();
		default:
			return -1;
		}
	}
	
	public List<String> getNameList(int type){
		switch (type){
		case CONSTANT:
			return constantsName;
		case FUNCTION:
			return functionsName;
		case PREDICATE:
			return predicatesName;
		default:
			return null;
		}
	}

	public List<Double> getWeightList(int type){
		switch (type){
		case CONSTANT:
			return constantsWeight;
		case FUNCTION:
			return functionsWeight;
		case PREDICATE:
			return predicatesWeight;
		default:
			return null;
		}
	}
		
	public List<Integer> getAggregator(int type){
		switch (type){
		case FUNCTION:
			return functionsAggregator;
		case PREDICATE:
			return predicatesAggregator;
		default:
			return null;
		}
	}
	public List<Integer> getUseParam(int type){
		switch (type){
		case FUNCTION:
			return functionsUseParam;
		case PREDICATE:
			return predicatesUseParam;
		default:
			return null;
		}
	}
	
	public List<Double> getIntVarWeight(int typeParent,int typeArg){
		switch (typeParent){
		case FUNCTION:
			switch (typeArg){
			case INTEGER:
				return functionsIntWeight;
			case VARIABLE:
				return functionsVarWeight;
			}
		case PREDICATE:
			switch (typeArg){
			case INTEGER:
				return predicatesIntWeight;
			case VARIABLE:
				return predicatesVarWeight;
			}
		}
		return null;
	}
	
	//getWeight(String symbol, int type, String parentSymbol, int parentType)
	//getVarWeight(String embeddedIn, int typeNesting)
	//getFullFunctionWeight(Term t, int pos)
	
	/**
     * Compares its two arguments for order.
     * @param sig1 the first object to be compared.
     * @param sig2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     */
    public int compare(IndepClause cl1, IndepClause cl2) {
      double comp;
	try {
		double w1=computeWeight(cl1);
		double w2=computeWeight(cl2);
		comp = w2-w1;
	      return (int)Math.signum(comp);
	} catch (ParseException e) {
		e.printStackTrace();
		return 100;
	}
    }
	
    public void loadFromXmlElement(Element root){
    	Element tempElem;
    	String tempText;
    	//GlobalParams
    	tempElem=root.getChild("GlobalParams");
    	if (tempElem!=null){
    		tempText=tempElem.getChildTextTrim("ClauseAggregator");
    		if (tempText!=null)
    			clauseAggregator=toCode(tempText);
    	}
    	//Defaults
    	tempElem=root.getChild("Defaults");
    	if (tempElem!=null)
    		processDefaults(tempElem);
    	//Symbols
    	tempElem=root.getChild("Symbols");
    	if (tempElem!=null)
    		for (Element symbol:ToolsJDomElement.typeAsElement(tempElem.getChildren()))
    			processSymbolElement(symbol, root);	
    }
	
    public int toCode(String s_code){
    	if (s_code.equals(S_AGG_AVERAGE))
    		return AGG_AVERAGE;
    	if (s_code.equals(S_AGG_MAX))
    		return AGG_MAX;
    	if (s_code.equals(S_AGG_MIN))
    		return AGG_MIN;
    	if (s_code.equals(S_AGG_PRODUCT))
    		return AGG_PRODUCT;
    	if (s_code.equals(S_USE_ALLPARAM))
    		return USE_ALLPARAM;
    	if (s_code.equals(S_USE_NONVAR))
    		return USE_NONVAR;
    	if (s_code.equals(S_USE_ONLYVAR))
    		return USE_ONLYVAR;
    	return -1;
    }
    
    private void processDefaults(Element defaults){
       	if (defaults.getChild("PredicateDefaults")!=null){
    		setTempFromTextElement(PREDICATE, defaults.getChild("PredicateDefaults"));
    		setFromTemp(0, PREDICATE);
    	}
    	if (defaults.getChild("FunctionDefaults")!=null){
    		setTempFromTextElement(FUNCTION, defaults.getChild("FunctionDefaults"));
    		setFromTemp(0, FUNCTION);
    	}
       	if (defaults.getChild("ConstantDefaults")!=null){
    		setTempFromTextElement(CONSTANT, defaults.getChild("ConstantDefaults"));
    		setFromTemp(0, CONSTANT);
    	}
       	if (defaults.getChild("IntegerDefaults")!=null){
    		setTempFromTextElement(INTEGER, defaults.getChild("IntegerDefaults"));
    		setFromTemp(0, INTEGER);
    	}
       	if (defaults.getChild("VariableDefaults")!=null){
    		setTempFromTextElement(VARIABLE, defaults.getChild("VariableDefaults"));
    		setFromTemp(0, VARIABLE);
    	}
    }
    
    private void processSymbolElement(Element symbol, Element root){
    	String name=symbol.getTextTrim();
    	int type=-1;
    	if (symbol.getName().equals("Predicate"))
    		type=PREDICATE;
    	if (symbol.getName().equals("Function"))
    		type=FUNCTION;
    	if (symbol.getName().equals("Constant"))
    		type=CONSTANT;
    	setTempFromDefaut(type);
    	setTempFromAttributeElement(type, symbol, root);
    	setFromTemp(name, type);
    }
    
    private void setTempFromClass(int type, String sClass, Element root){
    	Element eClass=root.getChild("Classes");
    	if (eClass==null) return;
    	switch(type){
    	case FUNCTION:
    		eClass=eClass.getChild("FunctionClasses");
    		break;
    	case PREDICATE:
    		eClass=eClass.getChild("PredicateClasses");
    		break;
    	case CONSTANT:
    		eClass=eClass.getChild("ConstantClasses");
    		break;
    	default: 
    		eClass=null;
    	}
    	if (eClass==null) return;
    	eClass=eClass.getChild(sClass);
    	if (eClass!=null)
    		setTempFromTextElement(type,eClass);
    }
    
    private void setTempFromTextElement(int type,Element e){
    	switch(type){
		case FUNCTION:
		case PREDICATE:
				if (e.getChildText("Aggregator")!=null)
					tempAggregator=toCode(e.getChildTextTrim("Aggregator"));
				if (e.getChildText("UseParam")!=null)
					tempUseParam=toCode(e.getChildTextTrim("UseParam"));
				if (e.getChildText("IntegerWeight")!=null)
					tempIntW=Double.parseDouble(e.getChildTextTrim("IntegerWeight"));
				if (e.getChildText("VariableWeight")!=null)
					tempVarW=Double.parseDouble(e.getChildTextTrim("VariableWeight"));
		case CONSTANT:
		case INTEGER:
		case VARIABLE:
				if (e.getChildText("Weight")!=null)
					tempWeight=Double.parseDouble(e.getChildTextTrim("Weight"));				
		}
	}
	private void setTempFromAttributeElement(int type,Element e, Element root){
    	if (e.getAttributeValue("class")!=null)
    		setTempFromClass(type, e.getAttributeValue("class"), root);    		
		switch(type){
		case FUNCTION:
		case PREDICATE:
				if (e.getAttributeValue("agg")!=null)
					tempAggregator=toCode(e.getAttributeValue("agg"));
				if (e.getAttributeValue("useParam")!=null)
					tempUseParam=toCode(e.getAttributeValue("useParam"));
				if (e.getAttributeValue("intW")!=null)
					tempIntW=Double.parseDouble(e.getAttributeValue("intW"));
				if (e.getAttributeValue("varW")!=null)
					tempVarW=Double.parseDouble(e.getAttributeValue("varW"));
		case CONSTANT:
				if (e.getAttributeValue("weight")!=null)
					tempWeight=Double.parseDouble(e.getAttributeValue("weight"));
		}
	}
	
    
    
    
    private void makeInitialDefauts(){
    	addSymbol("d_DefaultPred",PREDICATE,true,0,false);
    	addSymbol("d_DefaultFunc",FUNCTION,true,0,false);
    	addSymbol("d_DefaultConst",CONSTANT,true,0,false);
    	builtDefaultElements=true;
    }
    
//    private void addSymbol(String name,int type){
//    	if (getInd(name,type)==0)
//    		addSymbol(name,type,!builtDefaultElements,getNameList(type).size(),false);
//    }
    private void addSymbolfromTemp(String name,int type){
    	if (getInd(name,type)==0)
    		addSymbol(name,type,!builtDefaultElements,getNameList(type).size(),true);
    }
    
    	
	private void addSymbol(String name,int type,boolean useInitialDefaults, int ind,boolean useTemp){
		if (!useTemp){
			if (useInitialDefaults)
				this.setTempFromInitialDefaut(type);
			else
				this.setTempFromDefaut(type);
		}
		switch(type){
		case FUNCTION:
		case PREDICATE:			
			getAggregator(type).add(ind,tempAggregator);
			getUseParam(type).add(ind,tempUseParam);
			getIntVarWeight(type, INTEGER).add(ind,tempIntW);
			getIntVarWeight(type, VARIABLE).add(ind,tempVarW);
		case CONSTANT:
			getWeightList(type).add(ind,tempWeight);
			getNameList(type).add(ind,name);
			break;
		}
	}
    
	
    private void setFromTemp(String name,int type){
    	int ind=this.getInd(name, type);
    	if (ind==0){
    		addSymbolfromTemp(name, type);
    		ind=getInd(name, type);    		
    	}
    	setFromTemp(ind,type);
    }
    
    private void setFromTemp(int ind,int type){
        switch(type){
		case FUNCTION:
		case PREDICATE:			
			getAggregator(type).set(ind, tempAggregator);
			getUseParam(type).set(ind, tempUseParam);
			getIntVarWeight(type, INTEGER).set(ind, tempIntW);
			getIntVarWeight(type, VARIABLE).set(ind, tempVarW);
		case CONSTANT:
			getWeightList(type).set(ind, tempWeight);
			break;
		case INTEGER:
			defaultIntWeight=tempWeight;
			break;
		case VARIABLE:
			defaultVarWeight=tempWeight;
		}
	}
	
	private void setTempFromDefaut(int type){
		switch(type){
		case FUNCTION:
		case PREDICATE:
			tempAggregator=getAggregator(type).get(0);
			tempUseParam=getUseParam(type).get(0);
			tempIntW=getIntVarWeight(type, INTEGER).get(0);
			tempVarW=getIntVarWeight(type, VARIABLE).get(0);
		case CONSTANT:
			tempWeight=getWeightList(type).get(0);
			break;
		case INTEGER:
			tempWeight=defaultIntWeight;
			break;
		case VARIABLE:
			tempWeight=defaultVarWeight;
		}
	}
	private void setTempFromInitialDefaut(int type){
		switch(type){
		case FUNCTION:
			tempAggregator=this.defaultFunctionAggregator;
			tempUseParam=this.defaultFunctionUseParam;
			tempIntW=this.defaultFunctionIntWeight;
			tempVarW=this.defaultFunctionVarWeight;
			tempWeight=this.defaultFunctionWeight;
			break;
		case PREDICATE:
			tempAggregator=this.defaultPredicateAggregator;
			tempUseParam=this.defaultPredicateUseParam;
			tempIntW=this.defaultPredicateIntWeight;
			tempVarW=this.defaultPredicateVarWeight;
			tempWeight=this.defaultFunctionWeight;
			break;
		case CONSTANT:
			tempWeight=this.defaultConstantWeight;
			break;
		case INTEGER:
			tempWeight=defaultIntWeight;
			break;
		case VARIABLE:
			tempWeight=defaultVarWeight;
		}
	}
	

	public static void main(String[] args) throws JDOMException, IOException, ParseException{
	//String filenameNoExt="test1storderDist";
	String filenameNoExt="t08r1-dist1-6ag";
	
    WeightTable w=new WeightTable(filenameNoExt);
    
    System.out.println(w);
    IndepClause[] c=new IndepClause[5];
    c[0]=new IndepClause("[-h(3,X,X)]");
    c[1]=new IndepClause("[-h(3,6,X),-h(2,6,X)]");
    c[2]=new IndepClause("[-h(2,6,3),-h(1,3,3)]");
    c[3]=new IndepClause("[-p_a(3,6)]");
    c[4]=new IndepClause("[-p_a(3,6),-h(1,3,3)]");
    
    for (int i=0;i<5;i++) {
    	double weight=w.computeWeight(c[i]);
    	System.out.println("c"+1+" : "+c[i]+" - weight: "+weight);        
    }
    //int c=w.compare(c[i],c[j]);
    //System.out.println("compare(c1,c2) : "+c);
    //toTest
    // (getSymbol, getArity)
    // getFullWeight(term,parent,env)
    // getOwnWeight(symbol,type,parentSymbol,parentType)
    //computeWeight(term / literal / clause / IndepClause,env)

    //compate(IndepClause, IndepClause)
    
    
	}
	
	protected List <String> predicatesName=new ArrayList<String>();
	protected List <String> constantsName=new ArrayList<String>();
	protected List <String> functionsName=new ArrayList<String>();
	protected List <Double> predicatesWeight=new ArrayList<Double>();
	protected List <Double> constantsWeight=new ArrayList<Double>();
	protected List <Double> functionsWeight=new ArrayList<Double>();
	//Parameters aggregations
	protected List <Integer> predicatesAggregator=new ArrayList<Integer>();
	protected List <Integer> functionsAggregator=new ArrayList<Integer>();
	protected List <Integer> predicatesUseParam=new ArrayList<Integer>();
	protected List <Integer> functionsUseParam=new ArrayList<Integer>();
	protected List <Double> predicatesVarWeight=new ArrayList<Double>();
	protected List <Double> functionsVarWeight=new ArrayList<Double>();
	protected List <Double> predicatesIntWeight=new ArrayList<Double>();
	protected List <Double> functionsIntWeight=new ArrayList<Double>();
	// global param
	protected int clauseAggregator=AGG_PRODUCT;
	// global defauts (use for intialization and construction)
		//maintained
	protected double defaultVarWeight=1;
	protected double defaultIntWeight=1;
	protected boolean builtDefaultElements=false;
		//not maintained after init (use the first element of corresponding list)
	protected double defaultPredicateWeight=1;
	protected double defaultConstantWeight=1;
	protected double defaultFunctionWeight=0.9;
	protected int defaultPredicateAggregator=AGG_MIN;
	protected int defaultFunctionAggregator=AGG_PRODUCT;
	protected int defaultPredicateUseParam=USE_ALLPARAM;
	protected int defaultFunctionUseParam=USE_ALLPARAM;
	protected double defaultPredicateVarWeight=defaultVarWeight;
	protected double defaultPredicateIntWeight=defaultIntWeight;;
	protected double defaultFunctionVarWeight=defaultVarWeight;;
	protected double defaultFunctionIntWeight=defaultIntWeight;;
	
	
	//temporary tools variables
	private double tempWeight;
	private int tempAggregator;
	private int tempUseParam;
	private double tempIntW;
	private double tempVarW;
	
	//constants
	public static final int AGG_PRODUCT=0;
	public static final int AGG_MAX=1;
	public static final int AGG_MIN=2;
	public static final int AGG_AVERAGE=3;
	public static final int USE_ALLPARAM=0;
	public static final int USE_NONVAR=1;
	public static final int USE_ONLYVAR=2;
	public static final String S_AGG_PRODUCT="PROD";
	public static final String S_AGG_MAX="MAX";
	public static final String S_AGG_MIN="MIN";
	public static final String S_AGG_AVERAGE="AVG";
	public static final String S_USE_ALLPARAM="ALL";
	public static final String S_USE_NONVAR="NVAR";
	public static final String S_USE_ONLYVAR="VAR";
	
	
	
}
