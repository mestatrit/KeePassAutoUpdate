/**
 * 
 */
package com.napoleoncs.utilities;

import javax.swing.*;

import com.jcraft.jsch.UserInfo;

/**
 * @author barrtj
 * 
 */
public class NonInteractiveUserInfo implements UserInfo {

	protected String passphrase;
	protected JTextField passphraseField = new JPasswordField(20);
	protected String passwd;
	protected JTextField passwordField = new JPasswordField(20);

	/**
	 * Constructor with password.
	 * 
	 * @param passwd
	 *            password
	 * 
	 */
	public NonInteractiveUserInfo(String passwd) {
		this.passwd = passwd;
	}

	public boolean promptYesNo(String str) {
		int i = 0;
		return i == 0;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public boolean promptPassphrase(String message) {
		@SuppressWarnings("unused")
		boolean bResult;
		return bResult = true;
	}

	public String getPassword() {
		return passwd;
	}

	public boolean promptPassword(String message) {
		@SuppressWarnings("unused")
		boolean bResult = false;
		return bResult = true;
	}

	public void showMessage(String message) {
		JOptionPane.showMessageDialog(null, message);
	}
}
