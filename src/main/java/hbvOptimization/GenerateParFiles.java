package hbvOptimization;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

public class GenerateParFiles {
	
	public static void generateParFromParameters(String confile) throws FileNotFoundException, IOException {
		Properties p = Utils.loadProperties(confile);
		if (p == null)
			System.exit(1);
		Hashtable<Integer, Parameter> hbvParameters=HbvOptimization.loadParameters(p.getProperty("parameter_range_file"));;
		String tplFileFold = p.getProperty("tplfile_fold");
		String parFileFold = p.getProperty("target_parfile_fold");
		String parameter_file = Utils.readFirstLineFromFile(p.getProperty("parameter_file"));
		String[] values=parameter_file.split(",");
		for (int i=0;i<values.length;i++) {
			hbvParameters.get(i).setValue(Double.parseDouble(values[i]));
		}
		HbvOptimization.generateParfile(hbvParameters, tplFileFold, parFileFold, "GeneralParametersDaily");
		HbvOptimization.generateParfile(hbvParameters, tplFileFold, parFileFold, "HbvSoilParameters");
		HbvOptimization.generateParfile(hbvParameters, tplFileFold, parFileFold, "LandSurfaceParameters");
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length<1)
			return;
		generateParFromParameters(args[0]);
	}
}
