package hbvOptimization;

import org.moeaframework.Executor;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

public class RunHbvOptimization {
	public static void main(String[] args) {
		NondominatedPopulation result = new Executor().withAlgorithm("NSGAII").withProblemClass(HbvOptimization.class)
				.withMaxEvaluations(10000).run();
		for (Solution solution : result) {
			System.out.printf("%.5f => %.5f, %.5f\n", EncodingUtils.getReal(solution.getVariable(0)),
					solution.getObjective(0), solution.getObjective(1));
		}

		new Plot().add("NSGAII", result).show();
	}
}
