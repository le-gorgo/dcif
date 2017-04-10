package org.nabelab.solar.pfield;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.nabelab.solar.Env;
import org.nabelab.solar.Literal;
import org.nabelab.solar.Options;
import org.nabelab.solar.PLiteral;
import org.nabelab.solar.TermTypes;
import org.nabelab.solar.indexing.DiscTree;
import org.nabelab.solar.parser.ParseException;
import org.nabelab.solar.parser.Parser;

import com.sun.org.apache.xpath.internal.operations.Lt;

public class PFCardConstraint {

	public PFCardConstraint() {
		group=new ArrayList<PLiteral>();
	}
	public PFCardConstraint(int maxLength) {
		maxLengthGroup = maxLength;
		group=new ArrayList<PLiteral>();
	}
	public PFCardConstraint(List<PLiteral> group, int maxLength) {
		maxLengthGroup = maxLength;
		this.group=group;
	}
	
	public Literal contains(Env env, Options opt, Literal l){
		for (PLiteral pl : group){
			int i = 0;
			while(true){
				if(i <= pl.getTerm().getDepth()+1){
					if(pl.getTerm().getType(i) == l.getTerm().getType(i) &&  pl.getTerm().getName(i) == l.getTerm().getName(i))						
						i++;
					else 						
						if (pl.getTerm().getType(i) == TermTypes.VARIABLE)
							return l;
						else 
							break;
				}
				else 
					break;

			}
		}
		return null;
	}
	
	public List<PLiteral> getGroup() {
		return group;
	}
	public void add(PLiteral literal) {
		group.add(literal);
	}
	public void addAll(List<PLiteral> literals) {
		group.addAll(literals);
	}
	public int getMaxLengthGroup() {
		return maxLengthGroup;
	}
	public void setMaxLengthGroup(int maxLengthGroup) {
		this.maxLengthGroup = maxLengthGroup;
	}
	public void contains() {
		
	}
	private List<PLiteral> group;
	private int maxLengthGroup;
	
	public static PFCardConstraint parse(Env env, Options opt, String pfCard) throws ParseException {
		return new Parser(env, opt).pfCard(new BufferedReader(new StringReader(pfCard)));
	}
}
