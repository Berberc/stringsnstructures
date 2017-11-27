package modules.matrix;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;

import models.NamedFieldMatrix;
import modules.matrix.morph.ContainingElement;


public class MatrixBitWiseOperationTreeNodeElement {
	
	MatrixBitWiseOperationTreeNodeElement child1, child2,mother,root;
	public BitSet contextBitSet,or,and;
	public int fromNamedFieldMatrixRow;
	public ArrayList<ContainingElement> containingList;
	public boolean deselect=false;
	private static  char[] code=new char[1000];
	
	//cstr
	public MatrixBitWiseOperationTreeNodeElement(BitSet context,
			int row,
			MatrixBitWiseOperationTreeNodeElement child1,
			MatrixBitWiseOperationTreeNodeElement child2){
		
		// 17-10-11 this.contextBitSet=context;
		this.contextBitSet=(BitSet)context.clone();
		// 'or' and 'and' are needed in neighborhood tree, for to check similarity
		// of contained nodes. Similarity is needed for cuts in tree.
		// Similarity is a ratio of ORing and ANDing the contextbitsets of 
		// joined nodes
		
		/* 17-10-11
		this.or=null;
		this.and=null;
		*/		
		this.or= (BitSet)context.clone();
		this.and= (BitSet)context.clone();
		
		
		this.fromNamedFieldMatrixRow=row;
		
		this.child1=child1;
		this.child2=child2;	
		this.mother=null;
		this.root=this; //root maybe root of partial tree, 
		// s. use in MatrixDynamicMorphClustering class
		// first it is set to itself; thus there is no need to check whether root is null
		// e.g. in MatrixDynamicMorphClustering.searchBestPairForTree_1.
		this.containingList=null;//reference to list of (morphologically) contained
								// classes
	}// cstr
	
	
	
	// this type of treewalker is too specialized. It might be replaced by
	// a treewalker with an interface typed parameter or (better) with an interface for
	// a listener which listens to events generated by an entryAction or an exitAction
	
	// reset root for every treenode of tree
	
	public void walkForRootSetting(MatrixBitWiseOperationTreeNodeElement treeNode, 
			MatrixBitWiseOperationTreeNodeElement nodeAsRoot,
			PrintWriter p){
		
		if (treeNode !=null){
			// only test, only for terminals, all other node are labeld by 0
			//if (treeNode.fromNamedFieldMatrixRow!=0)
			//p.println("walkForRootSetting: "+treeNode.fromNamedFieldMatrixRow);
			//" nodeAsRoot: "+nodeAsRoot.fromNamedFieldMatrixRow);
			//entryAction
			//---------------
			// too specialized !
			// set root of partial tree (i.e. node)
			treeNode.root=nodeAsRoot;
			// depth first
			walkForRootSetting(treeNode.child1,nodeAsRoot,p);
			// bredth second
			walkForRootSetting(treeNode.child2,nodeAsRoot,p);
			// exitAction
		}
		 
	}// walkForRootSetting 
	
	
	public static void walkForCodeGeneration(MatrixBitWiseOperationTreeNodeElement treeNode,  
			char codeChar, int codeIndex,
			ArrayList<MatrixBitwiseOperationTreeNodeCodeGenerationElement> list,
			PrintWriter p,NamedFieldMatrix namedFieldMatrix){
		
		code[codeIndex]=codeChar;
		StringBuffer codeStr=new StringBuffer();
		MatrixBitwiseOperationTreeNodeCodeGenerationElement e=null;
		
		p.print(" Code: ");
		for (int i=0;i<=codeIndex;i++){
			p.print(code[i]);
			codeStr.append(code[i]);
		}
		String out="";
		if (treeNode.child1==null)out=out+"  Terminalknoten ";
		if (treeNode.deselect)out=out+  " deselect ";
		else {
			e=new MatrixBitwiseOperationTreeNodeCodeGenerationElement();
			e.code=codeStr.toString();
			e.nodeElement=treeNode;
			list.add(e);
			
		}
		p.println();
		// an existing root is supposed
		
		
		if (treeNode.child1 != null){
			//entryAction
			//---------------
			// too specialized, see above walkForRootSetting!
			
			// depth first
			walkForCodeGeneration(treeNode.child1,'0',codeIndex+1,list,p,namedFieldMatrix);
			// bredth second
			
			walkForCodeGeneration(treeNode.child2,'1',codeIndex+1,list,p,namedFieldMatrix);
			
			// exitAction
			//
		}
		else {
			//terminal
			if(treeNode.child1==null)
				out=out+namedFieldMatrix.getRowName(treeNode.fromNamedFieldMatrixRow);
			
		};
		p.println(out);
	
	}
	

}
