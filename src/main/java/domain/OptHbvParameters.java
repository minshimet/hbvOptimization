package domain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import evolution.Environment;
import evolution.Individual;
import evolution.Utils;

public class OptHbvParameters extends Environment {
	int paraNumber;
	String runoff_obs_file;
	String runoff_model_file;
	String ice_obs_file;
	String ice_model_file;
	Hashtable<String, Parameter> hbvParameters;
	String parFile;
	String tplFileFold;
	String parFileFold;

	public OptHbvParameters(String evoProperites) {
		super(evoProperites);

		Properties p = Utils.loadProperties(evoProperites);
		if (p == null)
			System.exit(1);

		runoff_obs_file = p.getProperty("runoff_obs_file");
		runoff_model_file = p.getProperty("runoff_model_file");
		ice_obs_file = p.getProperty("ice_obs_file");
		ice_model_file = p.getProperty("ice_model_file");
		hbvParameters = loadParameters(p.getProperty("parameter_file"));
		tplFileFold = p.getProperty("tplfile_fold");
		parFileFold = p.getProperty("target_parfile_fold");
		paraNumber = hbvParameters.size();
	}

	private static Hashtable<String, Parameter> loadParameters(String parafile) {
		Hashtable<String, Parameter> parameters = new Hashtable<String, Parameter>();
		try {
			// Construct BufferedReader from FileReader
			BufferedReader br = new BufferedReader(new FileReader(parafile));

			String line = null;
			int index = 0;
			while ((line = br.readLine()) != null) {
				if (line==null || line.isEmpty())
					break;
				String[] val = line.split(" +");
				Parameter pa = new Parameter();
				pa.setName(val[0]);
				pa.setIndex(index);
				pa.setLowBound(Double.parseDouble(val[1]));
				pa.setUpBound(Double.parseDouble(val[2]));
				parameters.put(index + "", pa);
				index++;
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return parameters;
	}

	private Hashtable<String, Parameter> convertIndividual(String bitString) {
		Hashtable<String, Parameter> parameters = new Hashtable<String, Parameter>();

		for (int i = 0; i < paraNumber; i++) {
			Parameter parameter = new Parameter();
			parameter.setIndex(i);
			parameter.setName(hbvParameters.get(i+"").getName());
			parameter.setValue(bitsToReal(bitString.substring(i * bitsLen, (i + 1) * bitsLen),
					hbvParameters.get(i+"").getLowBound(), hbvParameters.get(i+"").getUpBound()));
			parameters.put(i+"", parameter);
		}

		return parameters;
	}
	
	private Hashtable<String, Parameter> convertIndividual(Individual individual) {
		Hashtable<String, Parameter> parameters = new Hashtable<String, Parameter>();
		String bitString;

		// chromosome to string
		bitString = individual.toString();

		for (int i = 0; i < paraNumber; i++) {
			Parameter parameter = new Parameter();
			parameter.setIndex(i);
			parameter.setName(hbvParameters.get(i).getName());
			parameter.setValue(bitsToReal(bitString.substring(i * bitsLen, (i + 1) * bitsLen),
					hbvParameters.get(i).getLowBound(), hbvParameters.get(i).getUpBound()));
		}

		return parameters;
	}

	public double bitsToReal(String bits, double lowBound, double upBound) {
		int integer = Integer.parseInt(bits, 2);
		return (double) integer / Math.pow(2, bitsLen) * (upBound - lowBound) + lowBound;
	}

	@Override
	public double evalSolution(Individual individual) {
		// TODO Generate model parameter files
		Hashtable<String, Parameter> parameters = convertIndividual(individual);
		generateParfile(parameters, parFileFold);
		return 0;
	}
	
	public double evalSolution(String individual) {
		// TODO Generate model parameter files
		Hashtable<String, Parameter> parameters = convertIndividual(individual);
		generateParfile(parameters, parFileFold);
		return 0;
	}

	private void generateParfile(Hashtable<String, Parameter> parameters, String fileFold) {
		// remove old par files
		removeFile(parFileFold + "GeneralParametersDaily.par");
		removeFile(parFileFold + "GeneralParametersDaily.par");
		removeFile(parFileFold + "GeneralParametersDaily.par");
		// TODO Auto-generated method stub
		insertParameter(parameters,"GeneralParametersDaily");
		System.out.println();
		insertParameter(parameters,"HbvSoilParameters");
		System.out.println();
		insertParameter(parameters,"LandSurfaceParameters");
	}

	public static void removeFile(String filename) {
		try {

			File file = new File(filename);

			if (file.delete()) {
				System.out.println("Old par file " + file.getName() + " is deleted!");
			} else {
				System.out.println("Delete operation is failed.");
			}

		} catch (Exception e) {

			e.printStackTrace();

		}
	}

	private void insertParameter(Hashtable<String, Parameter> parameters, String filename) {
		try {
			// Construct BufferedReader from FileReader
			BufferedReader br = new BufferedReader(new FileReader(tplFileFold+filename+".tpl"));

			String line = null;
			while ((line = br.readLine()) != null) {
				for (int i=0;i<parameters.size();i++) {
					String name=parameters.get(i+"").getName();
					if (line.indexOf(name)>=0) {
						line=line.replaceAll(name, parameters.get(i+"").getValue()+"");
					}
				}
				System.out.println(line);
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public double evalSolution(Individual[] individual) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDimension() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vector createFullSolution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector createSubSolution(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getMaxFitness() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPopulationNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void nextTask() {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
		/**
		 * 
0111111111111110
1111011111111111
1111111111111011
1111111111111111
1111011111111111
1111110111111111
1111111110111111
1111111111101111
1111111011101111
1110111111111111
1111111011111111
1111111111101111
1100000111111111
1111111111000011
		 * */
		OptHbvParameters op=new OptHbvParameters("/disk1/git/NE/evolution/src/main/resources/hbv.properties");
		String individual="01111111111111101111011111111111111111111111101111111111111111111111011111111111111111011111111111111111101111111111111111101111111111101110111111101111111111111111111011111111111111111110111111000001111111111111111111000011";
		op.evalSolution(individual);
	}

}
