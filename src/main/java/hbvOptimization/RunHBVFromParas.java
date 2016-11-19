package hbvOptimization;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

public class RunHBVFromParas {
	
	public static void runHBVFromParameters(String paras) throws FileNotFoundException, IOException {
		
		String[] values=paras.split(",");
		HbvOptimization hbvOptimization=new HbvOptimization();
		hbvOptimization.evaluate(values);
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length<1)
			return;
		runHBVFromParameters(args[0]);
		System.out.println("done!");
	}
}
