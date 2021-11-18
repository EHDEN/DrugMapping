package org.ohdsi.drugmapping.wrapper;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;


import org.ohdsi.drugmapping.Version;

public class Wrapper {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		//check OS architecture and if it is a first run
		boolean bits64 = System.getProperty("os.arch").contains("64");
		OperatingSystemMXBean mxbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		int physicalMemory = (int) (mxbean.getTotalPhysicalMemorySize()/(1024 * 1024 * 1024));
		long maxMemory = Math.max(4, (int) (physicalMemory * 0.75));
		String datName = "DrugMapping-v" + Version.version + ".dat";
		
		String[] cmdArray = new String[args.length + 5];
		int partNr = -1;
		partNr++; cmdArray[partNr] = "java"; 
		partNr++; cmdArray[partNr] = "-jar";
		partNr++; cmdArray[partNr] = "-Xms512m";
		partNr++; cmdArray[partNr] = bits64 ? "-Xmx" + maxMemory + "G" : "-Xmx1280m";
		partNr++; cmdArray[partNr] = datName;
		for (String arg : args) {
			partNr++; cmdArray[partNr] = arg;
		} 
		
		//DEBUG
		//printDebugInfo(cmdArray);
		
		try {
			Process p = Runtime.getRuntime().exec(cmdArray);
			synchronized (p) {
				p.waitFor();
			}
		} catch (IOException e) {  
			e.printStackTrace();  
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
		System.out.print("Java parameters:");
		for (int partNr = 0; partNr < command.length; partNr++) {
			System.out.print(" " + command[partNr]);
		}
		System.out.println();
	}
}
