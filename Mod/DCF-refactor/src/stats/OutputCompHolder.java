package stats;

import genLib.io.LoaderTool;
import genLib.io.Saver;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class OutputCompHolder implements Saver{
	HashMap<String, Integer> mapRes = new HashMap<String, Integer>();
	
	public OutputCompHolder() {
		super();
		// TODO Auto-generated constructor stub
	}
	public void addEntry(String s, int i ){
		mapRes.put(s,i);
	}
	public void save(String filename, boolean replace) throws Exception{
		System.out.println(filename);
		LoaderTool.save(filename, ".csv", this, replace);
	}

	
	public void save(PrintStream p){
		Iterator<Entry<String, Integer>> it = mapRes.entrySet().iterator();
		String header ="";
		String values = "";
		String separator = ";";
		
	    while (it.hasNext()) {

	        Map.Entry<String, Integer> pair = (Map.Entry)it.next();
	   //     p.println(pair.getKey() + " = " + pair.getValue());
	        header += pair.getKey()+separator;
	        values += Integer.toString(pair.getValue())+separator;
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    
	    //removing last comma
		header = header.substring(0,header.length()-1);
	    values = values.substring(0,values.length()-1);

		System.out.println(header);
		System.out.println(values);
	    p.println(header);
		p.println(values);
	}
}