package org.ohdsi.drugmapping.wrapper;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class Wrapper {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		//check OS architecture and if it is a first run
		boolean bits64 = System.getProperty("os.arch").contains("64"); 
		//boolean firstRun = !(new File(".", "jerboa.properties")).exists();
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		Object attribute;
		try {
			attribute = mBeanServer.getAttribute(new ObjectName("java.lang","type","OperatingSystem"), "TotalPhysicalMemorySize");
			long physicalMemory = Long.parseLong(attribute.toString()) / (1024 * 1024 * 1024);
			
			//OperatingSystemMXBean mxbean = ManagementFactory.getOperatingSystemMXBean();
			//int physicalMemory = (int) (mxbean.getTotalPhysicalMemorySize()/(1024 * 1024 * 1024));
			long maxMemory = Math.max(4, (int) (physicalMemory * 0.75));
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
			
			//DEBUG
			//printDebugInfo(cmdArray);
			
			Process p = Runtime.getRuntime().exec(cmdArray);
			synchronized (p) {
				p.waitFor();
			}
		} catch (InstanceNotFoundException e) {
			e.printStackTrace();
		} catch (AttributeNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} catch (ReflectionException e) {
			e.printStackTrace();
		} catch (MBeanException e) {
			e.printStackTrace();
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
