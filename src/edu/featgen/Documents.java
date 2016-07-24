package edu.featgen;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.DocumentSource;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.def.FeatureSelector;
import edu.featgen.def.FeatureSet;
import edu.featgen.standard.util.Logger;
import edu.featgen.standard.util.ProgressReporter;
import edu.wiki.util.Tuple;

public class Documents implements DocumentSet, Serializable{
	private static final long serialVersionUID = 5194816601338895853L;
	
	DocumentSource documentSource;
	Map<String, FeatureGenerator> featureGenerators = new HashMap<String,FeatureGenerator>();
	Set<FeatureSelector> featureSelectros = new HashSet<FeatureSelector>();

	Map<Long, Document> documents = new HashMap<Long, Document>();
	
	public void serialize(ObjectOutputStream oos){
		try {
			oos.writeObject(documents);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void deserialize(ObjectInputStream ois){
		try {
			documents = (Map<Long, Document>)ois.readObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}
	
	public Documents() {
	}

	public Documents(Documents orig, Set<Long> filter) {
		this.featureGenerators = orig.featureGenerators;
		this.featureSelectros = orig.featureSelectros;
		this.documentSource = orig.documentSource;
		documents = new HashMap<>();
		orig.documents.forEach((id,doc)->{
			if(filter == null || filter.contains(id)){
				documents.put(id, doc);
			}
		});
	}

	@Override
	public Stream<Document> stream() {
		return documents.values().stream();
	}

	@Override
	public Document document(long id) {
		return documents.get(id);
	}
	
	@Override
	public int size() {
		return documents.size();
	}

	public void addFeatureGenerator(FeatureGenerator featureGenerator) {
		this.featureGenerators.put(featureGenerator.getName(), featureGenerator);
	}
	public void addFeatureSelector(FeatureSelector featureSelector) {
		this.featureSelectros.add(featureSelector);
	}
	public void setDocumentSource(DocumentSource documentSource) {
		this.documentSource = documentSource;
	}

	public DocumentSource getDocumentSource() {
		return documentSource;
	}
	
	public void readDataSet() {
		documents.clear();		
		documentSource.reset();
		long id = 0;
		Document newDoc = documentSource.getNext();
		Logger.writeToConsole("Getting documents from: " + documentSource.getName());
		while(newDoc != null) {
			documents.put(id, newDoc);
			if (id % 1000 == 0) {
				Logger.writeToConsole("Read " + id + " documents");
			}
			newDoc = documentSource.getNext();
			id++;
		}
		Logger.writeToConsole("Finished reading " + (id+1) + " documents");
	}
	
	@Override
	public void processForTrain(String forClass, Map<String,Object> transientData) {
		List<FeatureGenerator> generatorOrder = resolveGenerationOrder();

		Logger.writeToConsole("Doing training feature generation" + 
				(forClass != null ? " for class " + forClass : ""));
		generatorOrder.forEach((generator) -> {
			
			if((forClass == null && generator.isRecalculateForEachClass()) ||
					(forClass != null && !generator.isRecalculateForEachClass())){
				return;
			}			
			Logger.writeToConsole("Running generator: " + generator.getName());

			generator.reset(this, forClass);
			Object d = generator.preProcess(this, transientData.get(generator.getName()));
			transientData.put(generator.getName(), d);

			ProgressReporter<Void> rep = new ProgressReporter<>(Duration.ofSeconds(5), 
					(n,t) -> Logger.writeToConsole("Done " + n + " documents in " + t.get(ChronoUnit.SECONDS)));

			this.forEach((docId, doc) -> {
				generator.processDocument(doc); 
				rep.countOne();
			});
			rep.finish();
		});
	}
	
	@Override
	public void processForTest(String forClass) {
		List<FeatureGenerator> generatorOrder = resolveGenerationOrder();

		Logger.writeToConsole("Doing testing feature generation" + 
				(forClass != null ? " for class " + forClass : ""));
		generatorOrder.forEach((generator) -> {
			
			if((forClass == null && generator.isRecalculateForEachClass()) ||
					(forClass != null && !generator.isRecalculateForEachClass())){
				return;
			}
			Logger.writeToConsole("Running generator: " + generator.getName());
			
			ProgressReporter<Void> rep = new ProgressReporter<>(Duration.ofSeconds(5), 
					(n,t) -> Logger.writeToConsole("Done " + n + " documents in " + t.get(ChronoUnit.SECONDS)));

			this.forEach((docId, doc) -> {
				generator.processDocument(doc); 
				rep.countOne();
			});
			rep.finish();
		});
	}
	
	private boolean isSatisfied(FeatureGenerator generator, 
			Set<FeatureGenerator> sorted, 
			DocumentSource source) {
		return generator.getRequiredInputs().stream()
			.filter((name) ->
				sorted.stream().filter((g)->g.getName().equals(name)).count() == 0 
				&& !source.getName().equals(name))
			.count() == 0;
	}
	
	private List<FeatureGenerator> resolveGenerationOrder() {
		ArrayList<FeatureGenerator> list = new ArrayList<FeatureGenerator>();
		HashSet<FeatureGenerator> left = new HashSet<FeatureGenerator>();
		HashSet<FeatureGenerator> sorted = new HashSet<FeatureGenerator>();
		
		left.addAll(featureGenerators.values());
		boolean gotSomething = true;
		
		while(!left.isEmpty() && gotSomething) {
			Iterator<FeatureGenerator> iter = left.iterator();
			gotSomething = false;
			while(iter.hasNext()) {
				FeatureGenerator generator = iter.next();
				if (isSatisfied(generator, sorted, documentSource)) {
					list.add(generator);
					sorted.add(generator);
					iter.remove();
					gotSomething = true;
				}
			}
		}
		if (!left.isEmpty()) {
			throw new CantResolveGenerationOrderException();
		}
		return list;
	}
	
	public Set<String> getFilteredFeatures(String targetClass){
		return featureSelectros.stream()
				.flatMap((selector)->selector.getFilteredFeatures(this, targetClass).stream())
				.collect(Collectors.toSet());
	}
	
	public Set<String> collectFeatureNamesForClassification(String targetClass) {
		System.out.println("Getting feature names (feature selection)");
		Set<String> filteredFeatures = getFilteredFeatures(targetClass);
		Predicate<? super FeatureSet> fsPredicate = (fs) -> fs.isUsedForClassification();
		Predicate<? super String> featurePredicate = (fname) -> !filteredFeatures.contains(fname); 
		return collectFeatureNames(fsPredicate, featurePredicate);
	}
	public Set<String> collectFeatureNamesForFeatureSets(Set<String> fsNames) {
		Predicate<? super FeatureSet> fsPredicate = (fs) -> fsNames.contains(fs.getName());
		Predicate<? super String> featurePredicate = (fname) -> true; 
		Set<String> s = collectFeatureNames(fsPredicate, featurePredicate);
		return s;
	}
	public Set<String> collectFeatureNames( 
			Predicate<? super FeatureSet> fsPredicate, 
			Predicate<? super String> featurePredicate) {
		return 	
				// docs streams
				stream()
				.flatMap((doc)->doc.featureSetStream()).
				filter(fsPredicate)
				// to feature names stream (including feature set prefix)
				.flatMap((fs)->fs.stream())
				.map((f)->f.name)
				.filter(featurePredicate)
				// collect
				.map((s)->(String)s).collect(Collectors.toSet());
	}
	
	public Set<String> collectFeatureSetNames() {
		return 	// docs streams
				stream()
				// to feature sets stream
				.flatMap((doc)->doc.featureSetStream())
				// to their names
				.map((fs)->fs.getName())
				// collect
				.collect(Collectors.toSet());
	}
	
	public FeatureGenerator getGeneratorByName(String name){
		return featureGenerators.get(name);
	}

	@Override
	public void forEach(BiConsumer<Long, Document> action) {
		documents.forEach(action);
	}

	@Override
	public DocumentSet getTrainingSet(){
		Set<Long> filter = new HashSet<>();
		documents.forEach((id,doc)->{
			if(doc.isTraining()){
				filter.add(id);
			}
		});
		return new Documents(this, filter);
	}

	@Override
	public DocumentSet getTestingSet(){
		Set<Long> filter = new HashSet<>();
		documents.forEach((id,doc)->{
			if(!doc.isTraining()){
				filter.add(id);
			}
		});
		return new Documents(this, filter);
	}

	@Override
	public Tuple<DocumentSet,DocumentSet> getTrainValidationSplit(double validationFrac){
		Random rnd = new Random();
		Set<Long> trainFilter = new HashSet<>();
		Set<Long> validationFilter = new HashSet<>();
		documents.forEach((id,doc)->{
			if(doc.isTraining()){
				if(rnd.nextDouble() < validationFrac){
					validationFilter.add(id);
				}else{
					trainFilter.add(id);
				}
			}
		});
		return new Tuple<>(new Documents(this, trainFilter),
				new Documents(this, validationFilter));
	}
	@Override
	// x is train, y is validation
	public List<Tuple<DocumentSet,DocumentSet>> getCrossValidationSplit(int folds){
		Random rnd = new Random();
		List<Tuple<Set<Long>,Set<Long>>> filters = new ArrayList<>();
		for(int i = 0; i < folds; i++){
			filters.add(new Tuple<>(new HashSet<>(),new HashSet<>()));
		}
		
		documents.forEach((id,doc)->{
			if(doc.isTraining()){
				int fold = rnd.nextInt(folds);
				filters.get(fold).y.add(id);
				for(int i = 0; i < folds; i++){
					if(i != fold){
						filters.get(i).x.add(id);
					}
				}
			}
		});
		
		List<Tuple<DocumentSet,DocumentSet>> ret = new ArrayList<>();
		for(int i = 0; i < folds; i++){
			ret.add(new Tuple<>(
					new Documents(this, filters.get(i).x),
					new Documents(this, filters.get(i).y)));
		}
		
		return ret;
	}

	protected Tuple<String,String> breakDownFeatureName(String fname){
		int i = fname.indexOf('_');
		return new Tuple<>(fname.substring(0, i), fname.substring(i + 1));
	}
	@Override
	public String getFeatureDescription(String fname){
		Tuple<String,String> t = breakDownFeatureName(fname);
		FeatureGenerator fg = getGeneratorByName(t.x);
		return fg == null ? null : fg.getFeatureDescription(t.y);
	}
	@Override
	public String getFeatureDescriptiveName(String fname){
		Tuple<String,String> t = breakDownFeatureName(fname);
		FeatureGenerator fg = getGeneratorByName(t.x);
		String s = fg == null ? null : fg.getFeatureDescriptiveName(t.y);
		return s == null ? fname : s;
	}
	
}
