//
// Pascal Parser Grammar
//
// Adapted from,
// Pascal User Manual And Report (Second Edition-1978)
// Kathleen Jensen - Niklaus Wirth
//
// Hakki Dogusan dogusanh@tr-net.net.tr
//


// Import the necessary classes
{
  import java.io.*;
}


//-----------------------------------------------------------------------------
// Define a Parser, calling it PascalParser
//-----------------------------------------------------------------------------
class PascalParser extends Parser;
options {
  k = 2;                           // two token lookahead
  exportVocab=Pascal;              // Call its vocabulary "Pascal"
  codeGenMakeSwitchThreshold = 2;  // Some optimizations
  codeGenBitsetTestThreshold = 3;
  defaultErrorHandler = false;     // Don't generate parser error handlers
  buildAST = false;
}


// Define some methods and variables to use in the generated parser.
{
  // Define a main
  public static void main(String[] args) {
    // Use a try/catch block for parser exceptions
    try {
      // if we have at least one command-line argument
      if (args.length > 0 ) {
        System.err.println("Parsing...");

        // for each directory/file specified on the command line
        for(int i=0; i< args.length;i++)
          doFile(new File(args[i])); // parse it
      }
      else
        System.err.println("Usage: java PascalParser <file/directory name>");

    }
    catch(Exception e) {
      System.err.println("exception: "+e);
      e.printStackTrace(System.err);   // so we can get stack trace
    }
  }


  // This method decides what action to take based on the type of
  //   file we are looking at
  public static void doFile(File f) throws Exception {
    // If this is a directory, walk each file/dir in that directory
    if (f.isDirectory()) {
      String files[] = f.list();
      for(int i=0; i < files.length; i++)
        doFile(new File(f, files[i]));
    }

    // otherwise, if this is a Pascal file, parse it!
    else if ((f.getName().length()>4) &&
             f.getName().substring(f.getName().length()-4).equals(".pas")) {
      System.err.println("   "+f.getAbsolutePath());
      parseFile(new FileInputStream(f));
    }
  }

  // Here's where we do the real work...
  public static void parseFile(InputStream s) throws Exception {
    try {
      // Create a scanner that reads from the input stream passed to us
      PascalLexer lexer = new PascalLexer(s);

      // Create a parser that reads from the scanner
      PascalParser parser = new PascalParser(lexer);

      // start parsing at the program rule
	parser.program(); 
    }
    catch (Exception e) {
      System.err.println("parser exception: "+e);
      e.printStackTrace();   // so we can get stack trace
    }
  }
}


program
	: programHeading
      block
      DOT
	;

programHeading
    : PROGRAM identifier
      LPAREN fileIdentifier ( COMMA fileIdentifier )* RPAREN
      SEMI
	;

fileIdentifier
    : identifier
    ;

identifier
    : IDENT
    ;

block
    : ( labelDeclarationPart
      | constantDefinitionPart
      | typeDefinitionPart
      | variableDeclarationPart
      | procedureAndFunctionDeclarationPart
      )*
      statementPart
    ;

labelDeclarationPart
    : LABEL label ( COMMA label )* SEMI
    ;

label
    : unsignedInteger
    ;

constantDefinitionPart
    : CONST constantDefinition ( SEMI constantDefinition )* SEMI
    ;

constantDefinition
    : identifier EQUAL constant
    ;

constant
    : unsignedNumber
    | sign unsignedNumber
    | constantIdentifier
    | sign constantIdentifier
    | string
    ;


unsignedNumber
    : unsignedInteger
    | unsignedReal
    ;

unsignedInteger
    : NUM_INT
    ;

unsignedReal
    : NUM_REAL
    ;

sign
    : PLUS | MINUS
    ;

constantIdentifier
    : identifier
    ;

string
    : STRING_LITERAL
    ;

typeDefinitionPart
    : TYPE typeDefinition ( SEMI typeDefinition )* SEMI
    ;

