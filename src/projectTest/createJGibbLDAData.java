package projectTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import edu.featgen.Documents;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSource;
import edu.featgen.standard.generator.BOWFeatureGenerator;
import edu.featgen.standard.util.Logger;
import edu.featgen.standard.util.XMLConfigurationLoader;
import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.search.ESASearcher;

/**
 * Usage:
 * createJGibbLDAData <output_filename> <BOW_Generator_Name>
 *
 *	<output_filename> - path to the output file that will be created
 *  <BOW_Generator_Name> - name of BOW generator as defined in featgen-config.xml
 */
public class createJGibbLDAData {
	public static void main(String[] args) throws IOException {
		FileOutputStream fout = new FileOutputStream(args[0]);
		saveToFileESA(args[1], fout);
		fout.close();
	}

	public static void saveToFileESA(String BOWGeneratorName, OutputStream fout){
		PrintWriter pw = new PrintWriter(fout);
		Documents docs = 
				XMLConfigurationLoader.buildFromFile(new File("featgen-config.xml"))
				.getDocuments();
		BOWFeatureGenerator gen = (BOWFeatureGenerator)docs.getGeneratorByName(BOWGeneratorName);
		DocumentSource documentSource = docs.getDocumentSource();

		documentSource.reset();
		Document newDoc = documentSource.getNext();
		int nDocs = 0;
		while(newDoc != null) {
			if (newDoc.isTraining()){
				nDocs++;
			}
			newDoc = documentSource.getNext();
		}

		pw.println(nDocs);
		ESASearcher esa = new ESASearcher();
		
		documentSource.reset();
		newDoc = documentSource.getNext();
		Logger.writeToConsole("Saving data for JGibbLDA from: " + documentSource.getName());
		while(newDoc != null) {
			if (newDoc.isTraining()){
				gen.forEachNGram(newDoc, (ngram,i)->{
					IConceptVector v = esa.getNormalVector(ngram, 10);
					if (v == null){
						return;
					}
					IConceptIterator it = v.iterator();
					while(it.next()){
						try {
							pw.print("C" + it.getId());
							pw.print(" ");
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
				pw.println();
			}
			newDoc = documentSource.getNext();
		}
	}
	
	public static void saveToFile(String BOWGeneratorName, OutputStream fout){
		PrintWriter pw = new PrintWriter(fout);
		Documents docs = 
				XMLConfigurationLoader.buildFromFile(new File("featgen-config.xml"))
				.getDocuments();
		BOWFeatureGenerator gen = (BOWFeatureGenerator)docs.getGeneratorByName(BOWGeneratorName);
		DocumentSource documentSource = docs.getDocumentSource();

		documentSource.reset();
		Document newDoc = documentSource.getNext();
		int nDocs = 0;
		while(newDoc != null) {
			if (newDoc.isTraining()){
				nDocs++;
			}
			newDoc = documentSource.getNext();
		}

		pw.println(nDocs);
		
		documentSource.reset();
		newDoc = documentSource.getNext();
		Logger.writeToConsole("Saving data for JGibbLDA from: " + documentSource.getName());
		while(newDoc != null) {
			if (newDoc.isTraining()){
				gen.forEachNGram(newDoc, (ngram,i)->{
					try {
						pw.print(ngram);
						pw.print(" ");
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				pw.println();
			}
			newDoc = documentSource.getNext();
		}
	}
}
