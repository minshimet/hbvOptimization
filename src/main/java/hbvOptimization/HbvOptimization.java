package hbvOptimization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

public class HbvOptimization extends AbstractProblem {
	String runoff_obs_file;
	String runoff_model_file;
	String ice_obs_file;
	String ice_model_file;
	Hashtable<Integer, Parameter> hbvParameters;
	String tplFileFold;
	String parFileFold;
	String shellCommand;
	String shell_command_calculate;
	String output_runoff;
	String output_ice;
	int evaluationTimes = 0;

	public HbvOptimization(String propertiesFile) {
		super(14, 2);
		Properties p = Utils.loadProperties(propertiesFile);
		if (p == null) {
			System.exit(1);
		}

		runoff_obs_file = p.getProperty("runoff_obs_file");
		runoff_model_file = p.getProperty("runoff_model_file");
		ice_obs_file = p.getProperty("ice_obs_file");
		ice_model_file = p.getProperty("ice_model_file");
		hbvParameters = loadParameters(p.getProperty("parameter_range_file"));
		tplFileFold = p.getProperty("tplfile_fold");
		parFileFold = p.getProperty("target_parfile_fold");
		shellCommand = p.getProperty("shell_command");
		shell_command_calculate = p.getProperty("shell_command_calculate");
		output_runoff = p.getProperty("output_runoff");
		output_ice = p.getProperty("output_ice");

	}

	public static Hashtable<Integer, Parameter> loadParameters(String parafile) {
		Hashtable<Integer, Parameter> parameters = new Hashtable<Integer, Parameter>();
		try {
			// Construct BufferedReader from FileReader
			BufferedReader br = new BufferedReader(new FileReader(parafile));

			String line = null;
			int index = 0;
			while ((line = br.readLine()) != null) {
				if (line == null || line.isEmpty())
					break;
				String[] val = line.split(" +");
				Parameter pa = new Parameter();
				pa.setName(val[0]);
				pa.setIndex(index);
				pa.setLowBound(Double.parseDouble(val[1]));
				pa.setUpBound(Double.parseDouble(val[2]));
				parameters.put(index, pa);
				index++;
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parameters;
	}

	@Override
	public void evaluate(Solution solution) {
		double[] x = EncodingUtils.getReal(solution);
		for (int i = 0; i < getNumberOfVariables(); i++) {
			hbvParameters.get(i).setValue(x[i]);
			System.out.print(x[i] + ",");
		}
		generateParfile(hbvParameters, this.tplFileFold, this.parFileFold, "GeneralParametersDaily");
		generateParfile(hbvParameters, this.tplFileFold, this.parFileFold, "HbvSoilParameters");
		generateParfile(hbvParameters, this.tplFileFold, this.parFileFold, "LandSurfaceParameters");
		// Remove old model file
		Utils.removeFile(runoff_model_file);
		Utils.removeFile(ice_model_file);
		executeHbvModel();
		double ic =10000;
		double rf =10000;
		if (Utils.fileExist(ice_model_file)) {
			ic = evaluteIce(false);
			rf = evaluteRunOff(false);
		}
		solution.setObjective(0, rf);
		solution.setObjective(1, ic);
		System.out.println("evaluationTimes " + evaluationTimes + ": " + rf + ", " + ic);
		evaluationTimes++;
	}

	public void evaluate(String[] values) {
		for (int i = 0; i < values.length; i++) {
			hbvParameters.get(i).setValue(Double.parseDouble(values[i]));
		}
		generateParfile(hbvParameters, this.tplFileFold, this.parFileFold, "GeneralParametersDaily");
		generateParfile(hbvParameters, this.tplFileFold, this.parFileFold, "HbvSoilParameters");
		generateParfile(hbvParameters, this.tplFileFold, this.parFileFold, "LandSurfaceParameters");
		// Remove old model file
		Utils.removeFile(runoff_model_file);
		Utils.removeFile(ice_model_file);
		executeHbvModel();
		Utils.removeFile(output_runoff);
		Utils.removeFile(output_ice);
		double rf = evaluteRunOff(true);
		double ic = evaluteIce(true);
		System.out.println("RMSE RUNOFF: " + rf + " RMSE ICE: " + ic);
	}

	private double evaluteIce(boolean exportResult) {
		try {
			Hashtable<String, Double> iceObs = loadValues(ice_obs_file);
			Hashtable<String, Double> iceMod = loadValues(ice_model_file);
			String date;
			Set<String> keys = iceMod.keySet();
			Iterator<String> itr = keys.iterator();
			double[][] pairs = new double[iceObs.size()][2];
			int i = 0;
			while (itr.hasNext()) {
				date = itr.next();
				if (!date.contains("0901/1200")) {
					continue;
				}
				String obsKey = date.substring(0, 4);
				if (iceObs.get(obsKey) == null || iceObs.get(obsKey) == -9999 || iceMod.get(date) == -9999) {
					// if no observation data found that set both same value
					continue;
				} else {
					pairs[i][1] = iceMod.get(date) / 1000;
					pairs[i][0] = iceObs.get(obsKey);
					if (exportResult) {
						Utils.appendToFile(obsKey + " " + pairs[i][0] + " " + pairs[i][1], output_ice);
					}
					i++;
				}

			}
			return calculateRMSE(pairs);
		} catch (Exception e) {
			System.out.println(e.toString());
			return 10000000;
		}
	}

	private double evaluteRunOff(boolean exportResult) {
		try {
			Hashtable<String, Double> runoffObs = loadValues(runoff_obs_file);
			Hashtable<String, Double> runoffMod = loadValues(runoff_model_file);
			String date;
			Set<String> keys = runoffMod.keySet();
			Iterator<String> itr = keys.iterator();
			double[][] pairs = new double[runoffMod.size()][2];
			int i = 0;
			while (itr.hasNext()) {
				date = itr.next();
				if (runoffObs.get(date) == null || runoffObs.get(date) == -9999) {
					// if no observation data found that set both same value
					continue;
				} else {
					pairs[i][1] = runoffMod.get(date);
					pairs[i][0] = runoffObs.get(date);
					if (exportResult) {
						Utils.appendToFile(date + " " + pairs[i][0] + " " + pairs[i][1], output_runoff);
					}
					i++;
				}
			}
			return calculateRMSE(pairs);
		} catch (Exception e) {
			System.out.println(e.toString());
			return 10000000;
		}
	}

	private double calculateRMSE(double[][] pairValues) {
		double sum_sq = 0;
		double err;
		int validNum = 0;
		for (int i = 0; i < pairValues.length; ++i) {
			if (pairValues[i][0] == 0 && pairValues[i][1] == 0) {
				continue;
			}
			err = pairValues[i][0] - pairValues[i][1];
			sum_sq += (err * err);
			validNum++;
		}
		return (double) Math.sqrt(sum_sq / validNum);
	}

	private Hashtable<String, Double> loadValues(String filename) throws NumberFormatException, IOException {
		Hashtable<String, Double> values = new Hashtable<String, Double>();
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] value = line.split("\\s+");
			values.put(value[0], Double.parseDouble(value[1]));
		}
		br.close();

		return values;
	}

