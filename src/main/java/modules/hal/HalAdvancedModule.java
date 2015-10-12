package modules.hal;

import java.io.BufferedReader;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import modules.CharPipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;
import common.parallelization.CallbackReceiver;

public class HalAdvancedModule extends ModuleImpl {

	// Define property keys (every setting has to have a unique key to associate
	// it with)
	public static final String PROPERTYKEY_WINDOWSIZE = "window size";
	public static final String PROPERTYKEY_FIELDSEPARATOR = "field separator";
	public static final String PROPERTYKEY_COOCCURRSEPARATOR = "cooccurrency separator";

	// Define I/O IDs (must be unique for every input or output)
	private final String inputTextId = "text input";
	private final String outputCsvId = "csv output";

	// Local variables
	private int windowSize;
	private String fieldSeparator;
	private String cooccurrencySeparator;

	public HalAdvancedModule(CallbackReceiver callbackReceiver,
			Properties properties) throws Exception {

		// Call parent constructor
		super(callbackReceiver, properties);

		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_WINDOWSIZE,
				"Size of the sliding window (default: 5)");
		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_FIELDSEPARATOR,
				"Separator for the CSV fields");
		// Add property descriptions (obligatory for every property!)
		this.getPropertyDescriptions().put(PROPERTYKEY_COOCCURRSEPARATOR,
				"Separator for the cooccurrency position numbers");

		// Add property defaults (_should_ be provided for every property)
		this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME,
				"HAL advanced module"); // Property key for module name is
										// defined in parent class
		this.getPropertyDefaultValues().put(PROPERTYKEY_WINDOWSIZE, "5");
		this.getPropertyDefaultValues().put(PROPERTYKEY_FIELDSEPARATOR, "\t\t");
		this.getPropertyDefaultValues().put(PROPERTYKEY_COOCCURRSEPARATOR, ",");

		// Define I/O
		/*
		 * I/O is structured into separate ports (input~/output~). Every port
		 * can support a range of pipe types (currently byte or character
		 * pipes). Output ports can provide data to multiple pipe instances at
		 * once, input ports can in contrast only obtain data from one pipe
		 * instance.
		 */
		InputPort textInputPort = new InputPort(inputTextId,
				"Plain text character input, one segment per line.", this);
		textInputPort.addSupportedPipe(CharPipe.class);
		OutputPort outputPort = new OutputPort(outputCsvId,
				"CSV tabular output.", this);
		outputPort.addSupportedPipe(CharPipe.class);

		// Add I/O ports to instance (don't forget...)
		super.addInputPort(textInputPort);
		super.addOutputPort(outputPort);

	}

	@Override
	public boolean process() throws Exception {

		// We need a buffered reader for the boundaries input in order to read
		// line by line
		BufferedReader reader = new BufferedReader(this.getInputPorts()
				.get(inputTextId).getInputReader());

		// Define comparator
		Comparator<String> alphabetOrderComparator = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		};
		// Define map for results
		TreeMap<String, TreeMap<String, Integer[]>> sequenceMap = new TreeMap<String, TreeMap<String, Integer[]>>(
				alphabetOrderComparator);

		// Queue that acts as input window (plus one element to operate on in
		// each iteration)
		LinkedBlockingQueue<String> sequenceQueue = new LinkedBlockingQueue<String>(
				this.windowSize + 1);

		// Read input
		while (reader.ready() || !sequenceQueue.isEmpty()) {

			if (reader.ready()){
				// Read segment
				String segment = reader.readLine();
				
				System.out.println("Lese Segment: "+segment);
				
				// If there is not yet a map entry for that segment, construct one
				if (!sequenceMap.containsKey(segment)) {
					sequenceMap.put(segment, new TreeMap<String, Integer[]>(alphabetOrderComparator));
				}

				// Add segment to queue
				sequenceQueue.put(segment);

				// If the queue is not yet full, skip the rest
				if (sequenceQueue.size() < this.windowSize + 1)
					continue;
			}

			// Split the first sequence from the queue
			String firstSegment = sequenceQueue.poll();
			
			System.out.println("Bearbeite Segment: "+firstSegment);

			// Determine line map for that segment
			TreeMap<String, Integer[]> lineMap = sequenceMap.get(firstSegment);

			// Iterate over the remaining sequences within the queue
			Iterator<String> queueIterator = sequenceQueue.iterator();
			int index = 0; // We will need to determine the index of each queue
							// element
			while (queueIterator.hasNext()) {
				String queuedSegment = queueIterator.next();
				
				System.out.println("Segment in Warteschlange: "+queuedSegment);
				
				// Determine whether the two current segments (first in queue
				// and the one the iterator is pointing to) already have an
				// integer array associated.
				Integer[] positionArray = lineMap.get(queuedSegment);
				// Else, one will be instantiated
				if (positionArray == null) {
					positionArray = new Integer[this.windowSize];
					for (int i=0; i<positionArray.length; i++){
						positionArray[i] = new Integer(0);
					}
					lineMap.put(queuedSegment, positionArray);
				}
				
				System.out.println("War: "+positionArray[index]);

				// Update the integer array in the respective position
				Integer cooccurrenceCounter = positionArray[index];
				if (cooccurrenceCounter == null) {
					cooccurrenceCounter = new Integer(1);
					positionArray[index] = cooccurrenceCounter;
				} else {
					positionArray[index] += 1;
				}
				System.out.println("Ist: "+positionArray[index]);

				// Increase index
				index++;
			}
		}

		// reader is empty

		// Output results (first head of CSV)
		this.getOutputPorts().get(outputCsvId).outputToAllCharPipes("SEGMENT" + this.fieldSeparator);
		Iterator<String> lineKeys = sequenceMap.keySet().iterator();
		while (lineKeys.hasNext()) {
			String lineKey = lineKeys.next();
			this.getOutputPorts().get(outputCsvId)
					.outputToAllCharPipes(lineKey + this.fieldSeparator);
		}
		this.getOutputPorts().get(outputCsvId).outputToAllCharPipes("\n");
		// Output results (data lines)
		lineKeys = sequenceMap.keySet().iterator();
		while (lineKeys.hasNext()) {
			String lineKey = lineKeys.next();
			this.getOutputPorts().get(outputCsvId)
			.outputToAllCharPipes(lineKey + this.fieldSeparator);
			Iterator<String> rowKeys = sequenceMap.keySet().iterator();
			while (rowKeys.hasNext()) {
				String rowKey = rowKeys.next();
				Integer[] cooccurrencePositionArray = sequenceMap.get(lineKey)
						.get(rowKey);
				// Construct array if it should be nonexistent
				if (cooccurrencePositionArray == null) {
					cooccurrencePositionArray = new Integer[this.windowSize];
					for (int i=0; i<cooccurrencePositionArray.length; i++){
						cooccurrencePositionArray[i] = new Integer(0);
					}
				}

				for (int i = 0; i < cooccurrencePositionArray.length; i++) {
					this.getOutputPorts()
								.get(outputCsvId)
								.outputToAllCharPipes(
										cooccurrencePositionArray[i].toString());
					if (i + 1 < cooccurrencePositionArray.length)
						this.getOutputPorts().get(outputCsvId)
								.outputToAllCharPipes(this.cooccurrencySeparator);
				}
				this.getOutputPorts().get(outputCsvId)
						.outputToAllCharPipes(this.fieldSeparator);
			}
			this.getOutputPorts().get(outputCsvId).outputToAllCharPipes("\n");
		}

		// Close outputs (important!)
		this.closeAllOutputs();

		// Done
		return true;
	}

	@Override
	public void applyProperties() throws Exception {

		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();

		// Apply own properties
		try {
			this.windowSize = Integer.parseInt(this.getProperties().getProperty(
					PROPERTYKEY_WINDOWSIZE));
		} catch (Exception e) {
			this.windowSize = 5;
		}
		
		this.fieldSeparator = this.getProperties().getProperty(PROPERTYKEY_FIELDSEPARATOR);
		this.cooccurrencySeparator = this.getProperties().getProperty(PROPERTYKEY_COOCCURRSEPARATOR);

		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}

}