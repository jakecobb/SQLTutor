package edu.gatech.sqltutor.rules.datalog.iris;

import static edu.gatech.sqltutor.rules.datalog.iris.IrisUtil.predicate;

import org.deri.iris.api.basics.IPredicate;

/** Static predicates referring to symbolic sentence structure. */
public class SymbolicPredicates {
	/** 
	 * <code>(?parent:int,?child:int,?pos:int)</code> =>
	 * <code>?child</code> is the <code>?pos</code>'th child of <code>?parent</code> 
	 */
	public static final IPredicate parentOf = predicate("symParentOf", 3);
	
	/** <code>(?token:int,?type:string)</code> => <code>?token</code> is of type <code>?type</code> */
	public static final IPredicate type = predicate("symType", 2);
	
	/** <code>(?token:int,?tag:string)</code> => <code>?token</code> has part of speech tag <code>?tag</code> */
	public static final IPredicate partOfSpeech = predicate("symPartOfSpeech", 2); 
	
	/** <code>(?token:int,?entity:string,?attribute:string)</code> */
	public static final IPredicate refsAttribute = predicate("symRefsAttribute", 3);
	
	/** <code>(?token:int,?table:int)</code> */
	public static final IPredicate refsTable = predicate("symRefsTable", 2);
	
	/** <code>(?token:int,?number:number)</code> */
	public static final IPredicate number = predicate("symNumber", 2);
	
	/** <code>(?token:int,?type:string/NumericType)</code>. */
	public static final IPredicate numberType = predicate("symNumberType", 2);
	
	/** <code>(?token:int,?op:string)</code> */
	public static final IPredicate binaryOperator = predicate("symBinaryOperator", 2);
	
	/** <code>(?token:int,?expr:string)</code> */
	public static final IPredicate literalExpression = predicate("symLiteralExpression", 2); 
	
	// for debugging, id => token.toString()
	public static final IPredicate debugString = predicate("symDebugString", 2);
	
// defined statically
	/** <code>symAncestorOf(?ancestor:int,?descendent:int,?depth:int)</code> */
	public static final IPredicate ancestorOf = predicate("symAncestorOf", 3);
	/** <code>symCommonAncestorDepth(?ancestor:id,?token1:id,?token2:id,?depth1:int,?depth2:int)</code>
	 * token 1 and 2 have a common ancestor ?ancestor and depths 1 and 2, respectively
	 */
	public static final IPredicate commonAncestorDepth = predicate("symCommonAncestorDepth", 5);
	
	/** <code>symCommonAncestor(?ancestor:id,?token1:id,?token2:id)</code>
	 * token 1 and 2 have a common ancestor ?ancestor and depths 1 and 2, respectively
	 */
	public static final IPredicate commonAncestor = predicate("symCommonAncestor", 3);
	
	/**
	 * <code>symLeastCommonAncestorDepth(?ancestor:id,?token1:id,?token2:id)</code>
	 */
	public static final IPredicate leastCommonAncestor = predicate("symLeastCommonAncestor", 3); 
}