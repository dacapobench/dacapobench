// adapted from snippet sent to the antlr-interest list by Remi Koutcherawy
// RK: Added some tab handling stuff to it.
options {
	language="Cpp";
}

{
#include "L.hpp"

	int main( void )
	{
		try
		{
			L lexer(cin);
			lexer.set_tabsize(8);

			P parser(lexer);

			parser.start();
		}
		catch( exception& e )
		{
			cerr << "exception: " << e.what() << endl;
		}
	}
}

class P extends Parser;
start
  : (((WS)? t:NB
         { cout << "\"" <<  t->getText() << "\""
					<< " line " << t->getLine() << " col " << t->getColumn()
						<< endl;
			}
    )* NL)*
  ;

{
	// inserted into generated C++ file
	void L::tab(void)
	{
		unsigned int c, nc;

		c = getColumn();										// get current column...
		nc = ( ((c-1)/tabsize) + 1) * tabsize + 1;	// calculate tabstop
		setColumn( nc );										// set it...
	}
	unsigned int L::set_tabsize( unsigned int tsize )
	{
		unsigned int old = this->tabsize;
		tabsize = tsize;
		return old;
	}
}
class L extends Lexer;
{
	// these get inserted into the generated class
public:
	/** handles encountered tabs (it is called from the consume method in
	 * CharScanner (superclass of lexers)
	 * the default version increases the column by 1
	 */
	void tab( void );
	/// set tabsize, returns previous value
	unsigned int set_tabsize( unsigned int tab );
protected:
	unsigned int tabsize;
}
NL  : '\n' { newline(); }
    ;
WS  : ' '
	 | '\t'
    ;
NB  : ('0'..'9')
    ;
