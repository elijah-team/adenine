/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */

package edu.mit.lcs.haystack.server.extensions.text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;

import edu.mit.lcs.haystack.ReaderInputStream;

/**
 * This class fulfills the promise for textifying an HTML file.
 * The basic method of extracting the text from an HTML file used here is to
 * remove the characters that appear between the symbols < and > (the HTML
 * tags).  Comments are a major problem, because in older specifications of
 * HTML comments could be delimited by <! and > (not <!-- and --> as they
 * are now).  If a greater than or less than sign appears in a script located
 * within a comment, it can be very difficult to determine whether the sign
 * refers to the beginning or end of a tag, or is part of the script.  When
 * it encounters the beginning of a comment that starts with <!, this class
 * will continue parsing the input as a comment until it reaches the last >
 * that appears before a <.  This may result in an incorrect parsing of the
 * comment when a > and a < appear in that order within a comment.  This style
 * of comment is not included in the specification of HTML 4.0.
 *
 * <P>This code is part of the:
 * <A HREF="http://haystack.lcs.mit.edu/"> Haystack project</A>
 * <BR>Copyright &#169; Massachusetts Institute of Technology
 *
 * @author Dennis Quan
 * @author Damon Mosk-Aoyama (damonma@mit.edu) (original)
 * @author Orion Richardson (orionr@mit.edu)
 */
public class HtmlTextExtractor implements ITextExtractor {

	/**
	 * The line break character.
	 */
	private static final char LINE_BREAK = ((char) 10);

	/**
	 * Contains the HTML tags that the textifier replaces with a line break.
	 */
	private static final String[] MAJOR_BREAK_TAGS =
		{ "H1", "H2", "H3", "P", "TITLE", "BR", "TR" };

	/**
	 * Contains the HTML tags that the textifier replaces with a line break in
	 * situations in which the tag does not appear at the start of a line.
	 */
	private static final String[] MINOR_BREAK_TAGS =
		{ "H4", "H5", "H6", "PRE", "TD", "LI" };

	/**
	 * Contains pairs that specify HTML character entity references, and the
	 * symbols they should be replaced with.
	 */
	private static final String[][] CHAR_ENTITY_REF = { { "nbsp;", " " }, {
			"lt;", "<" }, {
			"gt;", ">" }, {
			"amp;", "&" }, {
			"quot;", "\"" }, {
			"#169;", "(C)" }, {
			"copy;", "(C)" }
	};

	/**
	 * Specifies the size of the character buffer array used to read in data.
	 */
	private static final int ARRAY_SIZE = 8192;

	/**
	 * The Reader that stores the HTML we will be textifying.
	 */
	private transient Reader theReader;

	/**
	 * This array is used to store the characters read in from the Reader.
	 */
	private transient char[] buffer = new char[ARRAY_SIZE];

	/**
	 * Stores the number of characters read in from the Reader.
	 */
	private transient int numChars;

	/** 
	 * Determines the current position of the textifier within the array.
	 */
	private transient int position;

	public InputStream convertToText(InputStream is) {
		try {
			this.theReader = new InputStreamReader(is);
			return new ReaderInputStream(this.convert().toString());
		} catch (Exception e) {
			e.printStackTrace();
			return new ReaderInputStream("");
		}
	}

	/**
	 * Reconstructs an HTML promise object during deserialization.  This method
	 * is used to initialize the transient instance variables of the object.
	 *
	 * @param in The input stream.
	 * @exception IOException Thrown if an I/O error occurs.
	 * @exception ClassNotFoundException Thrown if the class of a serialized
	 * object cannot be found.
	 */
	private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException {

		in.defaultReadObject();

		// Initialize the character array.
		this.buffer = new char[ARRAY_SIZE];
	}

	/**
	 * This method fulfills the promise, returning an InputStream that contains
	 * the promised object.
	 * If fulfill fails, returns null.
	 *
	 * @return an InputStream containing the promised text
	 */

