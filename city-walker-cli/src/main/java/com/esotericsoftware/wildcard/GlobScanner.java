/*
Copyright (c) 2009, Nathan Sweet
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    * Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.wildcard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class GlobScanner {
	private final File rootDir;
	private final List<String> matches = new ArrayList<>(128);

	public GlobScanner(File rootDir, List<String> includes, List<String> excludes, boolean ignoreCase) {
		if (rootDir == null)
			throw new IllegalArgumentException("rootDir cannot be null.");
		if (!rootDir.exists())
			throw new IllegalArgumentException("Directory does not exist: " + rootDir);
		if (!rootDir.isDirectory())
			throw new IllegalArgumentException("File must be a directory: " + rootDir);
		try {
			rootDir = rootDir.getCanonicalFile();
		} catch (IOException ex) {
			throw new RuntimeException("OS error determining canonical path: " + rootDir, ex);
		}
		this.rootDir = rootDir;

		if (includes == null)
			throw new IllegalArgumentException("includes cannot be null.");
		if (excludes == null)
			throw new IllegalArgumentException("excludes cannot be null.");

		if (includes.isEmpty())
			includes.add("**");
		List<Pattern> includePatterns = new ArrayList<>(includes.size());
		for (String include : includes)
			includePatterns.add(new Pattern(include, ignoreCase));

		List<Pattern> allExcludePatterns = new ArrayList<>(excludes.size());
		for (String exclude : excludes)
			allExcludePatterns.add(new Pattern(exclude, ignoreCase));

		scanDir(rootDir, includePatterns);

		if (!allExcludePatterns.isEmpty()) {
			// For each file, see if any exclude patterns match.
			outerLoop:
			//
			for (Iterator<String> matchIter = matches.iterator(); matchIter.hasNext();) {
				String filePath = (String) matchIter.next();
				List<Pattern> excludePatterns = new ArrayList<>(allExcludePatterns);
				try {
					// Shortcut for excludes that are "**/XXX", just check file name.
					for (Iterator<Pattern> excludeIter = excludePatterns.iterator(); excludeIter.hasNext();) {
						Pattern exclude = (Pattern) excludeIter.next();
						if (exclude.values.length == 2 && exclude.values[0].equals("**")) {
							exclude.incr();
							String fileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1);
							if (exclude.matchesFile(fileName)) {
								matchIter.remove();
								continue outerLoop;
							}
							excludeIter.remove();
						}
					}
					// Get the file names after the root dir.
					String[] fileNames = filePath.split("\\" + File.separator);
					for (String fileName : fileNames) {
						for (Iterator<Pattern> excludeIter = excludePatterns.iterator(); excludeIter.hasNext();) {
							Pattern exclude = (Pattern) excludeIter.next();
							if (!exclude.matchesFile(fileName)) {
								excludeIter.remove();
								continue;
							}
							exclude.incr(fileName);
							if (exclude.wasFinalMatch()) {
								// Exclude pattern matched.
								matchIter.remove();
								continue outerLoop;
							}
						}
						// Stop processing the file if none of the exclude patterns matched.
						if (excludePatterns.isEmpty())
							continue outerLoop;
					}
				} finally {
					for (Pattern exclude : allExcludePatterns)
						exclude.reset();
				}
			}
		}
	}

	private void scanDir(File dir, List<Pattern> includes) {
		if (!dir.canRead())
			return;

		// See if patterns are specific enough to avoid scanning every file in the
		// directory.
		boolean scanAll = false;
		for (Pattern include : includes) {
			if (include.value.indexOf('*') != -1 || include.value.indexOf('?') != -1) {
				scanAll = true;
				break;
			}
		}

		if (!scanAll) {
			// If not scanning all the files, we know exactly which ones to include.
			List<Pattern> matchingIncludes = new ArrayList<>(1);
			for (Pattern include : includes) {
				if (matchingIncludes.isEmpty())
					matchingIncludes.add(include);
				else
					matchingIncludes.set(0, include);
				process(dir, include.value, matchingIncludes);
			}
		} else {
			// Scan every file.
			String[] fileNames = dir.list();
			if (fileNames == null)
				return;
			for (String fileName : fileNames) {
				// Get all include patterns that match.
				List<Pattern> matchingIncludes = new ArrayList<>(includes.size());
				for (Pattern include : includes)
					if (include.matchesFile(fileName))
						matchingIncludes.add(include);
				if (matchingIncludes.isEmpty())
					continue;
				process(dir, fileName, matchingIncludes);
			}
		}
	}

	private void process(File dir, String fileName, List<Pattern> matchingIncludes) {
		// Increment patterns that need to move to the next token.
		boolean isFinalMatch = false;
		List<Pattern> incrementedPatterns = new ArrayList<>();
		for (Iterator<Pattern> iter = matchingIncludes.iterator(); iter.hasNext();) {
			Pattern include = (Pattern) iter.next();
			if (include.incr(fileName)) {
				incrementedPatterns.add(include);
				if (include.isExhausted())
					iter.remove();
			}
			if (include.wasFinalMatch())
				isFinalMatch = true;
		}

		File file = new File(dir, fileName);
		if (isFinalMatch) {
			int length = rootDir.getPath().length();
			if (!rootDir.getPath().endsWith(File.separator))
				length++; // Lose starting slash.
			matches.add(file.getPath().substring(length));
		}
		if (!matchingIncludes.isEmpty() && file.isDirectory())
			scanDir(file, matchingIncludes);

		// Decrement patterns.
		for (Pattern include : incrementedPatterns)
			include.decr();
	}

	public List<String> matches() {
		return matches;
	}

	public File rootDir() {
		return rootDir;
	}

	static class Pattern {
		String value;
		boolean ignoreCase;
		final String[] values;

		private int index;

		Pattern(String pattern, boolean ignoreCase) {
			this.ignoreCase = ignoreCase;

			pattern = pattern.replace('\\', '/');
			pattern = pattern.replaceAll("\\*\\*(?=[^/])", "**/*");
			pattern = pattern.replaceAll("(?<=[^/])\\*\\*", "*/**");
			if (ignoreCase)
				pattern = pattern.toLowerCase();

			values = pattern.split("/");
			value = values[0];
		}

		boolean matchesPath(String path) {
			reset();
			String[] files = path.split("[\\\\/]");
			for (int i = 0, n = files.length; i < n; i++) {
				String file = files[i];
				if (i > 0 && isExhausted())
					return false;
				if (!matchesFile(file))
					return false;
				if (!incr(file) && isExhausted())
					return true;
			}
			return wasFinalMatch();
		}

		boolean matchesFile(String fileName) {
			String value = this.value;
			if (value.equals("**"))
				return true;

			if (ignoreCase)
				fileName = fileName.toLowerCase();

			// Shortcut if no wildcards.
			if (value.indexOf('*') == -1 && value.indexOf('?') == -1)
				return fileName.equals(value);

			int i = 0, j = 0, fileNameLength = fileName.length(), valueLength = value.length();
			while (i < fileNameLength && j < valueLength) {
				char c = value.charAt(j);
				if (c == '*')
					break;
				if (c != '?' && c != fileName.charAt(i))
					return false;
				i++;
				j++;
			}

			// If reached end of pattern without finding a * wildcard, the match has to fail
			// if not same length.
			if (j == valueLength)
				return fileNameLength == valueLength;

			int cp = 0;
			int mp = 0;
			while (i < fileNameLength) {
				if (j < valueLength) {
					char c = value.charAt(j);
					if (c == '*') {
						if (j++ >= valueLength)
							return true;
						mp = j;
						cp = i + 1;
						continue;
					}
					if (c == '?' || c == fileName.charAt(i)) {
						j++;
						i++;
						continue;
					}
				}
				j = mp;
				i = cp++;
			}

			// Handle trailing asterisks.
			while (j < valueLength && value.charAt(j) == '*')
				j++;

			return j >= valueLength;
		}

		String nextValue() {
			if (index + 1 == values.length)
				return null;
			return values[index + 1];
		}

		boolean incr(String fileName) {
			if (value.equals("**")) {
				if (index == values.length - 1)
					return false;
				incr();
				if (matchesFile(fileName))
					incr();
				else {
					decr();
					return false;
				}
			} else
				incr();
			return true;
		}

		void incr() {
			index++;
			if (index >= values.length)
				value = null;
			else
				value = values[index];
		}

		void decr() {
			index--;
			if (index > 0 && values[index - 1].equals("**"))
				index--;
			value = values[index];
		}

		void reset() {
			index = 0;
			value = values[0];
		}

		boolean isExhausted() {
			return index >= values.length;
		}

		boolean isLast() {
			return index >= values.length - 1;
		}

		boolean wasFinalMatch() {
			return isExhausted() || (isLast() && value.equals("**"));
		}
	}

	public static void main(String[] args) {
		List<String> includes = new ArrayList<>();
		includes.add("src/**.java");
		List<String> excludes = new ArrayList<>();
		long start = System.nanoTime();
		List<String> files = new GlobScanner(new File("."), includes, excludes, false).matches();
		long end = System.nanoTime();
		System.out.println(files.toString().replaceAll(", ", "\n").replaceAll("[\\[\\]]", ""));
		System.out.println((end - start) / 1000000f);
		System.out.println(files);
	}
}
