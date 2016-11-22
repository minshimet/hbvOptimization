package hbvOptimization;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

public class RunHBVFromParas {
	
	public static void runHBVFromParameters(String propertiesFile, String paras) throws FileNotFoundException, IOException {
		
		String[] values=paras.split(",");
		HbvOptimization hbvOptimization=new HbvOptimization(propertiesFile);
		hbvOptimization.evaluate(values);
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length<2)
			return;
		runHBVFromParameters(args[0],args[1]);
		System.out.println("done!");
	}
}