	/*  public InputStream fulfill() {
	
	    InputStream is = TextifyPromise.getInputStreamForID(this.sourceNeedleID);
	
	    if (is == null) {
	      return (is);
	
	    } else {
	
	      try { 
		this.theReader = new InputStreamReader(is);
		return (this.convert());
	
	      } catch (IOException e) {
	//	HaystackRootServer.getRootServer().getLogger().
	//	  printUnanticipatedEvent(this, "HTML TextifyService: could not " + 
	//				  "create ReaderInputStream", e);
		return ((InputStream) null);
	      }
	    }
	  }*/

	/**
	 * This method obtains a file to which the textifier will write its output.
	 *
	 * @return A File to which the textifier will write its output.
	 * @exception IOException Thrown if an output file cannot be created.
	 */
	private File getFile() throws IOException {
		return (File.createTempFile("TextifyHTML", null));
	}

	/**
	 * This method reads in data from the Reader, placing it into the character
	 * array.  The number of characters specified by ARRAY_SIZE is read from the
	 * Reader for one invocation of this method.
	 *
	 * @throws IOException The method throws an IOException if errors are
	 * encountered accessing the Reader.
	 */
	private void readData() throws IOException {
		this.numChars = theReader.read(this.buffer);
		this.position = 0;
	}

	/**
	 * This method gets the next sequence of non-white-space characters from
	 * the character array, stopping when it encounters a > symbol or
	 * white-space.
	 *
	 * @return The method returns a string containing the sequence of
	 * characters taken from the array.
	 * @exception IOException If the method needs to read more characters from
	 * the Reader, and a problem is encountered accessing the Reader, an
	 * IOException is thrown.
	 */
	private String getNextTag() throws IOException {
		StringBuffer strbuff = new StringBuffer("");

		while (Character.isWhitespace(this.buffer[this.position])) {
			this.position++;
			if (this.position == ARRAY_SIZE) {
				this.readData();
				if (this.numChars < 0) {
					return "";
				}
			}
		}
		while (!(Character.isWhitespace(this.buffer[this.position]))
			&& (this.buffer[this.position] != '>')) {
			strbuff.append(this.buffer[this.position]);
			this.position++;
			if (this.position == ARRAY_SIZE) {
				this.readData();
				if (this.position < 0) {
					return strbuff.toString();
				}
			}
		}
		if (this.buffer[this.position] == '>') {
			strbuff.append(">");
		}
		this.position++;
		if (this.position == ARRAY_SIZE) {
			this.readData();
		}
		return strbuff.toString();
	}

	/**
	 * This method converts HTML character entity references (strings that
	 * begin with the & symbol) to their text representations.  Not all HTML
	 * character entity references are supported.
	 *
	 * @param reference A String containing the character entity reference
	 * (without the leading ampersand).
	 * @return The method returns a String that contains the text
	 * representation of the reference.
	 */
	private String charEntity(String reference) {
		for (int i = 0; i < CHAR_ENTITY_REF.length; i++) {
			if (reference.equals(CHAR_ENTITY_REF[i][0])) {
				return CHAR_ENTITY_REF[i][1];
			}
		}
		return ("&" + reference);
	}

	/**
	 * This method gets the next sequence of non-white-space characters from
	 * the character array and returns them as a string, under the assumption
	 * that those characters form a character entity reference (this method is
	 * invoked only when an & has been read to begin a word).
	 *
	 * @return The method returns a string that contains the character entity
	 * reference.
	 * @exception IOException If the method reaches the end of the character
	 * array and reads from the Reader, an exception will be thrown if a
	 * problem occurs when it accesses the Reader.
	 */
	private String getReference() throws IOException {
		StringBuffer str = new StringBuffer("");

		while ((this.buffer[this.position] != '<')
			&& (this.buffer[this.position] != ';')
			&& (!(Character.isWhitespace(this.buffer[this.position])))) {
			str.append(this.buffer[this.position]);
			this.position++;
			if (this.position == ARRAY_SIZE) {
				this.readData();
				if (this.numChars < 0) {
					return str.toString();
				}
			}
		}
		if (this.buffer[this.position] != '<') {
			if (this.buffer[this.position] == ';') {
				str.append(this.buffer[this.position]);
			}
			this.position++;
			if (this.position == ARRAY_SIZE) {
				this.readData();
			}
		}
		return str.toString();
	}

