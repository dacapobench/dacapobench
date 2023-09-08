options
{
	language = "CSharp";
}

class ExprParser extends Parser;

options {
	codeGenMakeSwitchThreshold = 3;
	codeGenBitsetTestThreshold = 4;
	buildAST=true;
	ASTLabelType = "antlr.CommonAST"; // change default of "AST"
}

expr : assignExpr EOF! ;

assignExpr
	:	addExpr
		(
			ASSIGN^
			assignExpr 
		)?
	;

addExpr
	:	multExpr 
		(
			pm:PLUS_MINUS^
			me:multExpr
			exception 
				catch [ RecognitionException ex ] 
				{ 
					Console.Out.WriteLine("Caught error in addExpr");
					reportError(ex.Message); 
				}
		)*
	;

multExpr
	:	postfixExpr
		(
			MULT_DIV^
			postfixExpr
		)*
	;

postfixExpr
	:	(id:ID LPAREN)=>
		// Matches function call syntax like "id(arg,arg)" 
		id2:ID^
		(
         parenArgs
		)?
	|	atom
	;

parenArgs
	:	
      LPAREN!
      (
         assignExpr
         (
            COMMA!
	        assignExpr
         )*
      )?
      RPAREN!
	;

atom
	:	ID
	|	INT
	|	CHAR_LITERAL 
	|	STRING_LITERAL
	|	LPAREN! assignExpr RPAREN!
	;

class ExprLexer extends Lexer;

WS	:	(' '
	|	'\t'
	|	'\n'
	|	'\r')
		{ _ttype = Token.SKIP; }
	;

LPAREN:	'('
	;

RPAREN:	')'
	;

PLUS_MINUS:	'+' | '-'
	;

MULT_DIV : '*' | '/'
   ;

ASSIGN :	'='
	;

COMMA : ','
   ;
   
CHAR_LITERAL
	:	'\'' (ESC|~'\'') '\''
	;

STRING_LITERAL
	:	'"' (ESC|~'"')* '"'
	;

protected
ESC	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	('0'..'3')
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	('0'..'9')
				(	
					options {
						warnWhenFollowAmbig = false;
					}
				:	'0'..'9'
				)?
			)?
		|	('4'..'7')
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	('0'..'9')
			)?
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

INT 
	: (DIGIT)+
	;

ID
options {
	testLiterals = true;
}
	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
	;

