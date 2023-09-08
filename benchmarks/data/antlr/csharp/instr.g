options
{
	language = "CSharp";
}

class InstrParser extends Parser;
options {
	buildAST = true;
	k=2;
}

tokens {
	CALL; // define imaginary token CALL
}

slist
	:	( stat )+
	;

stat:	LBRACE^ (stat)+ RBRACE
	|	"if"^ expr "then" stat ("else" stat)?
	|	ID ASSIGN^ expr SEMI
	|	call
	;

expr
	:	mexpr (PLUS^ mexpr)*
	;

mexpr
	:	atom (STAR^ atom)*
	;

atom:	INT
	|	ID
	;

call:	ID LPAREN (expr)? RPAREN SEMI
		{#call = #(#[CALL,"CALL"], #call);}
	;

class InstrLexer extends Lexer;
options {
	charVocabulary = '\3'..'\377';
}

WS	:	(' '
		|	'\t'
		|	('\n'|'\r'('\n')?) {newline();}
		)+
	;

// Single-line comments
SL_COMMENT
	:	"//"
		(~('\n'|'\r'))* ('\n'|'\r'('\n')?)
		{newline();}
	;

LBRACE:	'{'
	;

RBRACE:	'}'
	;

LPAREN:	'('
	;

RPAREN:	')'
	;

STAR:	'*'
	;

PLUS:	'+'
	;

SEMI:	';'
	;

ASSIGN
	:	'='
	;

protected
DIGIT
	:	'0'..'9'
	;

INT	:	(DIGIT)+
	;

ID	:	('a'..'z')+
	;

class InstrTreeWalker extends TreeParser;
{
	/** walk list of hidden tokens in order, printing them out */
	public static void dumpHidden(antlr.CommonHiddenStreamToken t) {
	  for ( ; t!=null ; t=InstrMain.filter.getHiddenAfter(t) ) {
	    Console.Error.Write(t.getText());
	  }
	}

	private void pr(AST p) {
		Console.Out.Write(p.getText());
		dumpHidden(
			((antlr.CommonASTWithHiddenTokens)p).getHiddenAfter()
		);
	}
}

slist
	:	{dumpHidden(InstrMain.filter.getInitialHiddenToken());}
		(stat)+
	;

stat:	#(LBRACE {pr(#LBRACE);} (stat)+ RBRACE {pr(#RBRACE);})
	|	#(i:"if" {pr(i);} expr t:"then" {pr(t);} stat (e:"else" {pr(e);} stat)?)
	|	#(ASSIGN ID {pr(#ID); pr(#ASSIGN);} expr SEMI {pr(#SEMI);} )
	|	call
	;

expr
	:	#(PLUS expr {pr(#PLUS);} expr)
	|	#(STAR expr {pr(#STAR);} expr)
	|	INT {pr(#INT);}
	|	ID  {pr(#ID);}
	;

call:	{
		// add instrumentation about call; manually call rule
		callDumpInstrumentation(#call);
		}
		#(CALL ID {pr(#ID);}
		  LPAREN {pr(#LPAREN);} (expr)? RPAREN {pr(#RPAREN);}
		  SEMI
		  {
		  // print SEMI manually; need '}' between it and whitespace
		  Console.Error.Write(#SEMI.getText());
		  Console.Error.Write("}"); // close {...} of instrumentation
		  dumpHidden(
			((antlr.CommonASTWithHiddenTokens)#SEMI).getHiddenAfter()
		  );
		  }
		)
	;

/** Dump instrumentation for a call statement.
 *  The reference to rule expr prints out the arg
 *  and then at the end of this rule, we close the
 *  generated called to dbg.invoke().
 */
callDumpInstrumentation
	:	#(CALL id:ID
		  {Console.Error.Write("{dbg.invoke(\""+id.getText()+"\", \"");}
		  LPAREN (e:expr)? RPAREN SEMI
		  {Console.Error.Write("\"); ");}
		)
	;

