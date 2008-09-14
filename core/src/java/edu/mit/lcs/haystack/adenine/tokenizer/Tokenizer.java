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

package edu.mit.lcs.haystack.adenine.tokenizer;

import edu.mit.lcs.haystack.Constants;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

/**
 * @author David Huynh
 */
public class Tokenizer implements ITokenIterator {
	LinkedList 		m_tokens = new LinkedList();
	
	int			m_line;
	int			m_column;
	boolean		m_eof;
	
	int			m_tabSize;
	
	CharIterator	m_charIterator;
	IScannerVisitor	m_scannerVisitor;
	
	public Tokenizer(Reader input, IScannerVisitor sVisitor, int tabSize, int startLine, int startColumn, int startOffset) {
		m_charIterator = new CharIterator(input, startOffset);
		m_scannerVisitor = sVisitor;
		m_tabSize = tabSize;
		
		m_line = startLine;
		m_column = startColumn;
	}

	public Tokenizer(Reader input, IScannerVisitor sVisitor, int startLine, int startColumn, int startOffset) {
		this(input, sVisitor, 4, startLine, startColumn, startOffset);
	}
	
	public Tokenizer(Reader input, IScannerVisitor sVisitor) {
		this(input, sVisitor, 4, 0, 0, 0);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#getToken()
	 */
	final public Token getToken() {
		return getToken(0);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#getToken(int)
	 */
	final public Token getToken(int ahead) {
		fetchSeveralTokens(ahead + 1);
		
		if (m_tokens.size() > ahead) {
			return (Token) m_tokens.get(ahead);
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#getLocation()
	 */
	final public Location getLocation() {
		fetchSeveralTokens(1);
		
		if (m_tokens.size() > 0) {
			return ((Token) m_tokens.get(0)).getSpan().getStart();
		} else {
			return new Location(m_line, m_column, m_charIterator.getOffset());
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#swallow()
	 */
	final public void swallow() {
		swallow(1);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#swallow(int)
	 */
	final public void swallow(int count) {
		fetchSeveralTokens(count);
		
		while (count > 0 && m_tokens.size() > 0) {
			Token token = (Token) m_tokens.remove(0);
			count--;
			
			if (m_scannerVisitor != null) {
				m_scannerVisitor.onToken(token);
			}
		}
	}

	final void fetchSeveralTokens(int count) {
		while (!m_eof && m_tokens.size() < count) {
			parseNextToken();
		}
	}
	
	final void parseNextToken() {
		char 	c = m_charIterator.getChar();
		
		if (c == '\0') {
			m_eof = true;
			return;
		} else if (c == '\r' || c == '\n') {
			parseNewLine(c);
		} else if (c == ' ' || c == '\t') {
			parseWhitespaceOrIndent(c);
		} else if (c == '#') {
			parseComment(c);
		} else if (c == '<') {
			parseFullResource(c);
		} else if (c == '"') {
			parseLiteral(c);
		} else if (c == '\'') {
			parseString(c);
		} else if (c == '?') {
			parseWildcardResource(c);
		} else if (c == ':' || Character.isLetter(c)) {
			parseGenericTokenOrPrefixedResource(c);
		} else if (Character.isDigit(c) || c == '-' || c == '+') {
			parseNumber(c);
		} else {
			Location start = new Location(m_line, m_column, m_charIterator.getOffset());
			m_charIterator.swallow();
			Location end = new Location(m_line, m_column, m_charIterator.getOffset());
			
			m_column++;
			
			addToken(new SymbolToken(new Span(start, end), Character.toString(c)));
		}
	}
	
	final void addToken(Token t) {
		m_tokens.add(t);
	}
	
	final void parseNewLine(char c) {
		Location start = new Location(m_line, m_column, m_charIterator.getOffset());
		
		swallowOneColumn(c);
		
		if (c == '\r' && !m_charIterator.isEOF()) {
			c = m_charIterator.getChar();
			if (c == '\n') {
				swallowOneColumn(c);
			}
		}
		
		Location end = new Location(m_line, m_column, m_charIterator.getOffset());
		
		newLine();
		
		addToken(new NewLineToken(new Span(start, end)));
	}

	final void parseWhitespaceOrIndent(char c) {
		Location 		start = new Location(m_line, m_column, m_charIterator.getOffset());
		boolean		newLine = start.getColumn() == 0;
		
		while (true) {
			if (c == ' ' || c == '\t') {
				swallowOneColumn(c);
				
				if (m_charIterator.isEOF()) {
					break;
				}
				
				c = m_charIterator.getChar();
			} else if (c == '#') {
				addToken(new WhitespaceToken(
					new Span(
						start, 
						new Location(m_line, m_column, m_charIterator.getOffset()))));
				
				parseComment(c);
				
				c = m_charIterator.getChar();
				
				start = new Location(m_line, m_column, m_charIterator.getOffset());
			} else {
				break;
			}
		}
		
		Location end = new Location(m_line, m_column, m_charIterator.getOffset());

		if (newLine && c != '#' && c != '\r' && c != '\n' && c != '\0') {
			addToken(new IndentToken(new Span(start, end)));
		} else {
			addToken(new WhitespaceToken(new Span(start, end)));
		}
	}

	final void parseComment(char c) {
		if (m_charIterator.getChar(1) == '[') {
			parseMultilineComment(c);
		} else {
			parseSinglelineComment(c);
		}
	}
	
	final void parseSinglelineComment(char c) {
		Location start = new Location(m_line, m_column, m_charIterator.getOffset());
			
		while (true) {
			if (c != '\r' && c != '\n') {
				swallowOneColumn(c);
					
				if (m_charIterator.isEOF()) {
					break;
				}
					
				c = m_charIterator.getChar();
			} else {
				break;
			}
		}
			
		Location end = new Location(m_line, m_column, m_charIterator.getOffset());
	
		addToken(new CommentToken(new Span(start, end), false));
	}
	
	final void parseMultilineComment(char c) {
		Location start = new Location(m_line, m_column, m_charIterator.getOffset());
		
		swallowOneColumn(c); // swallow #
		swallowOneColumn(c); // swallow [
		
		int nestedLevel = 1;
		int openCount = 1;
		int closeCount = 0;
		
		while (!m_charIterator.isEOF()) {
			c = m_charIterator.getChar();

			swallowOneColumn(c);
			
			if (c == '\r') {
				if (!m_charIterator.isEOF()) {
					c = m_charIterator.getChar();
					if (c == '\n') {
						swallowOneColumn(c);
					}
				}
				newLine();
			} else if (c == '\n') {
				newLine();
			} else if (c == '#') {
				if (!m_charIterator.isEOF()) {
					c = m_charIterator.getChar();
					
					if (c == '[') {
						swallowOneColumn(c);
						nestedLevel++;
						openCount++;
					} else if (c == ']') {
						swallowOneColumn(c);
						nestedLevel--;
						closeCount++;
						
						if (nestedLevel == 0) {
							break;
						}
					}
				}
			} else if (c == ']') {
				if (!m_charIterator.isEOF()) {
					c = m_charIterator.getChar();
					
					if (c == '#') {
						swallowOneColumn(c);
						nestedLevel--;
						closeCount++;
						
						if (nestedLevel == 0) {
							break;
						}
					}
				}
			}
		}
		
		Location end = new Location(m_line, m_column, m_charIterator.getOffset());

		if (nestedLevel == 0) {
			addToken(new CommentToken(new Span(start, end), true));
		} else {
			addToken(new ErrorToken(new Span(start, end), 
				"Multiline comment opened " + openCount + " time(s) but closed only " + closeCount + " time(s)"));
		}
	}

	static protected String	s_fullResourceExcepts = ">\r\n\t";
	final void parseFullResource(char c) {
		Location 		start = new Location(m_line, m_column, m_charIterator.getOffset());
		StringBuffer	sb = new StringBuffer();
		
		swallowOneColumn(c); // swallow <
		
		while (!m_charIterator.isEOF()) {
			c = m_charIterator.getChar();

			if (s_fullResourceExcepts.indexOf(c) >= 0) {
				break;
			}
			
			sb.append(c);

			swallowOneColumn(c);
		}
		
		if (c == '>') {
			swallowOneColumn(c);
			
			Location end = new Location(m_line, m_column, m_charIterator.getOffset());

			addToken(new ResourceToken(new Span(start, end), null, sb.toString())); 
		} else {
			Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());

			addToken(new ErrorToken(new Span(start, end), "Full resource URI missing closing angle bracket"));
		}
	}

	final void parseLiteral(char c) {
		Location 		start = new Location(m_line, m_column, m_charIterator.getOffset());
		StringBuffer	sb = new StringBuffer();
		boolean tripleQuote = false;
		
		// Check for """
		if (m_charIterator.getChar(1) == '\"' && m_charIterator.getChar(2) == '\"') {
			// swallow """
			swallowOneColumn(c); 
			swallowOneColumn(c);
			swallowOneColumn(c);
			
			while (!m_charIterator.isEOF()) {
				c = m_charIterator.getChar();

				if (c == '\"' && m_charIterator.getChar(1) == '\"' && m_charIterator.getChar(2) == '\"') {
					swallowOneColumn(c);
					swallowOneColumn(c);
					swallowOneColumn(c);
					break;
				}
	
				switch (c) {
					case '\r':
						char c1 = m_charIterator.getChar(1);
						if (c1 == '\n') {
							swallowOneColumn(c);
							c = c1;
						}
						// fall through
					case '\n':
						sb.append('\n');
						newLine();
						break;
					
					default:
						if (c == '\\' && m_charIterator.getChar(1) != '\0') {
							swallowOneColumn(c);
							
							char c2 = m_charIterator.getChar();
							
							switch (c2) {
								case 't':	sb.append('\t'); break;
								case 'r':	sb.append('\r'); break;
								case 'n':	sb.append('\n'); break;
								case 'b':	sb.append('\b'); break;
								case '"':	sb.append('"'); break;
								case '\'':	sb.append('\''); break;
								case '\r':
									char c3 = m_charIterator.getChar(1);
									if (c3 == '\n') {
										swallowOneColumn(c2);
										c2 = c3;
									}
									// fall through
								case '\n':
									newLine();
									sb.append(c);
									break;
								default:
									sb.append(c);
									sb.append(c2);
							}
							
							c = c2;
						} else {
							sb.append(c);
						}
				}
				swallowOneColumn(c);
			}
			
			if (c == '"') {
				Location end = new Location(m_line, m_column, m_charIterator.getOffset());
	
				addToken(new LiteralToken(new Span(start, end), sb.toString())); 
			} else {
				Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());
	
				addToken(new ErrorToken(new Span(start, end), "Literal missing closing quote mark"));
			}
		} else {
			swallowOneColumn(c); // swallow "
			
			while (!m_charIterator.isEOF()) {
				c = m_charIterator.getChar();
	
				if (c == '\r' || c == '\n' || c == '"') {
					break;
				} else if (c == '\\' && m_charIterator.getChar(1) != '\0') {
					swallowOneColumn(c);
					
					char c2 = m_charIterator.getChar();
					
					switch (c2) {
						case 't':	sb.append('\t'); break;
						case 'r':	sb.append('\r'); break;
						case 'n':	sb.append('\n'); break;
						case 'b':	sb.append('\b'); break;
						case '"':	sb.append('"'); break;
						case '\'':	sb.append('\''); break;
						case '\r':
							char c3 = m_charIterator.getChar(1);
							if (c3 == '\n') {
								swallowOneColumn(c2);
								c2 = c3;
							}
							// fall through
						case '\n':
							newLine();
							break;
						default:
							sb.append(c);
							sb.append(c2);
					}
					
					c = c2;
				} else {
					sb.append(c);
				}
	
				swallowOneColumn(c);
			}
			
			if (c == '"') {
				swallowOneColumn(c);
				
				Location end = new Location(m_line, m_column, m_charIterator.getOffset());
	
				addToken(new LiteralToken(new Span(start, end), sb.toString())); 
			} else {
				Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());
	
				addToken(new ErrorToken(new Span(start, end), "Literal missing closing quote mark"));
			}
		}
	}

	final void parseString(char c) {
		Location 		start = new Location(m_line, m_column, m_charIterator.getOffset());
		StringBuffer	sb = new StringBuffer();
		
		swallowOneColumn(c); // swallow '
		
		while (!m_charIterator.isEOF()) {
			c = m_charIterator.getChar();

			if (c == '\r' || c == '\n' || c == '\'') {
				break;
			} else if (c == '\\' && m_charIterator.getChar(1) != '\0') {
				swallowOneColumn(c);
				
				char c2 = m_charIterator.getChar();
				
				switch (c2) {
					case 't':	sb.append('\t'); break;
					case 'r':	sb.append('\r'); break;
					case 'n':	sb.append('\n'); break;
					case 'b':	sb.append('\b'); break;
					case '\'':	sb.append('\''); break;
					case '\"':	sb.append('\"'); break;
					case '\r':
						char c3 = m_charIterator.getChar(1);
						if (c3 == '\n') {
							swallowOneColumn(c2);
							c2 = c3;
						}
						// fall through
					case '\n':
						newLine();
						break;
					default:
						sb.append(c);
						sb.append(c2);
				}
				
				c = c2;
			} else {
				sb.append(c);
			}

			swallowOneColumn(c);
		}
		
		if (c == '\'') {
			swallowOneColumn(c);
			
			Location end = new Location(m_line, m_column, m_charIterator.getOffset());

			addToken(new StringToken(new Span(start, end), sb.toString())); 
		} else {
			Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());

			addToken(new ErrorToken(new Span(start, end), "String missing closing single-quote mark"));
		}
	}

	static protected String	s_genericTokenExcepts = "<>(){}[].,;'\"`@$%? \r\n\t\0";
	final void parseGenericTokenOrPrefixedResource(char c) {
		Location 		start = new Location(m_line, m_column, m_charIterator.getOffset());
		StringBuffer	sb = new StringBuffer();
		
		while (true) {
			if (s_genericTokenExcepts.indexOf(c) >= 0) {
				break;
			}
			
			swallowOneColumn(c);

			if (c == ':') {
				parsePrefixedResource(start, sb.toString());
				return;
			}
			
			sb.append(c);
			
			if (m_charIterator.isEOF()) {
				break;
			}
			c = m_charIterator.getChar();
		}
		
		Location end = new Location(m_line, m_column, m_charIterator.getOffset());

		addToken(new GenericToken(new Span(start, end), sb.toString()));
	}
	
	final void parsePrefixedResource(Location start, String prefix) {
		StringBuffer	sb = new StringBuffer();
		char			c = m_charIterator.getChar();
		
		while (true) {
			if (s_genericTokenExcepts.indexOf(c) >= 0) {
				break;
			}
			
			swallowOneColumn(c);
			sb.append(c);
			
			if (m_charIterator.isEOF()) {
				break;
			}
			c = m_charIterator.getChar();
		}
		
		Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());
		Span		span = new Span(start, end);

		addToken(new ResourceToken(span, prefix, sb.toString()));
	}

	final void parseWildcardResource(char c) {
		Location 		start = new Location(m_line, m_column, m_charIterator.getOffset());
		StringBuffer	sb = new StringBuffer();
		
		swallowOneColumn(c);

		while (!m_charIterator.isEOF()) {
			c = m_charIterator.getChar();
			
			if (s_genericTokenExcepts.indexOf(c) >= 0) {
				break;
			}

			swallowOneColumn(c);
			sb.append(c);
		}

		Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());
		Span		span = new Span(start, end);
		
		if (sb.length() > 0) {
			addToken(new ResourceToken(span, null, Constants.s_wildcard_namespace + sb.toString()));
		} else {
			addToken(new ErrorToken(span, "Wildcard URI incomplete"));
		}
	}
	
	final void parseNumber(char c) {
		Location 		start = new Location(m_line, m_column, m_charIterator.getOffset());
		StringBuffer	sb = new StringBuffer();
		
		int		sign;
		int		digitCount = 0;

		boolean	isFloat = false;
		long		decimalRatio = 1;

		long		value = 0;
		
		if (c == '+') {
			sign = 1;
			swallowOneColumn(c);
			sb.append(c);
		} else if (c == '-') {
			sign = -1;
			swallowOneColumn(c);
			sb.append(c);
		} else {
			sign = 1;
		}
		
		while (!m_charIterator.isEOF()) {
			c = m_charIterator.getChar();
			
			if (Character.isDigit(c)) {
				
				value = value * 10 + Character.getNumericValue(c);
				
				swallowOneColumn(c);
				sb.append(c);
				
				digitCount++;
				decimalRatio *= 10;
			} else if (c == '.') {
				swallowOneColumn(c);

				isFloat = true;
			} else if ((c == ' ' || c == '\t' || c == '\r' || c == '\n') && digitCount == 0) {
				Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());
				Span		span = new Span(start, end);

				addToken(new SymbolToken(span, sign == 1 ? "+" : "-"));
				return;
			} else {
				break;
			}
		}
		
		Location 	end = new Location(m_line, m_column, m_charIterator.getOffset());
		Span		span = new Span(start, end);

		if (digitCount > 0) {
			if (isFloat) {
				addToken(new FloatToken(span, sb.toString(), ((float) value * sign) / decimalRatio));
			} else {
				addToken(new IntegerToken(span, sb.toString(), (int) (value * sign)));
			}
		} else {
			addToken(new ErrorToken(span, "Malformed integer or float"));
		}
	}

