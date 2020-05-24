package org.ohdsi.drugmapping.wrapper;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

public class Wrapper {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		//check OS architecture and if it is a first run
		boolean bits64 = System.getProperty("os.arch").contains("64"); 
		//boolean firstRun = !(new File(".", "jerboa.properties")).exists();
		OperatingSystemMXBean mxbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		int physicalMemory = (int) (mxbean.getTotalPhysicalMemorySize()/(1024 * 1024 * 1024));
		int maxMemory = Math.max(4, (int) (physicalMemory * 0.75));
		String wrapperJarName = System.getProperty("java.class.path");
		String datName = wrapperJarName.substring(0, wrapperJarName.length() - 4) + ".dat";
		
		String[] cmdArray = new String[args.length + 6];
		int partNr = -1;
		partNr++; cmdArray[partNr] = "java"; 
		partNr++; cmdArray[partNr] = bits64 ? "-d64" : "";
		partNr++; cmdArray[partNr] = "-jar";
		partNr++; cmdArray[partNr] = "-Xms512m";
		partNr++; cmdArray[partNr] = bits64 ? "-Xmx" + maxMemory + "G" : "-Xmx1280m";
		partNr++; cmdArray[partNr] = datName;
		for (String arg : args) {
			partNr++; cmdArray[partNr] = arg;
		}
		/* Old code from Jerboa
		String[] cmdArray = {
				"java",
				bits64 ? "-d64" : "",
						"-Dfile.encoding=UTF-8",  //force the file encoding here as in the main method is too late
						"-jar",
						"-Xms512m",
						bits64 ? "-Xmx" + maxMemory + "G" : "-Xmx1280m",		 
						datName,
						
		};
		*/ 
		
		//DEBUG
//		printDebugInfo(cmdArray);

		// create the process
		try{
			Process p = Runtime.getRuntime().exec(cmdArray);
			synchronized (p) {
				p.waitFor();
			}
		} catch (IOException ioe) {  
			ioe.printStackTrace();  
		}  
	}
	
	/**
	 * Prints to the console the java version and the command formed for launching the jar.
	 * @param command - the command formed in the wrapper
	 */
	public static void printDebugInfo(String[] command){
		System.out.println("Java version: ");
		System.out.println("-----------------");
		System.out.println(System.getProperty("java.version"));
		System.out.println(command.toString());
	}
}
