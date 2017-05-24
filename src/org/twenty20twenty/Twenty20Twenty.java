/*
 * Copyright 2014 Anto Paul
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.twenty20twenty;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Twenty20Twenty {

	protected static TrayIcon trayIcon = null;

	protected static TimerTask reminder = null;
	protected static Timer timer = null;

	public static long interval = 20 * 1000 * 60;
	
	protected static RandomAccessFile lockFile = null;
	
	protected static boolean isSilentMode = false;
	
	static final String aboutMessage = "The 20-20-20 Rule "
			+ "for the Eye Reminder application\r\n"
			+ "_________________________________________________________\r\n"
			+ "Follow this rule to reduce eye strain when working in front of a computer.\r\n"
			+ "Every 20 minutes look at something 20 feet away for "
			+ "20 seconds.\r\n"
			+ "_________________________________________________________"
			+ "\r\nhttps://github.com/antopaul/20-20-20";

	public static void main(String[] args) {
		
		if(isAlreadyRunning()) {
			System.out.println("Already running. Exiting...");
			System.exit(0);
		}
		createLock();
		
		/* Use an appropriate Look and Feel */
		try {
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (InstantiationException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		/* Turn off metal's use of bold fonts */
		UIManager.put("swing.boldMetal", Boolean.FALSE);
		// Schedule a job for the event-dispatching thread:
		// adding TrayIcon.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
		setTimer();
	}

	private static void createAndShowGUI() {
		// Check the SystemTray support
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}
		final PopupMenu popup = new PopupMenu();
		final Image image = createImage("/images/eye.gif", "20-20-20 tray icon");
		trayIcon = new TrayIcon(image);
		final SystemTray tray = SystemTray.getSystemTray();

		// Create a popup menu components
		CheckboxMenuItem silentModeItem = new CheckboxMenuItem("Silent Mode");
		
		MenuItem aboutItem = new MenuItem("About...");
		
		MenuItem exitItem = new MenuItem("Exit");
		
		// Add components to popup menu
		popup.add(silentModeItem);
		popup.addSeparator();
		popup.add(aboutItem);
		popup.addSeparator();
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);

		trayIcon.setToolTip("20-20-20 Rule For The Eye");
		
		silentModeItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.DESELECTED) {
					isSilentMode = false;
					setTimer();
				}
				if(e.getStateChange() == ItemEvent.SELECTED) {
					isSilentMode = true;
					cancelTimer();
				}
			}
		});
		
		ActionListener aboutListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Icon icon = new ImageIcon(image);
				JOptionPane.showMessageDialog(null, aboutMessage, 
						"About 20-20-20", JOptionPane.PLAIN_MESSAGE, icon);
			}
        };

		aboutItem.addActionListener(aboutListener);

		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				releaseLock();
				tray.remove(trayIcon);
				timer.cancel();
				//System.exit(0);
			}
		});
		
		trayIcon.addActionListener(aboutListener);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			System.out.println("TrayIcon could not be added.");
			return;
		}
	}

	// Obtain the image URL
	protected static Image createImage(String path, String description) {
		URL imageURL = Twenty20Twenty.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} else {
			return (new ImageIcon(imageURL, description)).getImage();
		}
	}

	protected static void setTimer() {
		reminder = new TimerTask() {
			public void run() {
				trayIcon.displayMessage("20-20-20", "It's 20-20-20 time",
						TrayIcon.MessageType.INFO);
			}
		};

		timer = new Timer();
		timer.schedule(reminder, interval, interval);
	}
	
	protected static void cancelTimer() {
		timer.cancel();
	}
	
	protected static FileLock createLock() {
		String temp = System.getProperty("java.io.tmpdir");
		File file = new File(temp, "Twenty2020.tmp");
		file.deleteOnExit();
		
		FileChannel fileChannel = null;
		FileLock lock = null;
		
		try {
			lockFile = new RandomAccessFile(file,"rw");
			fileChannel = lockFile.getChannel();
			lock = fileChannel.tryLock();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lock;
	}
	
	protected static void releaseLock() {
		if(lockFile != null) {
			try {
				lockFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected static boolean isAlreadyRunning() throws RuntimeException {

		String temp = System.getProperty("java.io.tmpdir");
		File file = new File(temp, "Twenty2020.tmp");
		file.deleteOnExit();
		FileChannel fileChannel = null;
		
		try {
			lockFile = new RandomAccessFile(file,"rw");
			fileChannel = lockFile.getChannel();
			FileLock lock = fileChannel.tryLock();
			
			if(lock == null) {
				return true;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(fileChannel != null) {
				try {
					fileChannel.close();
					lockFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return false;
		
	}
}