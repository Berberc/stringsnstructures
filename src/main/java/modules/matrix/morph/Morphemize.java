package modules.matrix.morph;

import java.util.ArrayList;
import java.util.BitSet;
import common.logicBits.LogOp;

import modules.matrix.MatrixBitWiseOperationTreeNodeElement;

public class Morphemize {
	
/*
 * for all elements of list (TreeNodeElements) do in morphemize:
 * generate for each element list of contained (other) elements of treeNodeElementlist.
 * This means: it is looked for elements of partial distributin, e.g. regier+e, regier+st,
 * regier+ung. Partial distribution is found in telefonier+e, telefonier+st, but there is no
 *  *telefonier+ung. A futher, but disjunct distribution is found in zeit+ung.
 *  Thus, regier is quite similar to two disjunct classes.
 *  First, in morphemize, all contained (but not necessarily disjunct) classes are found.
 *  As a side effect, the greatest contained class is identified (e.g. telefonier).
 *  In a second step (in analyzeContainingList) the best but disjunct remaining classes are found
 *  and gathered (in the resulting containing list, by marking its elements by the undone flag).
 *  It should be remarked that a contained class itself may containing further contained classes.
 *  These contained classes may be identified prior or later. The resulting lists are ransfered to
 *  the elements for neighborhood tree building. Thus, regier is quite similar to telefonier on the
 *  one hand and to zeit(ung) on the other.
 *
 * 
 * 
 * 
 */
	
	private ContainingElement contains(MatrixBitWiseOperationTreeNodeElement el1, 
			MatrixBitWiseOperationTreeNodeElement el2){
			BitSet res=LogOp.AND(el1.contextBitSet, el2.contextBitSet);
			// el2 contained in el1
			if (res.cardinality()==el2.contextBitSet.cardinality()){
				
				return new ContainingElement(el1,el2,res);
			}
		return null;
	}
	
	private ArrayList<ContainingElement> analyzeContainingList(ArrayList<ContainingElement> containingList,
			BitSet bestSet, MatrixBitWiseOperationTreeNodeElement containingElement) {
		
		// all subclasses found?
		while (containingElement.contextBitSet.cardinality()> bestSet.cardinality()) {
			int best =bestSet.cardinality();int best_i=-1;
			BitSet localBestSet=null;
			// search best
			for (int i=0;i<containingList.size();i++){
				ContainingElement contained=containingList.get(i);
				// only undone elements
				if (contained.unDone){
					// ORing of elements
					BitSet res=LogOp.OR(bestSet,contained.containedBitSet );
					// does result grow? (i.e. does it cover new elements)
					if(res.cardinality()>best){
						// save best(better) result
						best=res.cardinality();
						best_i=i;
						localBestSet=res;
					}
				}
			} //for (int =0...
			// no further success
			if (best_i==-1)return null; else {
				bestSet=LogOp.OR(bestSet, localBestSet);
				// mark elements in list as worked
				containingList.get(best_i).unDone=false;
			}// if(best_i
			
			
		}// while
		// reduce containingList to elements which are done; remove undone		
		int i=0;
		while(i<containingList.size()){
			ContainingElement contained=containingList.get(i);
			if (contained.unDone){
				containingList.remove(i);
			} else i++;
			
		}
		// result is given back as (shortened)containigList. This list is used for 
		// neighborhood tree building, by using (indirect)references 
		// to entries in NamedFieldMatrix
		//
		return containingList;
	}
	
	public void morphemize(ArrayList<MatrixBitWiseOperationTreeNodeElement> list){
		
			// check all elements for subclasses (outer for loop for containing)
			for (int i=0;i<list.size();i++){
				
				int max=0;int indexOfBest=-1;
				ArrayList<ContainingElement> containingList=
				new ArrayList<ContainingElement>();	
				MatrixBitWiseOperationTreeNodeElement element_i=list.get(i);
				// 
				// inner for loop for contained
				for (int j=0;j<list.size();j++){
					if (i!=j){
						MatrixBitWiseOperationTreeNodeElement element_j=list.get(j);
						// create list of containing
						ContainingElement contEl=contains(element_i,element_j);
						if (contEl !=null ){
							// gather in list
							containingList.add(contEl);
							// look for best containing (side effect of gathering,
							// is used as base for analyzeContaining
							if (contEl.containedBitSet.cardinality()>max) {
								max=contEl.containedBitSet.cardinality();
								indexOfBest=containingList.size()-1;;
							}
						}// if (contEl..
					}// if (i!=j)
					
				}// for(int j..
				if (indexOfBest>=0)
					{ 
						// save best set
						BitSet bestSet=containingList.get(indexOfBest).containedBitSet;
						// delete best element in list which is already known
						containingList.get(indexOfBest).unDone=false;
						// analyze rest  TODO save list
						analyzeContainingList(containingList,bestSet,element_i);
					
				}//if (maxIndex>=0)
				
				
			}// for (int i...
	 }
}