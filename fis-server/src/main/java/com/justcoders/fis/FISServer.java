package com.justcoders.fis;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FISServer {

	private static final String PWD = "pwd";

	private static final String INFO = "info";

	private static final String DIR = "dir";

	private static final String CD = "cd";

	private ServerSocket serverSocket;

	private static final String NEW_LINE = "\n";
	private static final String USER_DIR = "user.dir";
	// socket server port on which it will listen
	private static int port = 6655;

	private static Set<String> VALID_COMMANDS = new HashSet<>();
	static {
		VALID_COMMANDS.add("connection");
		VALID_COMMANDS.add(DIR);
		VALID_COMMANDS.add(INFO);
		VALID_COMMANDS.add(PWD);
		VALID_COMMANDS.add(CD);
		VALID_COMMANDS.add("quit");
	}

	private static Map<String, String> RESPONSE_CODES = new HashMap<>();
	static {
		RESPONSE_CODES.put("success", "200");
		RESPONSE_CODES.put("warning", "300");
		RESPONSE_CODES.put("error", "400");
	}
	

	public static void main(String[] args) {
		try {
			String ipAddress = args != null && args.length > 0 ? args[0] : null;
			FISServer app = new FISServer(ipAddress);
			app.listen();
			System.out.println("\r\nRunning Server: " + "Host=" + app.getSocketAddress().getHostAddress() + " Port="
					+ app.getPort());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FISServer(String ipAddress) throws UnknownHostException, IOException {
		if (null != ipAddress && !ipAddress.isEmpty()) {
			InetAddress inetAddress = InetAddress.getByName(ipAddress);
			this.serverSocket = new ServerSocket(port, 1, inetAddress);
			System.out.println("Server started on the address:" + inetAddress);
		} else {
			InetAddress localHost = InetAddress.getLocalHost();
			this.serverSocket = new ServerSocket(port, 1, localHost);
			System.out.println("200 Welcome to File Information Service");
			System.out.println("Server started on the address:" + localHost);
		}
	}

	private synchronized void listen() throws IOException {
		try {
			while (true) {
				System.out.println("Waiting for client request");
				// creating socket and waiting for client connection
				Socket socket = serverSocket.accept();
				// read from socket to ObjectInputStream object
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				// create ObjectOutputStream object
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

				try {
					// convert ObjectInputStream object to String
					String command = (String) ois.readObject();
					System.out.println("Command : " + command);

					String[] splitCommands = splitCommands(command);
					if (null != splitCommands && splitCommands.length > 0) {

						String sendOutput = null;
						if ("quit".equalsIgnoreCase(command)) {
							sendOutput = "200 Goodbye!";
						} else {
							String readCommandOutput = getCommandOutput(command, splitCommands);
							sendOutput = readCommandOutput.concat("\n200 Ok");
						}

						oos.writeObject(sendOutput);
						// close resources
						ois.close();
						oos.close();
						socket.close();
					} else {
						// write object to Socket
						String warning = "Enter valid command";
						String concat = warning.concat("\n300 Warning! Command cannot be empty or null");
						oos.writeObject(concat);
						// close resources
						ois.close();
						oos.close();
						socket.close();
					}

					// terminate the server if client sends exit request
					if (command.equalsIgnoreCase("exit")) {
						break;
					}
				} catch (Exception e) {
					System.out.println("Exception occured in fis server");
					String error = "Something went wrong in server, please try again later";
					String concat = error.concat("\n500 Error!");
					oos.writeObject(concat);
					// close resources
					ois.close();
					oos.close();
					socket.close();
				}
			}
			System.out.println("Shutting down Socket server!!");
			serverSocket.close();

		} catch (Exception e) {
			System.out.println("Exception occured in server socket");
		}
	}

	private static String[] splitCommands(String command) {
		if (command != null) {
			String[] split = command.split(" ");
			if (null != split && split.length > 0) {
				if (!VALID_COMMANDS.contains(split[0])) {
					return null;
				}
				return split;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/*
	 * 
	 */
	private static String getCommandOutput(String commandToExe, String[] commandArgs)
			throws IOException, InterruptedException {
		if (commandToExe == null || commandToExe.isEmpty()) {
			return "Invalid Command";
		}

		StringBuffer commandOutput = new StringBuffer();

		if (CD.equalsIgnoreCase(commandArgs[0])) {
			changeDir(commandToExe, commandArgs, commandOutput);
		} else if (DIR.equalsIgnoreCase(commandArgs[0])) {
			listFiles(commandOutput);
		} else if (INFO.equals(commandArgs[0])) {
			getFileInfo(commandArgs, commandOutput);
		} else if (PWD.equals(commandArgs[0])) {
			getPwd(commandArgs, commandOutput);
		} else {
			commandOutput.append("Invalid command\n");
			commandOutput.append("300 Warning");
		}
		return commandOutput.toString();

	}

	private static void getPwd(String[] commands, StringBuffer commandOutput) {
		String currDir = System.getProperty(USER_DIR);
		commandOutput.append(currDir);
	}

	private static void getFileInfo(String[] commands, StringBuffer dataFromCommand) {
		String currDir = System.getProperty(USER_DIR);

		Path path = Paths.get(currDir + File.separator + commands[1]);

		LinkOption options = LinkOption.NOFOLLOW_LINKS;

		if (!Files.exists(path, options)) {
			dataFromCommand.append("501 File not found");
			return;
		}

		File file = path.toFile();
		if (file.isFile()) {
			dataFromCommand.append("Type:File").append(NEW_LINE);
		} else {
			dataFromCommand.append("Type:Directory").append(NEW_LINE);
		}

		BasicFileAttributes readAttributes;
		try {
			readAttributes = Files.readAttributes(path, BasicFileAttributes.class);
			dataFromCommand.append("Created:").append(readAttributes.creationTime()).append(NEW_LINE);
			dataFromCommand.append("Size:").append(readAttributes.size()).append(NEW_LINE);
		} catch (IOException e) {
			dataFromCommand.append("500 server error occured try again!");
		}
	}

	private static void listFiles(StringBuffer dataFromCommand) {
		String currDir = System.getProperty(USER_DIR);

		File file = new File(currDir);

		if (file.isDirectory()) {
			for (String fileName : file.list()) {
				dataFromCommand.append(fileName).append(NEW_LINE);
			}
		}
	}

	private static void changeDir(String commandToExe, String[] commands, StringBuffer dataFromCommand) {
		String currDir = System.getProperty(USER_DIR);
		System.out.println(currDir);
		
		StringBuffer changeDir = new StringBuffer();
		
		for(int i=1; i<commands.length; i++) {
			changeDir.append(commands[i]);
		}

		String pathname = currDir + File.separator + changeDir.toString();
		Path path = null;
		try {
			path = Paths.get(pathname);
		} catch (Exception e) {
			System.out.println("Invalid path to change directory");
		}

		String changedDir = currDir;
		if (path != null && Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			System.setProperty(USER_DIR, path.toAbsolutePath().toString());
			changedDir = path.toAbsolutePath().toString();
		} else {
			try {
				path = Paths.get(changeDir.toString());
			} catch (Exception exception) {
				System.out.println("Invalid path to change directory");
			}

			if (path != null && Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
				System.setProperty(USER_DIR, path.toAbsolutePath().toString());
				changedDir = path.toAbsolutePath().toString();
			} else {
				changedDir = "500 Invalid dir to change";
			}
		}
		dataFromCommand.append(changedDir);
	}

	public InetAddress getSocketAddress() {
		return this.serverSocket.getInetAddress();
	}

	public int getPort() {
		return this.serverSocket.getLocalPort();
	}

}
