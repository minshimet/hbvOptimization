package hbvOptimization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {
	public static Properties loadProperties(String filename) {
		Properties p = new Properties();
		InputStream iStream = null;
		try {
			iStream = new FileInputStream(filename);
			p.load(iStream);
		} catch (IOException e) {
			System.err.println("IO exception while reading properties:" + e);
			return null;
		} finally {
			if (iStream != null) {
				try {
					iStream.close();
				} catch (IOException e) {
					System.err.println("IO exception while closing:" + e);
					return null;
				}
			}
		}
		return p;
	}
	
	public static void removeFile(String filename) {
		try {

			File file = new File(filename);

			if (file.delete()) {
				//System.out.println("Old par file " + file.getName() + " is deleted!");
			} else {
				//System.out.println("Delete file "+ file.getName() +" operation is failed.");
			}

		} catch (Exception e) {

			e.printStackTrace();

		}
	}
	
	public static void appendToFile(String text, String fileName){
		if (text.isEmpty())
			return;
		try {
			File file = new File(fileName);

			FileWriter fw = new FileWriter(file, true);
			fw.write(text+"\r\n");
			fw.close();

		} catch (IOException iox) {
			iox.printStackTrace();
		}
	}
}
