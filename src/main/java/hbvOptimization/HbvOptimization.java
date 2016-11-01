package hbvOptimization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Properties;
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

	public HbvOptimization() {
		super(14, 2);
		Properties p = Utils.loadProperties("/disk1/git/hbvOptimization/src/main/resources/hbv.properties");
		if (p == null)
			System.exit(1);

		runoff_obs_file = p.getProperty("runoff_obs_file");
		runoff_model_file = p.getProperty("runoff_model_file");
		ice_obs_file = p.getProperty("ice_obs_file");
		ice_model_file = p.getProperty("ice_model_file");
		hbvParameters = loadParameters(p.getProperty("parameter_file"));
		tplFileFold = p.getProperty("tplfile_fold");
		parFileFold = p.getProperty("target_parfile_fold");
		shellCommand = p.getProperty("shell_command");
	}

	private static Hashtable<Integer, Parameter> loadParameters(String parafile) {
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		return parameters;
	}

	@Override
	public void evaluate(Solution solution) {
		double[] x = EncodingUtils.getReal(solution);
		for (int i = 0; i < getNumberOfVariables(); i++) {
			hbvParameters.get(i).setValue(x[i]);
		}
		generateParfile("GeneralParametersDaily");
		generateParfile("HbvSoilParameters");
		generateParfile("LandSurfaceParameters");
		executeHbvModel();
		System.out.println("Done!");
		System.exit(0);
	}

	private void executeHbvModel() {
		String command = "sh "+shellCommand;
		try {
            Process proc = Runtime.getRuntime().exec(command);
            BufferedReader read = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
//            while (read.ready()) {
//                System.out.println(read.readLine());
//            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
	}

	private void generateParfile(String filename) {
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
