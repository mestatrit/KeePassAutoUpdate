/**
 * 
 */
package com.napoleoncs.keepass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.jcraft.jsch.JSchException;
import com.napoleoncs.utilities.SshAccess;

import org.apache.commons.lang3.SerializationUtils;

/**
 * @author barrtj
 * 
 */
public class Updater {

	/**
	 * This map is to keep track while running which users on which servers have
	 * already been updated to prevent changing the password a second time.
	 * 
	 */
	static HashMap<String, HashMap<String, String>> serverMap = new HashMap<String, HashMap<String, String>>();

	private static Group alterItems(Group group, List<String> pwList)
			throws JSchException, InterruptedException, IOException,
			DatatypeConfigurationException {
		String password = null;
		String host = null;
		String username = null;
		String newPassword = pwList.get(0);
		List<Entry> entries = group.getEntry();
		Iterator<Entry> entryIter = entries.iterator();
		while (entryIter.hasNext()) {
			Entry entry = entryIter.next();
			List<Entry.String> entryStrings = entry.getString();
			password = getEntryString("Password", entryStrings);
			host = getEntryString("Title", entryStrings);
			username = getEntryString("UserName", entryStrings);
			if (!checkIfPasswordAlreadyChangedOnHost(host, username)) {
				/*
				 * SshAccess sa = new SshAccess(host, username, password);
				 * sa.changePassword(username, newPassword);
				 * sa.disconnectChannel(); sa.close();
				 */
				pwList.remove(0);
				updateEntry(entry, newPassword, entryStrings, username, host);
			} else {
				updateEntry(entry, newPassword, entryStrings, username, host);
			}
		}
		return group;
	}

	private static boolean checkIfPasswordAlreadyChangedOnHost(String host,
			String username) {
		if (!serverMap.isEmpty()) {
			if (serverMap.containsKey(host)) {
				HashMap<String, String> userMap = serverMap.get(host);
				if (userMap.containsKey(username)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	private static String getEntryString(String key,
			List<Entry.String> entryStrings) {
		String value = null;
		Iterator<Entry.String> esIter = entryStrings.iterator();
		while (esIter.hasNext()) {
			Entry.String entryString = esIter.next();
			if (entryString.key.equalsIgnoreCase(key)) {
				value = entryString.value;
			}
		}
		return value;
	}

	private static XMLGregorianCalendar getXMLDateTime()
			throws DatatypeConfigurationException {
		GregorianCalendar gc = new GregorianCalendar();
		XMLGregorianCalendar xcal = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(gc);
		return xcal;
	}

	private static void insertIntoServerMap(String host, String password,
			String username) {
		HashMap<String, String> userMap;
		if (serverMap.containsKey(host)) {
			userMap = serverMap.get(host);
			userMap.put(username, password);
		} else {
			userMap = new HashMap<String, String>();
			userMap.put(username, password);
			serverMap.put(host, userMap);
		}
	}

	private static List<Entry.String> setEntryString(String key, String value,
			List<Entry.String> entryStrings) {
		Iterator<Entry.String> esIter = entryStrings.iterator();
		while (esIter.hasNext()) {
			Entry.String entryString = esIter.next();
			if (entryString.key.equalsIgnoreCase(key)) {
				entryString.setValue(value);
			}
		}
		return entryStrings;
	}

	private static List<Group> spanGroupTree(List<Group> groups,
			List<String> groupTreeNames, List<String> pwList)
			throws JSchException, InterruptedException, IOException,
			DatatypeConfigurationException {
		String thisLevelGroupNameToFind = groupTreeNames.get(0);
		Iterator<Group> groupIter = groups.iterator();
		while (groupIter.hasNext()) {
			Group tempGroup = groupIter.next();
			if (tempGroup.getName().equalsIgnoreCase(thisLevelGroupNameToFind)) {
				if (tempGroup.getEntry().isEmpty()) {
					groupTreeNames.remove(0);
					tempGroup.setGroup(spanGroupTree(tempGroup.getGroup(),
							groupTreeNames, pwList));
				} else {
					tempGroup = alterItems(tempGroup, pwList);
				}
			}
		}
		return groups;
	}

	private static void updateEntry(Entry entry, String newPassword,
			List<Entry.String> entryStrings, String username, String host)
			throws DatatypeConfigurationException {
		Entry entryClone = SerializationUtils.clone(entry);
		entry.getHistory().entry.add(entryClone);
		entry.setString(setEntryString("Password", newPassword, entryStrings));
		insertIntoServerMap(host, newPassword, username);
		entry.getTimes().setLastModificationTime(getXMLDateTime());
	}

	public static Root updateRoot(Root root, List<String> pwList, String groups)
			throws JSchException, InterruptedException, IOException,
			DatatypeConfigurationException {
		List<Group> rootGroupList = root.getGroup();
		String[] arrayOfGroupTrees = groups.split("~");
		for (int i = 0; i < arrayOfGroupTrees.length; i++) {
			String delimitedGroupTreeString = arrayOfGroupTrees[i];
			String[] arrayOfGroupTreeEnrties = delimitedGroupTreeString
					.split("\\^");
			String parentName = arrayOfGroupTreeEnrties[0];
			List<String> subgroupTreeNames = new ArrayList<>();
			for (int j = 1; j < arrayOfGroupTreeEnrties.length; j++) {
				subgroupTreeNames.add(arrayOfGroupTreeEnrties[j]);
			}
			Iterator<Group> rootGroupIter = rootGroupList.iterator();
			while (rootGroupIter.hasNext()) {
				Group tempRootGroup = rootGroupIter.next();
				if (tempRootGroup.getName().equalsIgnoreCase(parentName)) {
					Group parentGroup = tempRootGroup;
					parentGroup.setGroup(spanGroupTree(parentGroup.getGroup(),
							subgroupTreeNames, pwList));
				}
			}
		}
		return root;
	}
}
