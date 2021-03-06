/*
 *   Copyright (c) 2014 Program Analysis Group, Georgia Tech
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package edu.gatech.sqltutor.rules.symbolic;

import static edu.gatech.sqltutor.rules.datalog.iris.IrisUtil.literal;

import java.util.EnumSet;

import org.deri.iris.api.basics.IQuery;
import org.deri.iris.api.basics.ITuple;
import org.deri.iris.builtins.NotEqualBuiltin;
import org.deri.iris.factory.Factory;
import org.deri.iris.storage.IRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.gatech.sqltutor.rules.DefaultPrecedence;
import edu.gatech.sqltutor.rules.ISymbolicTranslationRule;
import edu.gatech.sqltutor.rules.Markers;
import edu.gatech.sqltutor.rules.TranslationPhase;
import edu.gatech.sqltutor.rules.datalog.iris.IrisUtil;
import edu.gatech.sqltutor.rules.datalog.iris.RelationExtractor;
import edu.gatech.sqltutor.rules.datalog.iris.SymbolicPredicates;
import edu.gatech.sqltutor.rules.lang.StandardSymbolicRule;
import edu.gatech.sqltutor.rules.symbolic.tokens.ISymbolicToken;

public class DeterminerRedundancyRule 
		extends StandardSymbolicRule implements ISymbolicTranslationRule {
	private static final Logger _log = LoggerFactory.getLogger(DeterminerRedundancyRule.class);
	
	private static final IQuery QUERY = Factory.BASIC.createQuery(
		// attribute list parent
		literal(SymbolicPredicates.type, "?attrList", SymbolicType.ATTRIBUTE_LIST),
		
		// sequence in first position
		literal(SymbolicPredicates.parentOf, "?attrList", "?seq1", 0),
		literal(SymbolicPredicates.type, "?seq1", SymbolicType.SEQUENCE),
		
		// first sequence child is a determiner (DT)
		literal(SymbolicPredicates.parentOf, "?seq1", "?det1", 0),
		literal(SymbolicPredicates.type, "?det1", SymbolicType.LITERAL),
		literal(SymbolicPredicates.partOfSpeech, "?det1", PartOfSpeech.DETERMINER.getTag()),
		literal(SymbolicPredicates.literalExpression, "?det1", "?expr"),

		// there is another sequence child
		literal(SymbolicPredicates.parentOf, "?attrList", "?seq2", "?seqPos2"),
		literal(SymbolicPredicates.type, "?seq2", SymbolicType.SEQUENCE),
		literal(new NotEqualBuiltin(IrisUtil.asTerm("?seq1"), IrisUtil.asTerm("?seq2"))),
		
		// with the same determiner
		literal(SymbolicPredicates.parentOf, "?seq2", "?det2", 0),
		literal(SymbolicPredicates.type, "?det2", SymbolicType.LITERAL),
		literal(SymbolicPredicates.partOfSpeech, "?det2", PartOfSpeech.DETERMINER.getTag()),
		literal(SymbolicPredicates.literalExpression, "?det2", "?expr")
	);

	public DeterminerRedundancyRule() {
		super(DefaultPrecedence.SIMPLIFYING_LOWERED);
	}

	public DeterminerRedundancyRule(int precedence) {
		super(precedence);
	}

	@Override
	protected boolean handleResult(IRelation relation, RelationExtractor ext) {
		_log.trace(Markers.SYMBOLIC, "Got relation: {}", relation);
		
		ITuple result = relation.get(0);
		ext.setCurrentTuple(result);
		
		ISymbolicToken sequence2 = ext.getToken("?seq2");
		ISymbolicToken determiner2 = ext.getToken("?det2");
		
		sequence2.getChildren().remove(determiner2);
		_log.debug(Markers.SYMBOLIC, "Deleted {} from {}", determiner2, sequence2);
		
		return true;
	}

	@Override
	protected IQuery getQuery() {
		return QUERY;
	}
	
	@Override
	protected EnumSet<TranslationPhase> getDefaultPhases() {
		return EnumSet.of(TranslationPhase.LOWERING);
	}
}
