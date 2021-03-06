package modules.format_conversion.seqNewickExporter;

import java.util.HashMap;

/**
 * Helper class for Newick Exporter module(s)
 * 
 * @author Christopher Kraus
 *
 */


public class SeqNewickNode {
	
	//variables:
	private String nodeValue; //string saved in the node
	private int nodeCounter; //Zaehler value
	HashMap<String, SeqNewickNode> propNode;
	//end variables
	
	//constructors:
	public SeqNewickNode(String value, int counter) {
		nodeValue = value;
		nodeCounter = counter;
		propNode = new HashMap<String, SeqNewickNode>();
	}
	
	public SeqNewickNode(String value, int counter, SeqNewickNode node) {
		nodeValue = value;
		nodeCounter = counter;
		propNode = new HashMap<String, SeqNewickNode>();
		propNode.put(value, node);
	}
	//end constructors
	
	//methods:
	//setters:
	public void setValue (String val) {
		nodeValue = val;
	}
	
	public void concatValue (String val) {
		nodeValue += val;
	}
	
	public void setCount (int count) {
		nodeCounter = count;
	}
	
	public void addCount (int count) {
		nodeCounter += count;
	}
	
	public void addNode (String value, SeqNewickNode node) {
		propNode.put(value, node);
	}
	//end setters
	
	//getters:
	public String getValue ()  {
		return nodeValue;
	}
	
	public int getCounter ()  {
		return nodeCounter;
	}
	
	public HashMap<String, SeqNewickNode> getNodeHash () {
		return propNode;
	}
	//end getters
	//end methods
}
