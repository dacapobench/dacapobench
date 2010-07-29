package org.dacapo.util;

import java.io.File;
import java.util.LinkedList;

public final class LogFiles {

	private static final String DEFAULT_EXTENSION        = ".csv";
	
	public static LinkedList<File> orderedLogFileList(String logFileBaseName) {
		LinkedList<File> list           = new LinkedList<File>();
		int              extensionPoint = logFileBaseName.lastIndexOf('.');

		String extension = DEFAULT_EXTENSION;
		String baseName  = logFileBaseName;
		
		if (0 < extensionPoint) {
			extension = logFileBaseName.substring(extensionPoint);
			baseName  = logFileBaseName.substring(0,extensionPoint);
		}
		
		int fileNumber = 0;
		File file = null;
		do {
			file = new File(baseName+"-"+fileNumber+extension);
			if (file.exists() && file.canRead())
				list.add(file);
			else
				file = null;
			fileNumber++;
		} while (file != null);
		
		return list;
	}

}