typeDefinition
    : identifier EQUAL type
    ;

type
    : simpleType
    | structuredType
    | pointerType
    ;

simpleType
    : scalarType
    | subrangeType
    | typeIdentifier
    ;

scalarType
    : LPAREN identifier ( COMMA identifier )* RPAREN
    ;

subrangeType
    : constant DOTDOT constant
    ;

typeIdentifier
    : identifier
    | CHAR
    | BOOLEAN
    | INTEGER
    | REAL
    ;

structuredType
    : ( PACKED
      | empty
      ) unpackedStructuredType
    ;

unpackedStructuredType
    : arrayType
    | recordType
    | setType
    | fileType
    ;

arrayType
    : ARRAY LBRACK indexType ( COMMA indexType )* RBRACK OF
      componentType
    ;

indexType
    : simpleType
    ;

componentType
    : type
    ;

recordType
    : RECORD fieldList END
    ;

fieldList
    : fixedPart
        ( SEMI variantPart
        | empty
        )
    | variantPart
    ;

fixedPart
    : recordSection ( SEMI recordSection )*
    ;

recordSection
    : fieldIdentifier ( COMMA fieldIdentifier )* COLON type
    | empty
    ;

variantPart
    : CASE tagField typeIdentifier OF
      variant ( SEMI variant )*
    ;

tagField
    : fieldIdentifier COLON
    | empty
    ;

variant
    : caseLabelList COLON LPAREN fieldList RPAREN
    | empty
    ;

caseLabelList
    : caseLabel ( COMMA caseLabel )*
    ;

caseLabel
    : constant
    ;

setType
    : SET OF baseType
    ;

baseType
    : simpleType
    ;

fileType
    : FILE OF type
    ;

pointerType
    : POINTER typeIdentifier
    ;

variableDeclarationPart
    : VAR variableDeclaration ( SEMI variableDeclaration )* SEMI
    ;

variableDeclaration
    : identifier ( COMMA identifier )* COLON type
    ;

procedureAndFunctionDeclarationPart
    : procedureOrFunctionDeclaration SEMI
    ;

procedureOrFunctionDeclaration
    : procedureDeclaration
    | functionDeclaration
    ;

procedureDeclaration
    : procedureHeading
      block
    ;

procedureHeading
    : PROCEDURE identifier parameterList SEMI
    ;

parameterList
    : empty
    | LPAREN formalParameterSection ( SEMI formalParameterSection )* RPAREN
    ;

formalParameterSection
    : parameterGroup
    | VAR parameterGroup
    | FUNCTION parameterGroup
    | PROCEDURE identifier ( COMMA identifier )*
    ;

parameterGroup
    : identifier ( COMMA identifier )* COLON typeIdentifier
    ;

functionDeclaration
    : functionHeading
      block
    ;

functionHeading
    : FUNCTION identifier  parameterList  COLON resultType SEMI
    ;

resultType
    : typeIdentifier
    ;

statementPart
    : compoundStatement
    ;

statement
    : ( label COLON
      | empty
      )
      unlabelledStatement
    ;

unlabelledStatement
    : simpleStatement
    | structuredStatement
    ;

simpleStatement
    : assignmentStatement
    | procedureStatement
    | gotoStatement
    | emptyStatement
    ;

assignmentStatement
    : variable ASSIGN expression
    | functionIdentifier ASSIGN expression
    ;

variable
    : entireVariable
    | componentVariable
    | referencedVariable
    ;

entireVariable
    : variableIdentifier
    ;

variableIdentifier
    : identifier
    ;

componentVariable
    : indexedVariable
    | fieldDesignator
    | fileBuffer
    ;

indexedVariable
    : arrayVariable LBRACK expression ( COMMA expression)* RBRACK
    ;

arrayVariable
    : identifier
    ;

fieldDesignator
    : recordVariable DOT fieldIdentifier
    ;

recordVariable
    : identifier
    ;

fieldIdentifier
    : identifier
    ;

