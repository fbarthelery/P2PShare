/*
    Copyright (C) 2012 by GENYMOBILE and Frederic-Charles Barthelery
    fbarthelery@genymobile.com
    http://www.genymobile.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package com.genymobile.demo.p2pshare;

import java.io.File;
import java.net.URLDecoder;

public class FileUtils {

	public static File getOuputFile(File outDir, String fileres) {
		String decodedFile = getReadableFilename(fileres);
		boolean exist = true;
		File candidate = null;
		int version = 0;
		while (exist) {
			 candidate = new File(outDir, incrementFilename(decodedFile, version));
			 if (!candidate.exists())
				 exist = false;
			 version++;
		}
		return candidate;
	}
	
	public static String getReadableFilename(String fileres ) {
		String decodedFile = URLDecoder.decode(fileres);
		int idx = decodedFile.lastIndexOf('/');
		if (idx != -1)
			decodedFile = decodedFile.substring(idx);
		return decodedFile;
	}
	
	private static String incrementFilename(String filename, int version) {
		if (version == 0)
			return filename;
		int idx = filename.lastIndexOf('.');
		if (idx != -1) {
			String basename = filename.substring(0, idx);
			String ext = filename.substring(idx);
			return basename + " (" + version +")" + ext;
		} else {
			return filename + " (" + version + ")";
		}
		
	}
	
}
