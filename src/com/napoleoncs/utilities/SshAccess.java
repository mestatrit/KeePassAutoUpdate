/**
 * 
 */
package com.napoleoncs.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.*;

/**
 * @author barrtj
 * 
 */
public class SshAccess {

	private Channel currentChannel;

	private InputStream in;

	private Session session;

	public SshAccess(String host, String user, String pass)
			throws JSchException, InterruptedException {
		JSch jsch = new JSch();

		session = jsch.getSession(user, host, 22);

		UserInfo ui = new NonInteractiveUserInfo(pass);
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setUserInfo(ui);
		session.setConfig(config);
		session.setServerAliveInterval(30000);
		session.connect();
	}

	public void changePassword(String username, String newPassword)
			throws JSchException {
		String passwd = " | passwd --stdin " + username;
		String echo = "echo -e \"" + newPassword + '\n' + newPassword +"\"";
		String command = echo + passwd;
		currentChannel = session.openChannel("exec");
		((ChannelExec) currentChannel).setCommand(command);
		currentChannel.setInputStream(null);
		((ChannelExec) currentChannel).setErrStream(System.err);
		currentChannel.connect();
	}

	public void close() {
		session.disconnect();
	}

	public void disconnectChannel() throws IOException {
		if (in != null) {
			in.close();
		}
		if (this.currentChannel != null)
			this.currentChannel.disconnect();
	}

	private void initCat(String fileName) throws JSchException {
		String command = "cat " + fileName;

		currentChannel = session.openChannel("exec");
		((ChannelExec) currentChannel).setCommand(command);

		currentChannel.setInputStream(null);

		((ChannelExec) currentChannel).setErrStream(System.err);
		currentChannel.connect();
	}

	public BufferedReader openFile(String fileName) throws JSchException,
			IOException {
		initCat(fileName);

		InputStream in = currentChannel.getInputStream();
		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader reader = new BufferedReader(isr);

		return reader;
	}
}
