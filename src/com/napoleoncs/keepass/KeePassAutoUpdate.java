/**
 * 
 */
package com.napoleoncs.keepass;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * @author barrtj
 * 
 */
public class KeePassAutoUpdate {

	static String dbpw;
	static String driveLetterToMap;
	static String networkPathToKeePass;
	static String pathToDb;
	static String pathToKey;
	static String pwGenCount;
	static String pwGenProfile;
	static File tempDbExtractFile;
	static File tempDbImportFile;
	static String tempDbImportFileName;
	static File tempDirectory;
	static String tempDirName;
	static String tempOutputFileName;

	private static void createTempDirectory() {
		tempDirectory = new File(driveLetterToMap + ":\\" + tempDirName);
		tempDirectory.mkdir();
	}

	private static void createXmlImportFromObj(KeePassFile tmpKpf)
			throws JAXBException, FileNotFoundException {
		JAXBContext jc = javax.xml.bind.JAXBContext.newInstance(tmpKpf
				.getClass().getPackage().getName());
		Marshaller m = jc.createMarshaller();
		m.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "utf-8");
		m.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
				Boolean.TRUE);
		String importFile = driveLetterToMap + ":\\" + tempDirName + "\\"
				+ tempDbImportFileName;
		tempDbImportFile = new File(importFile);
		OutputStream os = new FileOutputStream(importFile);
		m.marshal(tmpKpf, os);
	}

	private static void disconnectWindowsNetworkDrive() throws IOException,
			InterruptedException {
		String disconnectDrive = "C:\\Windows\\system32\\net.exe use "
				+ driveLetterToMap + ": /delete";
		Process disconnectProc = Runtime.getRuntime().exec(
				"cmd /c " + disconnectDrive);
		disconnectProc.waitFor();
	}

	private static void errorBox(String infoMessage, String location) {
		JTextArea textarea = new JTextArea(infoMessage);
		textarea.setEditable(false);
		JScrollPane jsp = new JScrollPane(textarea);
		jsp.setPreferredSize(new Dimension(480, 320));
		Object[] errorObjs = new Object[] {
				String.format("An error has occured during the Automatic Update%nof the KeePass Database.%nBelow is the error, enjoy debugging!"),
				jsp };
		JOptionPane.showMessageDialog(null, errorObjs, "ERROR: " + location,
				JOptionPane.ERROR_MESSAGE);
	}

	private static void exportDbToFile() throws IOException,
			InterruptedException {
		String exportDbCommand = driveLetterToMap
				+ ":\\KPScript.exe -c:Export \"" + driveLetterToMap + ":"
				+ pathToDb;
		if (dbpw != null && !dbpw.isEmpty()) {
			exportDbCommand = exportDbCommand + " -pw:" + dbpw;
		}
		if (pathToKey != null && !pathToKey.isEmpty()) {
			exportDbCommand = exportDbCommand + "\" -keyfile:\""
					+ driveLetterToMap + ":" + pathToKey + "\"";
		}
		exportDbCommand = exportDbCommand
				+ " -Format:\"KeePass XML (2.x)\" -OutFile:\""
				+ driveLetterToMap + ":\\" + tempDirName + "\\"
				+ tempOutputFileName + "\"";
		Process exportDbProc = Runtime.getRuntime().exec(
				"cmd /c " + exportDbCommand);
		exportDbProc.waitFor();
		tempDbExtractFile = new File(driveLetterToMap + ":\\" + tempDirName
				+ "\\" + tempOutputFileName);
	}

	private static List<String> getNewPwList() throws IOException {
		List<String> tempList = new ArrayList<>();
		String genPwCommand = driveLetterToMap
				+ ":\\KPScript.exe -c:GenPw -count:" + pwGenCount
				+ " -profile:" + pwGenProfile;
		Process proc = Runtime.getRuntime().exec("cmd /c " + genPwCommand);
		InputStreamReader isr = new InputStreamReader(proc.getInputStream());
		BufferedReader br = new BufferedReader(isr);

		String pwTemp = null;
		while ((pwTemp = br.readLine()) != null
				&& !pwTemp
						.equalsIgnoreCase("OK: Operation completed successfully."))
			tempList.add(pwTemp);
		return tempList;
	}

	private static void loadImportFileToDb() {
		//TODO: Needs Engineered still.
	}

	private static KeePassFile loadExportToObj() throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(KeePassFile.class);
		Unmarshaller u = jc.createUnmarshaller();
		KeePassFile tmpKpf = (KeePassFile) u.unmarshal(tempDbExtractFile);
		return tmpKpf;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Properties prop = new Properties();
		List<String> pwList = new ArrayList<>();

		try {
			// Reading in Properties file
			InputStream in = KeePassAutoUpdate.class
					.getResourceAsStream("keePassAutoUpdate.properties");
			prop.load(in);

			dbpw = prop.getProperty("dbpw");
			driveLetterToMap = prop.getProperty("driveLetterToMap");
			networkPathToKeePass = prop.getProperty("networkPathToKeePass");
			pathToDb = prop.getProperty("pathToDb");
			pathToKey = prop.getProperty("pathToKey");
			pwGenCount = prop.getProperty("pwGenCount");
			pwGenProfile = prop.getProperty("pwGenProfile");
			tempDbImportFileName = prop.getProperty("tempDbImportFileName");
			tempDirName = prop.getProperty("tempDirName");
			tempOutputFileName = prop.getProperty("tempOutputFileName");
			String groups = prop.getProperty("groups");

			// Map Windows Network Drive
			mapWindowsNetworkDrive();

			// Create Temp Directory
			createTempDirectory();

			// Export DB to XML file
			exportDbToFile();

			// Get New Passwords
			pwList = getNewPwList();

			// Load XML File to Object
			KeePassFile keePassFile = loadExportToObj();

			// Delete extracted file
			tempDbExtractFile.delete();

			//Proprietary Code for Traversing the root object and changing the 
			//passwords per EA DBA's Group Structure
			keePassFile.setRoot(Updater.updateRoot(keePassFile.getRoot(),
					pwList, groups));

			// Create XML File from updated Object
			createXmlImportFromObj(keePassFile);

			// Import XML File into KeePass DB
			loadImportFileToDb();

			// Delete temp import file
			tempDbImportFile.delete();

			// Delete temp directory
			tempDirectory.delete();

			// Disconnect Windows Network Drive
			disconnectWindowsNetworkDrive();

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			errorBox(sw.toString(), "KeePassAutoUpdate.Main");
			e.printStackTrace();
		}

	}

	private static void mapWindowsNetworkDrive() throws IOException,
			InterruptedException {
		String mapDriveCommand = "C:\\Windows\\system32\\net.exe use "
				+ driveLetterToMap + ": \"" + networkPathToKeePass + "\"";
		Process mapProc = Runtime.getRuntime()
				.exec("cmd /c " + mapDriveCommand);
		mapProc.waitFor();
	}
}