	private void executeHbvModel() {
		// run hbv model
		String command = "sh " + shellCommand;
		try {
			Process proc = Runtime.getRuntime().exec(command);
			BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			// calculate model
			command = "sh " + shell_command_calculate;
			proc = Runtime.getRuntime().exec(command);
			read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			// while (read.ready()) {
			// System.out.println(read.readLine());
			// }
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public static void generateParfile(Hashtable<Integer, Parameter> hbvParameters, String tplFileFold,
			String parFileFold, String filename) {
		Utils.removeFile(parFileFold + filename + ".par");
		try {
			BufferedReader br = new BufferedReader(new FileReader(tplFileFold + filename + ".tpl"));
			String line = null;
			while ((line = br.readLine()) != null) {
				for (int i = 0; i < hbvParameters.size(); i++) {
					String name = hbvParameters.get(i).getName();
					if (line.indexOf(name) >= 0) {
						line = line.replaceAll(name, hbvParameters.get(i).getValue() + "");
					}
				}
				Utils.appendToFile(line, parFileFold + filename + ".par");
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Solution newSolution() {
		Solution solution = new Solution(getNumberOfVariables(), getNumberOfObjectives());

		for (int i = 0; i < getNumberOfVariables(); i++) {
			solution.setVariable(i,
					new RealVariable(hbvParameters.get(i).getLowBound(), hbvParameters.get(i).getUpBound()));
		}

		return solution;
	}
}
