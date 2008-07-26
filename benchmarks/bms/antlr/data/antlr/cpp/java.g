options {
	language="Cpp";
}

/** Java 1.1 Recognizer Grammar
 *
 * Run 'java JavaRecognizer <directory full of java files>'
 *
 * Authors:
 *		John Mitchell		johnm@non.net
 *		Terence Parr		parrt@magelang.com
 *		John Lilley			jlilley@empathy.com
 *		Scott Stanchfield	thetick@magelang.com
 *
 * Version 1.00 December 9, 1997 -- initial release
 * Version 1.01 December 10, 1997
 *		fixed bug in octal def (0..7 not 0..8)
 * Version 1.10 August 1998 (parrt)
 *		added tree construction
 *		fixed definition of WS_,comments for mac,pc,unix newlines
 *		added unary plus
 * Version 1.11 (Nov 20, 1998)
 *		Added "shutup" option to turn off last ambig warning.
 *		Fixed inner class def to allow named class defs as statements
 *		synchronized requires compound not simple statement
 *		add [] after builtInType DOT class in primaryExpression
 *		"const" is reserved but not valid..removed from modifiers
 *
 * This grammar is in the PUBLIC DOMAIN
 *
 * BUGS
 *		(expression) + "string" is parsed incorrectly (+ as unary plus).
 *
 */

class JavaRecognizer extends Parser;
options {
	k = 2;                           // two token lookahead
	exportVocab=Java;            // Call its vocabulary "Java"
	codeGenMakeSwitchThreshold = 2;  // Some optimizations
	codeGenBitsetTestThreshold = 3;
	defaultErrorHandler = false;     // Don't generate parser error handlers
	buildAST = true;
}

imaginaryTokenDefinitions
	:	BLOCK MODIFIERS OBJBLOCK SLIST CTOR_DEF METHOD_DEF VARIABLE_DEF
		INSTANCE_INIT STATIC_INIT TYPE CLASS_DEF INTERFACE_DEF
		PACKAGE_DEF ARRAY_DECLARATOR EXTENDS_CLAUSE IMPLEMENTS_CLAUSE
		PARAMETERS PARAMETER_DEF LABELED_STAT TYPECAST INDEX_OP
		POST_INC POST_DEC METHOD_CALL EXPR ARRAY_INIT
		IMPORT UNARY_MINUS UNARY_PLUS CASE_GROUP ELIST FOR_INIT FOR_CONDITION
		FOR_ITERATOR EMPTY_STAT
	;
	
// Compilation Unit: In Java, this is a single file.  This is the start
//   rule for this parser
compilationUnit
	:	// A compilation unit starts with an optional package definition
		(	packageDefinition
		|	/* nothing */
		)

		// Next we have a series of zero or more import statements
		( importDefinition )*

		// Wrapping things up with any number of class or interface
		//    definitions
		( typeDefinition )*

		EOF!
	;


