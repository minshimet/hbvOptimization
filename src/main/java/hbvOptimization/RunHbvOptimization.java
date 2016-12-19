package hbvOptimization;

import java.util.Properties;

import org.moeaframework.Executor;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

public class RunHbvOptimization {
	public static void main(String[] args) {
		if (args.length<1) {
			System.out.println("Please give problem properties file");
			System.exit(1);
		}
		Properties p = Utils.loadProperties(args[0]);
		if (p == null) {
			System.out.println("Error to load problem properties file");
			System.exit(1);
		}
		int evalutationTimes=100;
		String algorithm=p.getProperty("algorithm","NSGAII");
		evalutationTimes = Integer.parseInt(p.getProperty("evaluation_times"));
		Executor executor=new Executor().withAlgorithm(algorithm).withProblemClass(HbvOptimization.class,args[0])
				.withMaxEvaluations(evalutationTimes).distributeOnAllCores();
		if (p.getProperty("distribute_cores").equalsIgnoreCase("true")) {
			executor.distributeOnAllCores();
		}
		String algorithmProperties[]=p.getProperty("algorithm_properties").split(",");
		for (String property:algorithmProperties) {
			String[] token=property.split("=");
			if (token.length==2) {
				executor.withProperty(token[0], token[1]);
			}
		}
		NondominatedPopulation result = executor.run();
		for (Solution solution : result) {
			System.out.printf("%.5f => %.5f, %.5f\n", EncodingUtils.getReal(solution.getVariable(0)),
					solution.getObjective(0), solution.getObjective(1));
		}

		//new Plot().add("NSGAII", result).show();
	}
}
