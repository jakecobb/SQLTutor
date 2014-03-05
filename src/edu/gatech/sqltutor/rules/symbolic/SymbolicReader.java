package edu.gatech.sqltutor.rules.symbolic;

import java.util.List;

import edu.gatech.sqltutor.rules.symbolic.tokens.ISymbolicToken;
import edu.gatech.sqltutor.rules.symbolic.tokens.LiteralToken;
import edu.gatech.sqltutor.rules.symbolic.tokens.RootToken;

public class SymbolicReader {

	public SymbolicReader() {
	}
	
	public String readSymbolicState(RootToken root) {
		StringBuilder out = new StringBuilder();
		
		readSequence(out, root);
		out.append('.');
		
		return out.toString();
	}
	
	private void readToken(StringBuilder out, ISymbolicToken token) {				
		SymbolicType type = token.getType();
		switch( type ) {
			case LITERAL:
				out.append(((LiteralToken)token).getExpression());
				break;
			case ROOT:
			case LITERALS:
			case SEQUENCE: 
				readSequence(out, token); 
				break;
			case ATTRIBUTE_LIST:
				out.append("the ");
				// fall-through
			case AND:
				readConjunctedList(out, token, "and");
				break;
			case OR:
				readConjunctedList(out, token, "or");
				break;
			default:
				throw new SymbolicException("Unhandled type " + type + " for token " + token);
		}
	}
	
	/**
	 * Reads the children with spaces separating any non-punctuation symbols.
	 */
	private void readSequence(StringBuilder out, ISymbolicToken token) {
		boolean first = true;
		for( ISymbolicToken child: token.getChildren() ) {
			if( !first && !PartOfSpeech.isPunctuation(child.getPartOfSpeech()) ) {
				out.append(' ');
			}
			first = false;
			
			readToken(out, child);
		}
	}

	
	/**
	 * Append the English conjunction of a list of terms to <code>out</code>.
	 * 
	 * <p>Produces:<br />
	 * 1: &lt;child&gt; <br />
	 *	2: &lt;child1&gt; &lt;conjunct&gt; &lt;child2&gt;<br />
	 *	3+: &lt;child1&gt;, &lt;child2&gt;, ..., &lt;conjunct&gt; &lt;childN&gt;<br />
	 * </p>
	 */
	private void readConjunctedList(StringBuilder out, ISymbolicToken token, String conjunct) {
		// 1: <child>
		// 2: <child1> <conjunct> <child2>
		// 3+: <child1>, <child2>, ..., <conjunct> <childN> 
		List<ISymbolicToken> children = token.getChildren();
		for( int i = 0, ilen = children.size(); i < ilen; ++i ) {
			ISymbolicToken child = children.get(i);
			if( i != 0 ) {
				if( i != ilen - 1 ) {
					out.append(',');
				} else {
					if( i != 1 )
						out.append(',');
					out.append(' ').append(conjunct);
				}
				out.append(' ');
			}
			readToken(out, child);
		}
	}
}