// Package statement: "package" followed by an identifier.
packageDefinition
	options {defaultErrorHandler = true;} // let ANTLR handle errors
	:	p:"package"^ {#p->setType(PACKAGE_DEF);} identifier SEMI!
	;


// Import statement: import followed by a package or class name
importDefinition
	options {defaultErrorHandler = true;}
	:	i:"import"^ {#i->setType(IMPORT);} identifierStar SEMI!
	;

// A type definition in a file is either a class or interface definition.
typeDefinition
	options {defaultErrorHandler = true;}
	:	m:modifiers!
		( classDefinition[#m]
		| interfaceDefinition[#m]
		)
	|	SEMI!
	;

/** A declaration is the creation of a reference or primitive-type variable
 *  Create a separate Type/Var tree for each var in the var list.
 */
declaration!
	:	m:modifiers t:typeSpec[false] v:variableDefinitions[#m,#t]
		{#declaration = #v;}
	;

/* A declaration with no modifiers
localVariableDeclaration
	:	t:typeSpec[false] v:variableDefinitions[#[MODIFIERS, "MODIFIERS"],#t]
		{#localVariableDeclaration = #v;}
	;
 */

// A list of zero or more modifiers.  We could have used (modifier)* in
//   place of a call to modifiers, but I thought it was a good idea to keep
//   this rule separate so they can easily be collected in a Vector if
//   someone so desires
modifiers
	:	( modifier )*
		{#modifiers = #([MODIFIERS, "MODIFIERS"], #modifiers);}
	;


// A type specification is a type name with possible brackets afterwards
//   (which would make it an array type).
typeSpec[bool addImagNode]
	:	type (lb:LBRACK^ {#lb->setType(ARRAY_DECLARATOR);} RBRACK!)*
		{
			if ( addImagNode ) {
				#typeSpec = #(#[TYPE,"TYPE"], #typeSpec);
			}
		}
	;

// A type name. which is either a (possibly qualified) class name or
//   a primitive (builtin) type
type
	:	identifier
	|	builtInType
	;

// The primitive types.
builtInType
	:	"void"
	|	"boolean"
	|	"byte"
	|	"char"
	|	"short"
	|	"int"
	|	"float"
	|	"long"
	|	"double"
	;

// A (possibly-qualified) java identifier.  We start with the first IDENT
//   and expand its name by adding dots and following IDENTS
identifier
	:	IDENT  ( DOT^ IDENT )*
	;

identifierStar
	:	IDENT
		( DOT^ IDENT )*
		( DOT^ STAR  )?
	;


// modifiers for Java classes, interfaces, class/instance vars and methods
modifier
	:	"private"
	|	"public"
	|	"protected"
	|	"static"
	|	"transient"
	|	"final"
	|	"abstract"
	|	"native"
	|	"threadsafe"
	|	"synchronized"
//	|	"const"			// reserved word; leave out
	|	"volatile"
	;


// Definition of a Java class
classDefinition![ANTLR_USE_NAMESPACE(antlr)RefAST modifiers]
	:	"class" IDENT
		// it _might_ have a superclass...
		sc:superClassClause
		// it might implement some interfaces...
		ic:implementsClause
		// now parse the body of the class
		cb:classBlock
		{#classDefinition = #(#[CLASS_DEF,"CLASS_DEF"],
							   modifiers,IDENT,sc,ic,cb);}
	;

superClassClause!
	:	( "extends" id:identifier )?
		{#superClassClause = #(#[EXTENDS_CLAUSE,"EXTENDS_CLAUSE"],id);}
	;

// Definition of a Java Interface
interfaceDefinition![ANTLR_USE_NAMESPACE(antlr)RefAST modifiers]
	:	"interface" IDENT
		// it might extend some other interfaces
		(ie:interfaceExtends)?
		// now parse the body of the interface (looks like a class...)
		cb:classBlock
		{#interfaceDefinition = #(#[INTERFACE_DEF,"INTERFACE_DEF"],
									modifiers,IDENT,ie,cb);}
	;


// This is the body of a class.  You can have fields and extra semicolons,
// That's about it (until you see what a field is...)
classBlock
	:	LCURLY!
			( field | SEMI )*
		RCURLY!
		{#classBlock = #([OBJBLOCK, "OBJBLOCK"], #classBlock);}
	;

// An interface can extend several other interfaces...
interfaceExtends
	:	e:"extends" {#e->setType(EXTENDS_CLAUSE);}
		identifier ( COMMA! identifier )*
	;

// A class can implement several interfaces...
implementsClause
	:	(
			i:"implements"! identifier ( COMMA! identifier )*
		)?
		{#implementsClause = #(#[IMPLEMENTS_CLAUSE,"IMPLEMENTS_CLAUSE"],
								 #implementsClause);}
	;

// Now the various things that can be defined inside a class or interface...
// Note that not all of these are really valid in an interface (constructors,
//   for example), and if this grammar were used for a compiler there would
//   need to be some semantic checks to make sure we're doing the right thing...
field!
	:	// method, constructor, or variable declaration
		mods:modifiers
		(	h:ctorHead s:compoundStatement // constructor
			{#field = #(#[CTOR_DEF,"CTOR_DEF"], mods, h, s);}

		|	cd:classDefinition[#mods]       // inner class
			{#field = #cd;}
			
		|	id:interfaceDefinition[#mods]   // inner interface
			{#field = #id;}

		|	t:typeSpec[false]  // method or variable declaration(s)
			(	IDENT  // the name of the method

				// parse the formal parameter declarations.
				LPAREN! param:parameterDeclarationList RPAREN!

				rt:returnTypeBrackersOnEndOfMethodHead[#t]

				// get the list of exceptions that this method is declared to throw
				(tc:throwsClause)?

				( s2:compoundStatement | SEMI )
				{#field = #(#[METHOD_DEF,"METHOD_DEF"],
						     mods,
							 #(#[TYPE,"TYPE"],rt),
							 IDENT,
							 param,
							 tc,
							 s2);}
			|	v:variableDefinitions[#mods,#t] SEMI
//				{#field = #(#[VARIABLE_DEF,"VARIABLE_DEF"], v);}
				{#field = #v;}
			)
		)

    // "static { ... }" class initializer
	|	"static" s3:compoundStatement
		{#field = #(#[STATIC_INIT,"STATIC_INIT"], s3);}

    // "{ ... }" instance initializer
	|	compoundStatement
		{#field = #(#[INSTANCE_INIT,"INSTANCE_INIT"], s3);}
	;

variableDefinitions[ANTLR_USE_NAMESPACE(antlr)RefAST mods, ANTLR_USE_NAMESPACE(antlr)RefAST t]
	:	variableDeclarator[getASTFactory()->dupTree(mods),
						   getASTFactory()->dupTree(t)]
		(	COMMA!
			variableDeclarator[getASTFactory()->dupTree(mods),
							   getASTFactory()->dupTree(t)]
		)*
	;

/** Declaration of a variable.  This can be a class/instance variable,
 *   or a local variable in a method
 * It can also include possible initialization.
 */
variableDeclarator![ANTLR_USE_NAMESPACE(antlr)RefAST mods, ANTLR_USE_NAMESPACE(antlr)RefAST t]
	:	id:IDENT d:declaratorBrackets[t] v:varInitializer
		{#variableDeclarator = #(#[VARIABLE_DEF,"VARIABLE_DEF"], mods, #(#[TYPE,"TYPE"],d), id, v);}
	;

declaratorBrackets[ANTLR_USE_NAMESPACE(antlr)RefAST typ]
	:	{#declaratorBrackets=typ;}
		(lb:LBRACK^ {#lb->setType(ARRAY_DECLARATOR);} RBRACK!)*
	;

varInitializer
	:	( ASSIGN^ initializer )?
	;

// This is an initializer used to set up an array.
arrayInitializer
	:	lc:LCURLY^ {#lc->setType(ARRAY_INIT);}
			(	initializer
				(
					// CONFLICT: does a COMMA after an initializer start a new
					//           initializer or start the option ',' at end?
					//           ANTLR generates proper code by matching
					//			 the comma as soon as possible.
					options {
						warnWhenFollowAmbig = false;
					}
				:
					COMMA! initializer
				)*
				(COMMA!)?
			)?
		RCURLY!
	;


// The two "things" that can initialize an array element are an expression
//   and another (nested) array initializer.
initializer
	:	expression
	|	arrayInitializer
	;

// This is the header of a method.  It includes the name and parameters
//   for the method.
//   This also watches for a list of exception classes in a "throws" clause.
ctorHead
	:	IDENT  // the name of the method

		// parse the formal parameter declarations.
		LPAREN! parameterDeclarationList RPAREN!

		// get the list of exceptions that this method is declared to throw
		(throwsClause)?
	;

// This is a list of exception classes that the method is declared to throw
throwsClause
	:	"throws"^ identifier ( COMMA! identifier )*
	;


returnTypeBrackersOnEndOfMethodHead[ANTLR_USE_NAMESPACE(antlr)RefAST typ]
	:	{#returnTypeBrackersOnEndOfMethodHead = typ;}
		(lb:LBRACK^ {#lb->setType(ARRAY_DECLARATOR);} RBRACK!)*
	;

// A list of formal parameters
parameterDeclarationList
	:	( parameterDeclaration ( COMMA! parameterDeclaration )* )?
		{#parameterDeclarationList = #(#[PARAMETERS,"PARAMETERS"],
									#parameterDeclarationList);}
	;

// A formal parameter.
parameterDeclaration!
	:	pm:parameterModifier t:typeSpec[false] id:IDENT
		pd:parameterDeclaratorBrackets[#t]
		{#parameterDeclaration = #(#[PARAMETER_DEF,"PARAMETER_DEF"],
									pm, #([TYPE,"TYPE"],pd), id);}
	;

parameterDeclaratorBrackets[ANTLR_USE_NAMESPACE(antlr)RefAST t]
	:	{#parameterDeclaratorBrackets = t;}
		(lb:LBRACK^ {#lb->setType(ARRAY_DECLARATOR);} RBRACK!)*
	;

parameterModifier
	:	(f:"final")?
		{#parameterModifier = #(#[MODIFIERS,"MODIFIERS"], f);}
	;

// Compound statement.  This is used in many contexts:
//   Inside a class definition prefixed with "static":
//      it is a class initializer
//   Inside a class definition without "static":
//      it is an instance initializer
//   As the body of a method
//   As a completely indepdent braced block of code inside a method
//      it starts a new scope for variable definitions

compoundStatement
	:	lc:LCURLY^ {#lc->setType(SLIST);}
			// include the (possibly-empty) list of statements
			(statement)*
		RCURLY!
	;


statement
	// A list of statements in curly braces -- start a new scope!
	:	compoundStatement

	// class definition (no modifiers allowed; pass empty list)
	|	classDefinition[#[MODIFIERS, "MODIFIERS"]]

	// interface definition (no modifiers allowed; pass empty list)
	|	interfaceDefinition[#[MODIFIERS, "MODIFIERS"]]

		// declarations are ambiguous with "ID DOT" relative to expression
		// statements.  Must backtrack to be sure.  Could use a semantic
		// predicate to test symbol table to see what the type was coming
		// up, but that's pretty hard without a symbol table ;)
//	|	(localVariableDeclaration)=> localVariableDeclaration SEMI!
	|	(declaration)=> declaration SEMI!

		// An expression statement.  This could be a method call,
		// assignment statement, or any other expression evaluated for
		// side-effects.
	|	expression SEMI!

	// Attach a label to the front of a statement
	|	IDENT c:COLON^ {#c->setType(LABELED_STAT);} statement

	// If-else statement
	|	"if"^ LPAREN! expression RPAREN! statement
		(
			// CONFLICT: the old "dangling-else" problem...
			//           ANTLR generates proper code matching
			//			 as soon as possible.  Hush warning.
			options {
				warnWhenFollowAmbig = false;
			}
		:
			"else"! statement
		)?

	// For statement
	|	"for"^
			LPAREN!
				forInit SEMI!   // initializer
				forCond	SEMI!   // condition test
				forIter         // updater
			RPAREN!
			statement                     // statement to loop over

	// While statement
	|	"while"^ LPAREN! expression RPAREN! statement

	// do-while statement
	|	"do"^ statement "while"! LPAREN! expression RPAREN! SEMI!

	// get out of a loop (or switch)
	|	"break"^ (IDENT)? SEMI!

	// do next iteration of a loop
	|	"continue"^ (IDENT)? SEMI!

	// Return an expression
	|	"return"^ (expression)? SEMI!

	// switch/case statement
	|	"switch"^ LPAREN! expression RPAREN! LCURLY!
			( casesGroup )*
		RCURLY!

	// exception try-catch block
	|	tryBlock

	// throw an exception
	|	"throw"^ expression SEMI!

	// synchronize a statement
	|	"synchronized"^ LPAREN! expression RPAREN! compoundStatement

	// empty statement
	|	s:SEMI {#s->setType(EMPTY_STAT);}
	;


casesGroup
	:	(	// CONFLICT: to which case group do the statements bind?
			//           ANTLR generates proper code: it groups the
			//           many "case"/"default" labels together then
			//           follows them with the statements
			options {
				warnWhenFollowAmbig = false;
			}
			:
			aCase
		)+
		caseSList
		{#casesGroup = #([CASE_GROUP, "CASE_GROUP"], #casesGroup);}
	;

aCase
	:	("case"^ expression | "default") COLON!
	;

caseSList
	:	(statement)*
		{#caseSList = #(#[SLIST,"SLIST"],#caseSList);}
	;

// The initializer for a for loop
forInit
		// if it looks like a declaration, it is
	:	(	(declaration)=> declaration
		// otherwise it could be an expression list...
		|	expressionList
		)?
		{#forInit = #(#[FOR_INIT,"FOR_INIT"],#forInit);}
	;

forCond
	:	(expression)?
		{#forCond = #(#[FOR_CONDITION,"FOR_CONDITION"],#forCond);}
	;

forIter
	:	(expressionList)?
		{#forIter = #(#[FOR_ITERATOR,"FOR_ITERATOR"],#forIter);}
	;

// an exception handler try/catch block
tryBlock
	:	"try"^ compoundStatement
		(handler)*
		( "finally"^ compoundStatement )?
	;


// an exception handler
handler
	:	"catch"^ LPAREN! parameterDeclaration RPAREN! compoundStatement
	;


// expressions
// Note that most of these expressions follow the pattern
//   thisLevelExpression :
//       nextHigherPrecedenceExpression
//           (OPERATOR nextHigherPrecedenceExpression)*
// which is a standard recursive definition for a parsing an expression.
// The operators in java have the following precedences:
//    lowest  (13)  = *= /= %= += -= <<= >>= >>>= &= ^= |=
//            (12)  ?:
//            (11)  ||
//            (10)  &&
//            ( 9)  |
//            ( 8)  ^
//            ( 7)  &
//            ( 6)  == !=
//            ( 5)  < <= > >=
//            ( 4)  << >>
//            ( 3)  +(binary) -(binary)
//            ( 2)  * / %
//            ( 1)  ++ -- +(unary) -(unary)  ~  !  (type)
//                  []   () (method call)  . (dot -- identifier qualification)
//                  new   ()  (explicit parenthesis)
//
// the last two are not usually on a precedence chart; I put them in
// to point out that new has a higher precedence than '.', so you
// can validy use
//     new Frame().show()
// 
// Note that the above precedence levels map to the rules below...
// Once you have a precedence chart, writing the appropriate rules as below
//   is usually very straightfoward



// the mother of all expressions
expression
	:	assignmentExpression
		{#expression = #(#[EXPR,"EXPR"],#expression);}
	;


// This is a list of expressions.
expressionList
	:	expression (COMMA! expression)*
		{#expressionList = #(#[ELIST,"ELIST"], expressionList);}
	;


// assignment expression (level 13)
assignmentExpression
	:	conditionalExpression
		(	(	ASSIGN^
            |   PLUS_ASSIGN^
            |   MINUS_ASSIGN^
            |   STAR_ASSIGN^
            |   DIV_ASSIGN^
            |   MOD_ASSIGN^
            |   SR_ASSIGN^
            |   BSR_ASSIGN^
            |   SL_ASSIGN^
            |   BAND_ASSIGN^
            |   BXOR_ASSIGN^
            |   BOR_ASSIGN^
            )
			assignmentExpression
		)?
	;


// conditional test (level 12)
conditionalExpression
	:	logicalOrExpression
		( QUESTION^ conditionalExpression COLON! conditionalExpression )?
	;


// logical or (||)  (level 11)
logicalOrExpression
	:	logicalAndExpression (LOR^ logicalAndExpression)*
	;


// logical and (&&)  (level 10)
logicalAndExpression
	:	inclusiveOrExpression (LAND^ inclusiveOrExpression)*
	;


// bitwise or non-short-circuiting or (|)  (level 9)
inclusiveOrExpression
	:	exclusiveOrExpression (BOR^ exclusiveOrExpression)*
	;


// exclusive or (^)  (level 8)
exclusiveOrExpression
	:	andExpression (BXOR^ andExpression)*
	;


// bitwise or non-short-circuiting and (&)  (level 7)
andExpression
	:	equalityExpression (BAND^ equalityExpression)*
	;


// equality/inequality (==/!=) (level 6)
equalityExpression
	:	relationalExpression ((NOT_EQUAL^ | EQUAL^) relationalExpression)*
	;


// boolean relational expressions (level 5)
relationalExpression
	:	shiftExpression
		(	(	LT_^
			|	GT^
			|	LE^
			|	GE^
			)
			shiftExpression
		)*
	;


// bit shift expressions (level 4)
shiftExpression
	:	additiveExpression ((SL^ | SR^ | BSR^) additiveExpression)*
	;


// binary addition/subtraction (level 3)
additiveExpression
	:	multiplicativeExpression ((PLUS^ | MINUS^) multiplicativeExpression)*
	;


// multiplication/division/modulo (level 2)
multiplicativeExpression
	:	castExpression ((STAR^ | DIV^ | MOD^ ) castExpression)*
	;

// cast/unary (level 1)
castExpression
	// if it _looks_ like a cast, it _is_ a cast
	:	( LPAREN typeSpec[true] RPAREN castExpression )=>
			lp:LPAREN^ {#lp->setType(TYPECAST);} typeSpec[true] RPAREN!
			c:castExpression

	// otherwise it's a unary expression
	|	INC^ castExpression
	|	DEC^ castExpression
	|	MINUS^ {#MINUS->setType(UNARY_MINUS);} castExpression
	|	PLUS^  {#PLUS->setType(UNARY_PLUS);} castExpression
	|	BNOT^ castExpression
	|	LNOT^ castExpression
	|	postfixExpression ( "instanceof"^ typeSpec[true] )?
		// instanceof should not allow just primitives (x instanceof int)
		// need a semantic check if we're compiling...
	;

// qualified names, array expressions, method invocation, post inc/dec
postfixExpression
	:	primaryExpression // start with a primary

		
		(	// qualified id (id.id.id.id...) -- build the name
			DOT^ ( IDENT
				| "this"
				| "class"
				| newExpression
				| "super" LPAREN ( expressionList )? RPAREN
				)
			// the above line needs a semantic check to make sure "class"
			//   is the _last_ qualifier.

			// an array indexing operation
		|	lb:LBRACK^ {#lb->setType(INDEX_OP);} expression RBRACK!

			// method invocation
			// The next line is not strictly proper; it allows x(3)(4) or
			//   x[2](4) which are not valid in Java.  If this grammar were used
			//   to validate a Java program a semantic check would be needed, or
			//   this rule would get really ugly...
		|	lp:LPAREN^ {#lp->setType(METHOD_CALL);}
				argList
			RPAREN!
		)*

			// possibly add on a post-increment or post-decrement
		(	in:INC^ {#in->setType(POST_INC);}
	 	|	de:DEC^ {#de->setType(POST_DEC);}
		|	// nothing
		)
	;

// the basic element of an expression
primaryExpression
	:	IDENT
	|	builtInType 
		( lb:LBRACK^ {#lb->setType(ARRAY_DECLARATOR);} RBRACK! )*
		DOT^ "class"
	|	newExpression
	|	constant
	|	"super"
	|	"true"
	|	"false"
	|	"this"
	|	"null"
	|	LPAREN! assignmentExpression RPAREN!
	;


/** object instantiation.
 *  Trees are built as illustrated by the following input/tree pairs:
 *  
 *  new T()
 *  
 *  new
 *   |
 *   T --  ELIST
 *           |
 *          arg1 -- arg2 -- .. -- argn
 *  
 *  new int[]
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *  
 *  new int[] {1,2}
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR -- ARRAY_INIT
 *                                  |
 *                                EXPR -- EXPR
 *                                  |      |
 *                                  1      2
 *  
 *  new int[3]
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *                |
 *              EXPR
 *                |
 *                3
 *  
 *  new int[1][2]
 *  
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *               |
 *         ARRAY_DECLARATOR -- EXPR
 *               |              |
 *             EXPR             1
 *               |
 *               2
 *  
 */
newExpression
	:	"new"^ type
		(	LPAREN! argList RPAREN! (classBlock)?

			//java 1.1
			// Note: This will allow bad constructs like
			//    new int[4][][3] {exp,exp}.
			//    There needs to be a semantic check here...
			// to make sure:
			//   a) [ expr ] and [ ] are not mixed
			//   b) [ expr ] and an init are not used together

		|	newArrayDeclarator (arrayInitializer)?
		)
	;

argList
	:	(	expressionList
		|	/*nothing*/
			{#argList = #[ELIST,"ELIST"];}
		)
	;

newArrayDeclarator
	:	(
			// CONFLICT:
			// newExpression is a primaryExpression which can be
			// followed by an array index reference.  This is ok,
			// as the generated code will stay in this loop as
			// long as it sees an LBRACK (proper behavior)
			options {
				warnWhenFollowAmbig = false;
			}
		:
			lb:LBRACK^ {#lb->setType(ARRAY_DECLARATOR);}
				(expression)?
			RBRACK!
		)+
	;

constant
	:	NUM_INT
	|	CHAR_LITERAL
	|	STRING_LITERAL
	|	NUM_FLOAT
	;


