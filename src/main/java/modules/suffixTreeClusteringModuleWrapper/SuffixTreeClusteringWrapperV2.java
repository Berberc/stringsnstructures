package modules.suffixTreeClusteringModuleWrapper;

//java standard imports:
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

//modularization imports:
import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;
import common.parallelization.CallbackReceiver;

//clustering specific imports:
import modules.suffixTreeClustering.clustering.flat.FlatCluster;
import modules.suffixTreeClustering.clustering.flat.FlatClusterer;
import modules.suffixTreeClustering.clustering.hierarchical.HierarchicalCluster;
import modules.suffixTreeClustering.clustering.hierarchical.HierarchicalClusterer;
import modules.suffixTreeClustering.clustering.neighborjoin.NeighborJoining;
import modules.suffixTreeClustering.data.Type;
import modules.suffixTreeClustering.st_interface.SuffixTreeInfo;

//JSON gson imports
import com.google.gson.Gson;

/**
 * This is a wrapper to modularize the vectorization process. 
 * @author Christopher Kraus
 * parts of this code are refactored from neumannm
 */

//TODO: ATTENTION THIS MODULE IS STILL EXPERIMENTAL!

public class SuffixTreeClusteringWrapperV2 extends ModuleImpl {
	
	// property keys:
	
	// this property saves the selected form of clustering
	public static final String PROPERTYKEY_CLUST = "clustering type";
	
	// this property saves the name of the used corpus
	public static final String PROPERTYKEY_CORPNAME = "corpus/text name";
	
	// variables:
	
	// variable for saving the corpus 
	private SuffixTreeInfo corpus;
	
	// variable which holds the types
	private List<Type> types;
	
	// the type of clustering which should be applied
	private String clusterType;
	
	// the name of the corpus
	private String corpusName;
	
	//the result of the clustering
	private String clustResult;
	
	// definitions of I/O variables
	private final String INPUTIDTREEVEC = "JSON vector";
	private final String OUTPUTID = "output";
	
	// end variables
	
	// constructors:
	
	public SuffixTreeClusteringWrapperV2 (CallbackReceiver callbackReceiver,
	Properties properties) throws Exception {
	
	// call parent constructor
	super(callbackReceiver, properties);
	
	// module description
	this.setDescription("This is a wrapper to modularize the clustering process.<br/>"
		+ "It takes one input:<br/>"
		+ "<ul type =\"disc\" ><li>vectors of the type \"SuffixTreeInfo\" in JSON format</li></ul>");
	
	// Add module category
	this.setCategory("Clustering");
	
	// property descriptions 
	this.getPropertyDescriptions().put(PROPERTYKEY_CLUST, "Three possible clustering types: \"NJ\" "
		+ "(Neighbor Joining), \"KM\" (flat k-means), \"HAC\" (hierachial agglomerative clustering)");
	
	this.getPropertyDescriptions().put(PROPERTYKEY_CORPNAME, "Insert corpus/text name");
	
	// property defaults
	this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "SuffixTreeClusteringWrapperV2"); 
	this.getPropertyDefaultValues().put(PROPERTYKEY_CLUST, "KM");
	this.getPropertyDefaultValues().put(PROPERTYKEY_CORPNAME, "myCorpus");
	
	// I/O definition
	InputPort inputPortVec = new InputPort(INPUTIDTREEVEC, "[JSON] Vector after \"SuffixTreeInfo\" in JSON format.", this);
	inputPortVec.addSupportedPipe(CharPipe.class);
	
	OutputPort outputPort = new OutputPort(OUTPUTID, "[text] Plain text character output.", this);
	outputPort.addSupportedPipe(CharPipe.class);
	
	// add I/O ports to instance
	super.addInputPort(inputPortVec);
	
	super.addOutputPort(outputPort);
	}
	
	// process()
	@Override
	public boolean process() throws Exception {
		
		Gson gson = new Gson();
		this.corpus = gson.fromJson(this.getInputPorts().get(this.INPUTIDTREEVEC).getInputReader(), SuffixTreeInfo.class);

		types = new ArrayList<Type>(corpus.getTypes());
		
		// selection of the clustering type
		switch (clusterType) {
		case "NJ":
			clusterNeighborJoin(types);
			break;
		case "KM":
			clusterFlat(types, corpusName);
			break;
		case "HAC":
			clusterHierarchical(types, corpusName);
			break;
		default:
			clusterFlat(types, corpusName);
			break;
		}
		
		// put the results into the output char pipe
		this.getOutputPorts().get(OUTPUTID).outputToAllCharPipes(this.clustResult);
		
		// close outputs
		this.closeAllOutputs();
		
		// success
		return true;
	}
	
	// applyProperties()
	@Override
	public void applyProperties() throws Exception {
		
		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();
		
		// Apply own properties
		this.corpusName = this.getProperties().getProperty(PROPERTYKEY_CORPNAME, this.getPropertyDefaultValues().get(PROPERTYKEY_CORPNAME));
		this.clusterType = this.getProperties().getProperty(PROPERTYKEY_CLUST, this.getPropertyDefaultValues().get(PROPERTYKEY_CLUST));
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}
	
	// methods:
	
	private void clusterFlat(List<Type> types, String name) {
		FlatClusterer f_analysis = new FlatClusterer(types);
		//LOGGER.info("Flaches Clustern von " + types.size() + " Types");
		List<FlatCluster> fClusters = f_analysis.analyse(3, 10);
		for (FlatCluster cluster : fClusters) {
			System.out.println(cluster.getMedoid().getString());
		}
		this.clustResult = f_analysis.toDot();
		// System.out.println(dot);
	}

	private void clusterHierarchical(List<Type> types,
			String name) {
		// usage of hierachial clustering 
		HierarchicalClusterer h_analysis = new HierarchicalClusterer(types);
		//LOGGER.info("Hierarchisches Clustern von " + types.size() + " Types");
		h_analysis.analyze();
		List<HierarchicalCluster> hClusters = h_analysis.getClusters();

		if (hClusters.size() == 1) {
			this.clustResult = h_analysis.toDot();
			
		} else
			System.err.println("Hierarchical Clustering didn't come up with exactly 1 cluster!");
	}
	
	private void clusterNeighborJoin(List<Type> types) {
		// usage of Neighbor Joining 
		NeighborJoining nj = new NeighborJoining(types);
		//LOGGER.info("Neighbor Joining Clustern von " + types.size() + " Types");
		nj.start();
		this.clustResult = nj.getTree();
	}
	
	// end methods
}