fileBuffer
    : fileVariable POINTER
    ;

fileVariable
    : identifier
    ;

referencedVariable
    : pointerVariable POINTER
    ;

pointerVariable
    : identifier
    ;

expression
    : simpleExpression
      ( empty
      | relationalOperator simpleExpression
      )
    ;

relationalOperator
    : EQUAL | NOT_EQUAL | LT | LE | GE | GT | IN
    ;

simpleExpression
    : ( sign
      | empty
      )
      term ( addingOperator term )*
    ;

addingOperator
    : PLUS | MINUS | OR
    ;

term
    : factor ( multiplyingOperator factor )*
    ;

multiplyingOperator
    : STAR | SLASH | DIV | MOD | AND
    ;

factor
    : variable
    | unsignedConstant
    | LPAREN expression RPAREN
    | functionDesignator
    | set
    | NOT factor
    ;

unsignedConstant
    : unsignedNumber
    | string
    | constantIdentifier
    | NIL
    ;

functionDesignator
    : functionIdentifier
        ( LPAREN actualParameter ( COMMA actualParameter ) * RPAREN
        | empty
        )
    ;

functionIdentifier
    : identifier
    ;

set
    : LBRACK elementList RBRACK
    ;

elementList
    : element ( COMMA element )*
    | empty
    ;

element
    : expression
        ( DOTDOT expression
        | empty
        )
    ;

procedureStatement
    : procedureIdentifier
        ( LPAREN actualParameter ( COMMA actualParameter )* RPAREN
        | empty
        )
    ;

procedureIdentifier
    : identifier
    ;

actualParameter
    : expression
    | variable
    | procedureIdentifier
    | functionIdentifier
    ;

gotoStatement
    : GOTO label
    ;

emptyStatement
    : empty
    ;

empty
    : /* empty */
    ;

structuredStatement
    : compoundStatement
    | conditionalStatement
    | repetetiveStatement
    | withStatement
    ;

compoundStatement
    : BEGIN
        statement ( SEMI statement )*
      END
    ;

conditionalStatement
    : ifStatement
    | caseStatement
    ;

ifStatement
    : IF expression THEN statement
      ( ELSE statement
      | empty
      )
    ;

caseStatement
    : CASE expression OF
        caseListElement ( SEMI caseListElement )*
      END
    ;

caseListElement
    : caseLabelList COLON statement
    | empty
    ;

repetetiveStatement
    : whileStatement
    | repeatStatement
    | forStatement
    ;

whileStatement
    : WHILE expression DO
        statement
    ;

repeatStatement
    : REPEAT
        statement ( SEMI statement )*
      UNTIL expression
    ;

forStatement
    : FOR controlVariable ASSIGN forList DO
        statement
    ;

forList
    : initialValue ( TO | DOWNTO ) finalValue
    ;

controlVariable
    : identifier
    ;

initialValue
    : expression
    ;

finalValue
    : expression
    ;


withStatement
    : WITH recordVariableList DO
        statement
    ;

recordVariableList
    : recordVariable ( COMMA recordVariable )*
    ;


//----------------------------------------------------------------------------
// The Pascal scanner
//----------------------------------------------------------------------------
class PascalLexer extends Lexer;

options {
  charVocabulary = '\0'..'\377';
  exportVocab = Pascal;   // call the vocabulary "Pascal"
  testLiterals = false;   // don't automatically test for literals
  k = 4;                  // four characters of lookahead
  caseSensitive = false;
  caseSensitiveLiterals = false;
}

