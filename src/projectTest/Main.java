package projectTest;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import projectTest.classifier.Evaluations;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import edu.featgen.Experiment;
import edu.featgen.standard.util.Logger;
import edu.featgen.standard.util.WikiDBConnector;
import edu.featgen.standard.util.XMLConfigurationLoader;
import edu.featget.adapters.weka.WekaSparseAdapter;
import edu.wiki.util.Tuple;
 
public class Main {
	protected static int N_TOPICS = 15;
	
	public static boolean testHasPositive(Instances instances) {
		Attribute toTest = instances.attribute(WekaSparseAdapter.TARGET_ATTR_NAME);
		Enumeration<Instance> eInst = instances.enumerateInstances();
		while(eInst.hasMoreElements()) {
			Instance instance = eInst.nextElement();
			if (instance.value(toTest) == 1.0) {
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		System.out.println("Preloading db tables...");
		//TermQueryOptimizer.getInstance().loadAll();
		//IdfQueryOptimizer.getInstance().loadAll();
		
		Logger.writeToResults("Starting test " + 
				new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime()));
		Logger.writeToResults("---------------------------------");
		Logger.writeToReport("------------ new report ---------");
		WikiDBConnector.initialize("localhost", "enwiki-20150403", "root", "rockon123");
		
		Experiment experiment = XMLConfigurationLoader.buildFromFile(new File("featgen-config.xml"));
		experiment.processDocuments();
		
		// get list of topics with document count per topic
		List<Entry<String, Long>> topicCounts = experiment.getDocuments().stream()
				// map to target features of each document
				.flatMap((doc)->doc.getClasses().stream())
				// count documents per each target feature
				.collect(Collectors.groupingBy(String::toString, Collectors.counting()))
				// sort features by doc count
				.entrySet().stream().sorted((e1,e2)->e2.getValue().compareTo(e1.getValue()))
				.collect(Collectors.toList());

		// Original categories from Gabrielovich at el.
		String[][] cats =
		{
			{"B-Lymphocytes", "Metabolism, Inborn Errors", "Creatinine", "Hypersensitivity", "Bone Diseases, Metabolic", "Fungi", "New England", "Biliary Tract", "Forecasting", "Radiation"},
			{"Thymus Gland", "Insurance", "Historical Geographic Locations", "Leukocytes", "Hemodynamics", "Depression", "Clinical Competence", "Anti-Inflammatory Agents, Non-Steroidal", "Cytophotometry", "Hydroxy Acids"},
			{"Endothelium, Vascular", "Contraceptives, Oral, Hormonal", "Acquired Immunode¯ciency Syndrome", "Gram-Positive Bacteria", "Diarrhea", "Embolism and Thrombosis", "Health Behavior", "Molecular Probes", "Bone Diseases, Developmental", "Referral and Consultation"},
			{"Antineoplastic and Immunosuppressive Agents", "Receptors, Antigen, T-Cell", "Government", "Arthritis, Rheumatoid", "Animal Structures", "Bandages", "Italy", "Investigative Techniques", "Physical Sciences", "Anthropology"},
			{"HTLV-BLV Infections", "Hemoglobinopathies", "Vulvar Diseases", "Polycyclic Hydrocarbons, Aromatic", "Age Factors", "Philosophy, Medical", "Antigens, CD4", "Computing Methodologies", "Islets of Langerhans", "Regeneration"}
		};

		// Test for top topics
		Logger.writeToResults("topic\tsize\tprecsnn\trecall\tF\ttrainF");
		for(int i = 100; i < 115; i++) {
			Entry<String,Long> e = topicCounts.get(i);
			
			Tuple<Evaluations,Evaluations> evals = experiment.testForTopic(e.getKey());
			Evaluations testEvals = evals.x;
			Evaluations trainEvals = evals.y;
			
			Logger.writeToReport("topic: " + e.getKey() + "\t" +
					e.getValue() + "\t");
			Logger.writeToResults(e.getKey() + "\t" +
					e.getValue() + "\t" +
					String.format("%.3f", testEvals.precision()) + "\t" +
					String.format("%.3f", testEvals.recall()) + "\t" +
					String.format("%.3f", testEvals.fMeasure()) + "\t" +
					String.format("%.3f", trainEvals.precision()) + "\t" +
					String.format("%.3f", trainEvals.recall()) + "\t" +
					String.format("%.3f", trainEvals.fMeasure()) + "\t");
		}
		Logger.writeToResults("---------------------------------");
		Logger.writeToResults("Finished test " + 
				new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime()));
		Logger.writeToResults("---------------------------------");
	}
}