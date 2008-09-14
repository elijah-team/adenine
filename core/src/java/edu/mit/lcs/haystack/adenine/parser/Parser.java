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

package edu.mit.lcs.haystack.adenine.parser;

import edu.mit.lcs.haystack.adenine.*;

import java.io.*;
import java.util.*;

class InputLine {
	InputLine(String s, int i) {
		m_str = s;
		m_lineno = i;
	}
	
	String m_str;
	int m_lineno;
	int m_pos = 0;
}

/**
 * Adenine parser.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Parser {
	static public void main(String args[]) {
		try {
			ArrayList al = tokenize(new FileReader(args[0]));
			System.out.println(blockify(al).prettyPrint(-1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String escapeQuotes(StringBuffer tq) {
		return escapeQuotes(tq.toString());
	}

	public static String escapeQuotes(String tq) {
		// Translate instances of " to \"
		StringBuffer tqNew = new StringBuffer();
		for (int i = 0; i < tq.length(); i++) {
			char ch = tq.charAt(i);
			if (ch == '\"') {
				tqNew.append("\\\"");
			} else if (ch == '\'') {
				tqNew.append("\\\'");
			} else if (ch == '\\') {
				if (i < (tq.length() - 1)) {
					tqNew.append(ch);
					tqNew.append(tq.charAt(++i));
				} else {
					tqNew.append(ch);
				}
			} else {
				tqNew.append(ch);
			}
		}
		return tqNew.toString();
	}
	
	public static ArrayList tokenize(Reader input) throws AdenineException {
		ArrayList tokens = new ArrayList();
		BufferedReader reader = new BufferedReader(input);
		String line;
		int lineno = 0;
		ArrayList lines = new ArrayList();
		try {
			while ((line = reader.readLine()) != null) {
				// Skip completely blank lines
				String l2 = line.trim();
				if (l2.length() == 0) {
					++lineno;
					continue;
				}
				
				// Skip comments
				if (l2.charAt(0) == '#') {
					++lineno;
					continue;
				}
				
				// Find triple-quoted string literals
				int tq1 = line.indexOf("\'\'\'");
				int tq2 = line.indexOf("\"\"\"");
				int tqStart = -1;
				String tqDelimiter = null;
				if (tq1 != -1) {
					if ((tq2 != -1) && (tq2 < tq1)) {
						tq1 = -1;
					} else {
						tq2 = -1;
						tqDelimiter = "\'\'\'";
						tqStart = tq1;
					}
				}

				if (tq2 != -1) {
					tqDelimiter = "\"\"\"";
					tqStart = tq2;
				}
				
				if (tqStart != -1) {
					StringBuffer tq = new StringBuffer();
					
					int tqEnd = line.indexOf(tqDelimiter, tqStart + 3);
					if (tqEnd != -1) {
						// All on one line
						line = line.substring(0, tqStart) + tqDelimiter.charAt(0) + escapeQuotes(line.substring(tqStart + 3, tqEnd)) + tqDelimiter.charAt(0) + line.substring(tqEnd + 3);
					} else {
						String startLine = line;
						tq.append(line.substring(tqStart + 3));
						tq.append("\\\n");
						while ((line = reader.readLine()) != null) {
							++lineno;
							tqEnd = line.indexOf(tqDelimiter);
							if (tqEnd != -1) {
								tq.append(line.substring(0, tqEnd));
								break;
							}
							tq.append(line);
							tq.append("\\\n");
						}
						if (line == null) {
							throw new SyntaxException("Unexpected end of file found in " + tqDelimiter + " expression");
						}
						
						line = startLine.substring(0, tqStart) + tqDelimiter.charAt(0) + escapeQuotes(tq) + tqDelimiter.charAt(0) + line.substring(tqEnd + 3);
					}
				}
				
				// Look for continuation markers
				StringBuffer line2 = new StringBuffer();
				boolean inContinuation = false;
				do {
					++lineno;
					int len = line.length();

					if (!inContinuation && (len > 1) && (line.indexOf("\\\\") == 0)) {
						inContinuation = true;
						line = line.substring(2);
						len -= 2;
					}
					
					if (!inContinuation) {
						if ((len > 0) && (line.charAt(len - 1) == '\\')) {
							line2.append(line.substring(0, len - 1));
							line2.append(" ");
						} else {
							line2.append(line);
							break;
						}
					} else {
						if ((len > 1) && (line.charAt(len - 1) == '\\') &&
							(line.charAt(len - 2) == '\\')) {
							inContinuation = false;
							line = line.substring(0, len - 2);
							line2.append(line);
							break;
						}
						line2.append(line);
						line2.append(" ");
					}
				} while ((line = reader.readLine()) != null);
				
				// Strip inline comments
				String line3 = line2.toString();
/*				int pound = line3.lastIndexOf('#');
				if (pound != -1) {
					line3 = line3.substring(0, pound);
				}*/
				
				lines.add(new InputLine(line3, lineno));
			}
		} catch (IOException ioe) {}

		// Process lines
		Iterator i = lines.iterator();
		while (i.hasNext()) {
			InputLine il = (InputLine)i.next();
			tokenizeHelper(il, i, tokens, false, false, false);
		}

		/*StringBuffer sb = new StringBuffer();
		Iterator j = tokens.iterator();
		while (j.hasNext()) {
			sb.append(j.next().toString());
			sb.append(" ");
		}
		System.out.println(sb.toString());*/

		return tokens;
	}
	
	static boolean isURIIdentifier(String str) {
		return (str.indexOf(":") != -1) || (str.indexOf("?") == 0);
	}

	static Token convertBackQuotes(Token token) throws AdenineException {
		ArrayList al = new ArrayList();
		al.add(token);
		al = convertBackQuotes(al);
		return (Token)al.get(0);
	}
	
	static ArrayList convertBackQuotes(ArrayList tokens) throws AdenineException {
		ListIterator i = tokens.listIterator(tokens.size());
		ArrayList output = new ArrayList();
		while (i.hasPrevious()) {
			Token token = (Token)i.previous();
			
			if (token instanceof BackQuoteToken) {
				if (output.size() == 0) {
					throw new SyntaxException("Invalid backquote", token.m_line);
				}
				
				Token token2 = (Token)output.get(0);
				if ((token2 instanceof IndentToken) || (token2 instanceof SemicolonToken)) {
					throw new SyntaxException("Invalid backquote", token.m_line);
				} else {
					((BackQuoteToken)token).setToken(token2);
					output.set(0, token);
				}
			} else if (token instanceof ContainerToken) {
				ContainerToken ct = (ContainerToken)token;
				ct.m_tokens = convertBackQuotes(ct.m_tokens);
				output.add(0, ct);
			} else if (token instanceof DereferencementToken) {
				DereferencementToken dt = (DereferencementToken)token;
				dt.m_base = convertBackQuotes(dt.m_base);
				dt.m_member = convertBackQuotes(dt.m_member);
				output.add(0, dt);
			} else if (token instanceof IndexToken) {
				IndexToken it = (IndexToken)token;
				it.m_base = convertBackQuotes(it.m_base);
				it.m_index = convertBackQuotes(it.m_index);
				output.add(0, it);
			} else {
				output.add(0, token);
			}
		}
		return output;
	}

	static ArrayList convertDotsBrackets(ArrayList tokens) throws AdenineException {
		ArrayList al = new ArrayList();
		Stack s = new Stack();
		for (int i = tokens.size() - 1; i >= 0; i--) {
			s.push(tokens.get(i));
		}
		Token last = null;
		while (!s.isEmpty()) {
			Token t = (Token)s.pop();
			
			if (t instanceof ContainerToken) {
				ContainerToken ct = (ContainerToken)t;
				ct.m_tokens = convertDotsBrackets(ct.m_tokens);
			}
			
			if (t instanceof IdentifierToken) {
				// Filter out tokens that are really URIs
				if (!isURIIdentifier(t.m_token)) {
					int i = t.m_token.indexOf(".");
					
					// Ignore if this is a number
					try {
						Double.parseDouble(t.m_token);
					} catch(NumberFormatException e) {
						if (t.m_token.equals(".")) {
							if (last == null) {
								throw new SyntaxException("Dereferenced expression missing base", t.m_line);
							} else if (last instanceof DereferencementToken) {
								DereferencementToken dt = (DereferencementToken)last;
								if (dt.m_member == null) {
									throw new SyntaxException("Dereferenced expression incomplete", t.m_line);
								}
							}
							
							// Convert to DereferencementToken
							last = new DereferencementToken(last, null);
							continue;
						} else if (i != -1) {
							// Break up token
							if (i == 0) {
								s.push(new IdentifierToken(t.m_token.substring(1), t.m_line));
								s.push(new IdentifierToken(".", t.m_line));
							} else {
								if (i != (t.m_token.length() - 1)) {
									s.push(new IdentifierToken(t.m_token.substring(i + 1), t.m_line));
								}
								s.push(new IdentifierToken(".", t.m_line));
								s.push(new IdentifierToken(t.m_token.substring(0, i), t.m_line));
							}
							continue;
						}
					}
				}
			} else if (t instanceof BracketedToken) {
				if (last == null) {
					throw new SyntaxException("Indexed expression missing base", t.m_line);
				}
				
				if (last instanceof DereferencementToken) {
					DereferencementToken dt = (DereferencementToken)last;
					if (dt.m_member == null) {
						throw new SyntaxException("Dereferenced expression incomplete", t.m_line);
					}
				}

				BracketedToken bt = (BracketedToken)t;
				if (bt.m_tokens.size() != 1) {
					throw new SyntaxException("Only one index supported on an object", bt.m_line);
				}

				// Convert to IndexToken
				last = new IndexToken(last, (Token)bt.m_tokens.get(0));
				continue;
			} 

			if (last != null) {
				if (last instanceof DereferencementToken) {
					DereferencementToken dt = (DereferencementToken)last;
					if (dt.m_member != null) {
						al.add(last);
						last = t;
					} else {
						dt.m_member = t;
					}
				} else {
					al.add(last);
					last = t;
				}
			} else {
				last = t;
			}
		}
		
		if (last != null) {
			if (last instanceof DereferencementToken) {
				DereferencementToken dt = (DereferencementToken)last;
				if (dt.m_member == null) {
					throw new SyntaxException("Dereferenced expression incomplete", dt.m_line);
				}
			}
			al.add(last);
		}
		
		return al;
	}

	static String special = " \t@{}[]\"();<>$%\'`";
	
	static InputLine tokenizeHelper(InputLine il, Iterator ili, ArrayList tokens, boolean inParens, boolean inBrackets, boolean inBraces) throws AdenineException {
		int startLine = il.m_lineno;

		if (il.m_pos == 0) {
			// Process indentation
			IndentToken it = new IndentToken(il.m_lineno);
			il.m_str = it.processIndent(il.m_str);
			tokens.add(it);
		}
		
		// Process line
		String ident = "";
		boolean dollar = false;
		boolean atSign = false;
		boolean percent = false;
		while (true) {
			char ch = ' ';
			if (il.m_pos >= il.m_str.length()) {
				if (inBraces || inParens || inBrackets) {
					try {
						il = (InputLine)ili.next();
					} catch (java.util.NoSuchElementException nsee) {
						char chErr = (inBraces ? '}' : (inParens ? ')' : ']'));
						throw new SyntaxException("Unexpected end of file searching for closing " + chErr + " opened on line " + startLine, il.m_lineno);
					}
				} else {
					break;
				}
			} else {
				ch = il.m_str.charAt(il.m_pos++);
			}
			
			if (atSign && ch != '(') {
				ident = ident + '@';
				atSign = false;
			} 
			
			if (special.indexOf(ch) == -1) {
				ident = ident + ch;
			} else {
				if (ident.length() > 0) {
					tokens.add(new IdentifierToken(ident, il.m_lineno));
					ident = "";
				}
				
				if (ch == '$') {
					if (dollar) {
						throw new SyntaxException("Double $$", il.m_lineno);
					}
					dollar = true;
					percent = false;
					atSign = false;
					continue;
				}

				if (ch == '%') {
					if (percent) {
						throw new SyntaxException("Double %%", il.m_lineno);
					}
					percent = true;
					dollar = false;
					atSign = false;
					continue;
				}

				if (ch == '@') {
					if (atSign) {
						throw new SyntaxException("Double @@", il.m_lineno);
					}
					atSign = true;
					dollar = false;
					percent = false;
					continue;
				}

				if ((dollar || percent) && ch != '{') {
					throw new SyntaxException("Misplaced " + (percent ? "%" : "$"), il.m_lineno);
				}
				
				switch (ch) {
					case '{':
					{
						BracedToken bt = new BracedToken(il.m_lineno);
						bt.m_dollar = dollar;
						bt.m_percent = percent;
						il = tokenizeHelper(il, ili, bt.m_tokens, false, false, true);
						tokens.add(bt);
						break;
					}
					
					case ';':
						tokens.add(new SemicolonToken(il.m_lineno));
						break;
					
					case '`':
						tokens.add(new BackQuoteToken(il.m_lineno));
						break;
					
					case '>':
						throw new SyntaxException("Mismatched >", il.m_lineno);
					
					case '}':
						if (!inBraces) {
							throw new SyntaxException("Mismatched }", il.m_lineno);
						}
						return il;
						
					case '(':
					{
						ParenthesizedToken pt = new ParenthesizedToken(il.m_lineno);
						if (atSign) {
							pt.m_prefix = '@';
						}
						il = tokenizeHelper(il, ili, pt.m_tokens, true, false, false);
						tokens.add(pt);
						break;
					}
					
					case ')':
						if (!inParens) {
							throw new SyntaxException("Mismatched )", il.m_lineno);
						}
						return il;
						
					case '[':
					{
						BracketedToken bt = new BracketedToken(il.m_lineno);
						il = tokenizeHelper(il, ili, bt.m_tokens, false, true, false);
						tokens.add(bt);
						break;
					}
					
					case ']':
						if (!inBrackets) {
							throw new SyntaxException("Mismatched ]", il.m_lineno);
						}
						return il;
						
					case '<':
					{
						URIToken urit = new URIToken(il.m_lineno);
						il.m_pos = urit.processToken(il.m_str, il.m_pos);
						tokens.add(urit);
						break;
					}
					
					case '\"':
					{
						LiteralToken lt = new LiteralToken(il.m_lineno);
						il.m_pos = lt.processToken(il.m_str, il.m_pos);
						tokens.add(lt);
						break;
					}
					
					case '\'':
					{
						StringToken lt = new StringToken(il.m_lineno);
						il.m_pos = lt.processToken(il.m_str, il.m_pos);
						tokens.add(lt);
						break;
					}
				}
				
				atSign = false;
				percent = false;
				dollar = false;
			}
		}
		
		if (inBraces) {
			throw new SyntaxException("{ without matching }", il.m_lineno);
		}
		
		if (inParens) {
			throw new SyntaxException("( without matching )", il.m_lineno);
		}
		
		if (inBrackets) {
			throw new SyntaxException("[ without matching ]", il.m_lineno);
		}
		
		if (dollar) {
			throw new SyntaxException("Extra $ at end of line", il.m_lineno);
		}
		
		if (ident.length() > 0) {
			tokens.add(new IdentifierToken(ident, il.m_lineno));
		}

		return il;
	}
	
	static public Block blockify(ArrayList tokens) throws AdenineException {
		Iterator i = tokens.iterator();
		
		// First token should indent
		if (!i.hasNext()) {
			return new Block();
		}
		
		try {
			return blockifyHelper(i, (IndentToken)i.next(), null);
		} catch (BlockException be) {
			// Should never happen!
			be.printStackTrace();
			return new Block();
		}
	}
	
	static Block blockifyHelper(Iterator i, IndentToken indent, Block parent) throws BlockException, AdenineException {
		Block block = new Block();		
		block.m_parent = parent;
		block.m_indent = indent.m_token;
		block.m_startline = indent.m_line;
		Line line = new Line();
		line.m_lineno = indent.m_line;
		if (parent != null) {
			parent.m_items.add(block);
		}
		while (i.hasNext()) {
			Token t = (Token)i.next();
			if (t instanceof IndentToken) {
				if (line.m_tokens.size() > 0) {
					line.m_tokens = convertBackQuotes(convertDotsBrackets(line.m_tokens));
					block.m_items.add(line);
					line = new Line();
				}
				if (t.m_token.equals(block.m_indent)) {
					line.m_lineno = t.m_line;
				} else if ((parent != null) && (parent.findIndent(t.m_token))) {
					throw new BlockException(t.m_token, t.m_line);
				} else {
					// Create new block
					try {
						blockifyHelper(i, (IndentToken)t, block);
					} catch (BlockException be) {
						if (be.m_indent.equals(block.m_indent)) {
							line = new Line();
							line.m_lineno = be.m_lineno;
						} else {
							throw be;
						}
					}
				}
			} else {
				line.m_tokens.add(t);
			}
		}
		if (line.m_tokens.size() > 0) {
			line.m_tokens = convertBackQuotes(convertDotsBrackets(line.m_tokens));
			block.m_items.add(line);
		}
		return block;
	}
}