	/**
	 * This method returns true if the parameter is an HTML tag that should be
	 * translated by the textifier to a line break, and false otherwise.
	 * The tags that are replaced with line breaks are specified in the
	 * MAJOR_BREAK_TAGS array.  For each tag in the MAJOR_BREAK_TAGS array,
	 * the corresponding end tag is also replaced with a line break.
	 *
	 * @param tag A string that contains an HTML tag.
	 * @return The method returns a boolean value on the basis of whether the
	 * textifier should replace the tag with a line break in its output.
	 */
	private boolean isMajorBreakTag(String tag) {
		for (int i = 0; i < MAJOR_BREAK_TAGS.length; i++) {
			if ((tag.equalsIgnoreCase(MAJOR_BREAK_TAGS[i]))
				|| (tag.equalsIgnoreCase(MAJOR_BREAK_TAGS[i] + ">"))
				|| (tag.equalsIgnoreCase("/" + MAJOR_BREAK_TAGS[i]))
				|| (tag.equalsIgnoreCase("/" + MAJOR_BREAK_TAGS[i] + ">"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a boolean value that specifies whether the given string is an
	 * HTML tag that should be replaced by a line break when it appears anywhere
	 * except for the beginning of a line.
	 *
	 * @param tag A string that contains an HTML tag.
	 * @return True if the string is a minor break tag, false otherwise.
	 */
	private boolean isMinorBreakTag(String tag) {
		for (int i = 0; i < MINOR_BREAK_TAGS.length; i++) {
			if ((tag.equalsIgnoreCase(MINOR_BREAK_TAGS[i]))
				|| (tag.equalsIgnoreCase(MINOR_BREAK_TAGS[i] + ">"))
				|| (tag.equalsIgnoreCase("/" + MINOR_BREAK_TAGS[i]))
				|| (tag.equalsIgnoreCase("/" + MINOR_BREAK_TAGS[i] + ">"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This is the central method that does the conversion from HTML to text.
	 * It goes through the Reader using a while loop, reading in an array of
	 * characters at a time.  Then it processes the array of characters, one
	 * character at a time in most cases.  When it encounters a <, the loop does
	 * some special processing to determine what type of tag or comment appears
	 * at that point in the HTML, and acts accordingly.
	 *
	 * @return The method returns an input stream that contains the text
	 * obtained from fulfilling the promise.
	 * @exception IOException An IOException is thrown if an output file cannot
	 * be created, or data cannot be read from the Reader or written to the
	 * Writer.
	 */
	private StringBuffer convert() throws IOException {
		StringBuffer temp = new StringBuffer("");
		String word;

		/* lastString is used to store the characters written to the destination
		   file since the last word was written to it. */
		StringBuffer lastString = new StringBuffer("\n");

		/* The variable tagLevel takes on a nonzero value when the characters
		   currently being read from the Reader are located within a tag,
		   and the value zero when the characters should be written out. */
		int tagLevel = 0;

		// This StringBuffer stores characters that will be written to the Writer.
		StringBuffer toWrite = new StringBuffer("");

		do {
			this.readData();
			while (this.position < this.numChars) {
				if (tagLevel == 0) {
					switch (this.buffer[this.position]) {
						case '<' :

							// The beginning of a tag.  Read in the tag:
							this.position++;
							if (this.position == ARRAY_SIZE) {
								this.readData();
							}
							word = this.getNextTag();

							// Determine whether this is a comment:
							if ((word.startsWith("!--"))
								&& (!(word.charAt(word.length() - 1) == '>'))) {

								// Read in words until the end of the comment is reached:
								do {
									word = this.getNextTag();
									if (word.endsWith("--")) {
										while (Character
											.isWhitespace(this.buffer[this.position])) {
											this.position++;
											if (this.position == ARRAY_SIZE) {
												this.readData();
												if (this.numChars < 0) {
													break;
												}
											}
										}
									}
								}
								while (!(((this.buffer[this.position] == '>')
									&& (word.endsWith("--")))
									|| (word.endsWith("-->"))));
								if ((this.buffer[this.position] == '>')
									&& (word.endsWith("--"))) {
									this.position++;
									if (this.position == ARRAY_SIZE) {
										this.readData();
									}
								}
							} else if (
								(word.startsWith("!"))
									&& (word.charAt(word.length() - 1) != '>')) {

								// This is a style of comment used in older versions of HTML.
								do {
									word = this.getNextTag();
									if (word.endsWith(">")) {

										// Determine whether this is the last > in the comment:
										while (this.buffer[this.position] != '<') {
											if (this.buffer[this.position] == '>') {
												temp = new StringBuffer("");
											} else {
												temp.append(this.buffer[this.position]);
											}
											this.position++;
											if (this.position == ARRAY_SIZE) {
												this.readData();
												if (this.numChars < 0) {
													break;
												}
											}
										}
									}
								}
								while (!((this.buffer[this.position] == '<')
									&& (word.endsWith(">"))));
								toWrite.append(temp.toString());
							} else {
								if (word.charAt(word.length() - 1) != '>') {

									// Specify that the following characters are part of a tag:
									tagLevel++;
								}

								// The textifier replaces some tags with white space:
								if (this.isMajorBreakTag(word)) {

									// A major break tag is written to the output unless the
									// previous two characters written were both line breaks.
									if (lastString.charAt(0) == LINE_BREAK) {
										if (lastString.length() == 1) {
											toWrite.append(LINE_BREAK);
											lastString.append(LINE_BREAK);
										}
									} else {
										toWrite.append(LINE_BREAK);
										lastString.setLength(1);
										lastString.setCharAt(0, LINE_BREAK);
									}
								} else if (this.isMinorBreakTag(word)) {

									// A minor break tag is written to the output if the previous
									// character written was not a line break.
									if (lastString.charAt(0) != LINE_BREAK) {
										toWrite.append(LINE_BREAK);
										lastString.setLength(1);
										lastString.setCharAt(0, LINE_BREAK);
									}
								}
							}
							break;
						case '&' :
							this.position++;
							if (this.position == ARRAY_SIZE) {
								this.readData();
							}

							// Determine what character entity reference this is:
							word = this.charEntity(this.getReference());
							toWrite.append(word);
							lastString.append(word);
							break;
						case LINE_BREAK :

							// If the previous character written to the output was a line
							// break, skip this line break.
							if (lastString.charAt(0) != LINE_BREAK) {
								toWrite.append(this.buffer[this.position]);
								lastString.setLength(1);
								lastString.setCharAt(0, this.buffer[this.position]);
							}
							this.position++;
							if (this.position == ARRAY_SIZE) {
								this.readData();
							}
							break;
						default :
							if (Character.isWhitespace(this.buffer[this.position])) {

								// This condenses the white space.
								if (!Character.isWhitespace(lastString.charAt(0))) {
									toWrite.append(this.buffer[this.position]);
									lastString.setLength(1);
									lastString.setCharAt(0, this.buffer[this.position]);
								}
							} else {

								// In this case, the current character is not white space.
								toWrite.append(this.buffer[this.position]);
								if (Character.isWhitespace(lastString.charAt(0))) {
									lastString.setLength(1);
									lastString.setCharAt(0, this.buffer[this.position]);
								} else {
									lastString.append(this.buffer[this.position]);
								}
							}
							this.position++;
							if (this.position == ARRAY_SIZE) {
								this.readData();
							}
							break;
					}
				} else {
					switch (this.buffer[this.position]) {
						case '>' :
							if (tagLevel > 0) {

								// Specify that the end of the tag has been reached:
								tagLevel--;
							}
							break;
						case '<' :
							tagLevel++;
							break;
						default :
							break;
					}
					this.position++;
					if (this.position == ARRAY_SIZE) {
						this.readData();
					}
				}
			}
		}
		while (this.numChars >= 0);
		theReader.close();
		return toWrite;
	}

}
