package coen346assignment3.memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class MemoryManager {

	// Main Memory and Virtual Memory
	private static ArrayList<Page> virtualMemory = new ArrayList<Page>();
	private static int virtualMemoryTailPointer = 0;
	private static Frame[] mainMemory;

	private static int mainMemorySize;
	private static int usedMainMemory = 0;
	
	private static boolean vmAcceess = false;

	public MemoryManager(int memorySize) {
		mainMemorySize = memorySize;
		mainMemory = new Frame[memorySize];
	}
	
	private static synchronized boolean IsVMAccessed() {
		return vmAcceess;
	}
	
	private synchronized static void SetVMAccess(boolean value) {
		vmAcceess = value;
	}

	/**
	 * Stores Given variable ID and its value to first unassigned spot in main
	 * memory
	 * 
	 * @param variableID
	 * @param value
	 */
	public synchronized static void memStore(String variableID, int value) {
		if (usedMainMemory < mainMemorySize) {// Main memory is not full store as frame
			for (int i = 0; i < mainMemory.length; i++) {// find free place to store
				if (mainMemory[i] == null || mainMemory[i].GetVariableID().equals(variableID)) {// space free
					mainMemory[i] = new Frame(variableID, value);
					usedMainMemory++;// Increase amount of memory used
					return;// stored successfuly so exit
				}
			}
			// space wasnt free (should never reach this point)
			// TODO Deal with exception
		} else {// Main Memory Full Store as page
			Page pageToStore = new Page(variableID, value);
			StorePage(pageToStore);
		}
	}

	/**
	 * Frees up memory location containing this ID
	 * 
	 * @param variableID
	 */
	public synchronized static void memFree(String variableID) {
		// check the main memory first
		for (int i = 0; i < mainMemory.length; i++) {
			if (mainMemory[i] != null) {// ensures memory location has something
				if (mainMemory[i].GetVariableID().equals(variableID)) {// found
					while(mainMemory[i].IsLocked());//wait while locked
					mainMemory[i] = null; // delete what is at memory location
					return;// been removed no need to access virtual memory
				}
			}
		}
		try {
			String newVm = "";
			while(IsVMAccessed());//wait
			SetVMAccess(true);
			Scanner vmScan = new Scanner(new File("vm.txt"));
			while (vmScan.hasNext()) {
				String line = vmScan.next();
				String[] lineSplit = line.split(",");
				if (lineSplit[1].equals(variableID)) {
					newVm += lineSplit[0] + ",null,null\n";// adds in line at correct location
					continue;
				}
				newVm += line + "\n"; // copies in old line
			}
			vmScan.close();// close the file
			SetVMAccess(false);
			while(IsVMAccessed());
			SetVMAccess(true);
			// write vm
			PrintWriter vmWriter = new PrintWriter(new File("vm.txt"));
			vmWriter.write(newVm);
			vmWriter.close();// ensure the file is closed
			SetVMAccess(false);//realace
		} catch (FileNotFoundException e) {
			System.out.println("Virtual Memory Not Found!");
		}
	}

	/**
	 * Checks to see if this variable ID exists in memory
	 * 
	 * @param variableID
	 * @return does this ID Exist
	 */
	public synchronized static int memLookup(String variableID) {
		// check main memory first
		int lastAccessedIndex = 0;

		for (int i = 0; i < mainMemory.length; i++) {// ensures our last accessed index doesn point to null if the
														// memory location was freed up
			if (mainMemory[i] != null) {
				lastAccessedIndex = i;
				break;
			}
		}
		// search
		for (int i = 0; i < mainMemory.length; i++) {

			if (mainMemory[i] != null) {// prevents null comparison if memory location was freed
				if (mainMemory[i].lastAccess.getTime() <= mainMemory[lastAccessedIndex].lastAccess.getTime()) {// if the time is less then it is older because of how system time works
					// new older index
					lastAccessedIndex = i;
				}
				if (mainMemory[i].GetVariableID().equals(variableID)) {
					while(mainMemory[i].IsLocked());//wait until locked relaced
					mainMemory[i].SetLocked(true);
					int value = mainMemory[i].GetValue();
					mainMemory[i].SetLocked(false);
					return value;
				}
			}

		}
		Scanner vmScan = null;
		try {// check virtual memory
			while(IsVMAccessed());//wait
			SetVMAccess(true);
			vmScan = new Scanner(new File("vm.txt"));
			while (vmScan.hasNext()) {
				String line = vmScan.next();
				String[] lineSplit = line.split(",");
				if (lineSplit[1].equals(variableID)) {
					// TODO Memory Swap
					vmScan.close();
					SetVMAccess(false);
					memFree(variableID);// removes current virtua storage of variable which will be in virtual memory
					System.out.println("SWAP: Variable " + mainMemory[lastAccessedIndex].GetVariableID() + " with Variable " + lineSplit[1]);
					Page temp = new Page(mainMemory[lastAccessedIndex].GetVariableID(), mainMemory[lastAccessedIndex].GetValue()); 
					StorePage(temp);//moves it into virutal memory
					mainMemory[lastAccessedIndex] = new Frame(lineSplit[1], Integer.parseInt(lineSplit[2]));

					return mainMemory[lastAccessedIndex].GetValue();
				}
			}

		} catch (FileNotFoundException e) {
			System.out.println("Could not find in virtul memory!");
		}
		vmScan.close();
		SetVMAccess(false);
		System.out.println("Variable does not exist!");
		return -1;
	}

	/**
	 * Store page in our virtual memory Pages stored as CSV and each page has its
	 * own line index, variableID, value
	 * 
	 * @param page
	 */
	public static void StorePage(Page page) {
		virtualMemory.add(page);
		String memoryLocValue = virtualMemoryTailPointer + "," + page.GetVariableID() + "," + page.GetValue();
		StoreToVM(memoryLocValue);
		virtualMemoryTailPointer++;// increase tail pointer
	}

	// Write to VM.txt
	/**
	 * Writes our desired value to store to our virtual memory
	 * 
	 * @param memoryLocValue
	 */
	private static void StoreToVM(String memoryLocValue) {
		try {
			String newVm = "";
			boolean memoryAdded = false;
			while(IsVMAccessed());//wait
			SetVMAccess(true);
			Scanner vmScan = new Scanner(new File("vm.txt"));
			String[] memoryLocDelimeted = memoryLocValue.split(",");
			while (vmScan.hasNext()) {
				String line = vmScan.next();
				String[] lineSplit = line.split(",");
				if (!memoryAdded) {
					if (lineSplit[0].equals(memoryLocDelimeted[0]) || lineSplit[1].equals(memoryLocDelimeted[1])) {
						newVm += memoryLocValue + "\n";// adds in line at correct location
						memoryAdded = true;
						continue;
					}
				}
				newVm += line + "\n"; // copies in old line
			}
			vmScan.close();// close the file
			SetVMAccess(false);
			if (!memoryAdded) {// if our new memory hasnt yet been added store it
				newVm += memoryLocValue + "\n";
			}
			while(IsVMAccessed());//wait
			SetVMAccess(true);
			PrintWriter vmWriter = new PrintWriter(new File("vm.txt"));
			vmWriter.write(newVm);
			vmWriter.close();// ensure the file is closed
			SetVMAccess(false);
		} catch (FileNotFoundException e) {
			System.out.println("Virtual Memory Not Found!");
		}

	}

}