tokens {
  AND              = "and"             ;
  ARRAY            = "array"           ;
  BEGIN            = "begin"           ;
  BOOLEAN          = "boolean"         ;
  CASE             = "case"            ;
  CHAR             = "char"            ;
  CONST            = "const"           ;
  DIV              = "div"             ;
  DO               = "do"              ;
  DOWNTO           = "downto"          ;
  ELSE             = "else"            ;
  END              = "end"             ;
  FILE             = "file"            ;
  FOR              = "for"             ;
  FUNCTION         = "function"        ;
  GOTO             = "goto"            ;
  IF               = "if"              ;
  IN               = "in"              ;
  INTEGER          = "integer"         ;
  LABEL            = "label"           ;
  MOD              = "mod"             ;
  NIL              = "nil"             ;
  NOT              = "not"             ;
  OF               = "of"              ;
  OR               = "or"              ;
  PACKED           = "packed"          ;
  PROCEDURE        = "procedure"       ;
  PROGRAM          = "program"         ;
  REAL             = "real"            ;
  RECORD           = "record"          ;
  REPEAT           = "repeat"          ;
  SET              = "set"             ;
  THEN             = "then"            ;
  TO               = "to"              ;
  TYPE             = "type"            ;
  UNTIL            = "until"           ;
  VAR              = "var"             ;
  WHILE            = "while"           ;
  WITH             = "with"            ;
}

//----------------------------------------------------------------------------
// OPERATORS
//----------------------------------------------------------------------------
PLUS            : '+'   ;
MINUS           : '-'   ;
STAR            : '*'   ;
SLASH           : '/'   ;
ASSIGN          : ":="  ;
COMMA           : ','   ;
SEMI            : ';'   ;
COLON           : ':'   ;
EQUAL           : '='   ;
NOT_EQUAL       : "<>"  ;
LT              : '<'   ;
LE              : "<="  ;
GE              : ">="  ;
GT              : '>'   ;
LPAREN          : '('   ;
RPAREN          : ')'   ;
LBRACK          : '['   ;
RBRACK          : ']'   ;
POINTER         : '^'   ;
//DOT             : '.'   ;
//DOTDOT          : ".."  ;


// Whitespace -- ignored
WS      : ( ' '
		|	'\t'
		|	'\f'
		// handle newlines
		|	(	"\r\n"  // Evil DOS
			|	'\r'    // Macintosh
			|	'\n'    // Unix (the right way)
			)
			{ newline(); }
		)
		{ _ttype = Token.SKIP; }
	;


COMMENT_1
        : "(*"
		   ( options { generateAmbigWarnings=false; }
		   :	{ LA(2) != ')' }? '*'
		   |	'\r' '\n'		{newline();}
		   |	'\r'			{newline();}
		   |	'\n'			{newline();}
           |   ~('*' | '\n' | '\r')
		   )*
          "*)"
		{$setType(Token.SKIP);}
	;

COMMENT_2
        :  '{'
		    ( options {generateAmbigWarnings=false;}
            :   '\r' '\n'       {newline();}
		    |	'\r'			{newline();}
		    |	'\n'			{newline();}
            |   ~('}' | '\n' | '\r')
		    )*
           '}'
		{$setType(Token.SKIP);}
	;

// an identifier.  Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
IDENT
	options {testLiterals=true;}
	:	('a'..'z') ('a'..'z'|'0'..'9')*
	;

// string literals
STRING_LITERAL
	: '\'' ("\'\'" | ~('\''))+ '\''
	;

// a numeric literal
NUM_INT
	{boolean isDecimal=false;}
	:	".." {_ttype = DOTDOT;}
    |   '.'  {_ttype = DOT;}
		(('0'..'9')+ (EXPONENT)? { _ttype = NUM_REAL; })?
	|	(	'0' {isDecimal = true;} // special case for just '0'
		|	('1'..'9') ('0'..'9')*  {isDecimal=true;}		// non-zero decimal
		)
		// only check to see if it's a float if looks like decimal so far
		(	{ LA(2)!='.' && LA(3)!='.' && isDecimal}?
			(	'.' ('0'..'9')* (EXPONENT)?
			|	EXPONENT
			)
			{ _ttype = NUM_REAL; }
		)?
	;


// a couple protected methods to assist in matching floating point numbers
protected
EXPONENT
	:	('e') ('+'|'-')? ('0'..'9')+
	;