	final void swallowOneColumn(char c) {
		m_charIterator.swallow();
		if (c == '\t') {
			m_column = ((m_column / m_tabSize) + 1) * m_tabSize;
		} else {
			m_column++;
		}
	}
	
	final void newLine() {
		m_line++;
		m_column = 0;
	}
}


class CharIterator {
	static int m_bufferSize = 10;
		
	char[]		m_chars = new char[m_bufferSize];
	int		m_count;

	Reader		m_reader;
	boolean	m_eof;
	
	int		m_offset;
		
	public CharIterator(Reader reader, int offset) {
		m_reader = reader;
		m_offset = offset;
	}
	
	final public int getOffset() {
		return m_offset;
	}
	
	final public char getChar() {
		return getChar(0);
	}
	
	final public char getChar(int ahead) {
		fetchSeveralChars(ahead + 1);
			
		if (m_count > ahead) {
			return m_chars[ahead];
		} else {
			return '\0';
		}
	}
	
	final public void swallow() {
		swallow(1);
	}
	
	final public void swallow(int count) {
		fetchSeveralChars(count);
		
		if (count >= m_count) {
			m_offset += m_count;
			m_count = 0;
		} else {
			for (int i = 0; i < m_count - count; i++) {
				m_chars[i] = m_chars[i + count];
			}
			m_count -= count;
			m_offset += count;
		}
	}
	
	final public boolean isEOF() {
		return m_eof;
	}
	
	final void fetchSeveralChars(int count) {
		if (count > m_count && !m_eof) {
			count = Math.min(m_bufferSize, count);
			
			int more = count - m_count;
			try {
				int actualMore = m_reader.read(m_chars, m_count, more);
				if (actualMore < more) {
					m_eof = true;
				}
				
				m_count += actualMore;
			} catch (IOException e) {
				m_eof = true;
			}
		}
	}
}
	
