/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cpd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.cpd.SourceCode.StringCodeLoader;

public class CPPTokenizerContinuationTest {

    @Test
    public void parseWithContinuation() throws Exception {
        String code = load("cpp_with_continuation.cpp");
        Tokens tokens = parse(code);
        if (tokens.size() < 53) {
            printTokens(tokens);
            fail("Not enough tokens - probably parsing error");
        }

        assertEquals("static", findByLine(8, tokens).get(0).toString());
        assertEquals("int", findByLine(8, tokens).get(1).toString());

        // Note: the token should be "ab", but since we skip "\\\n" between a and b,
        // we end up with two tokens. -> the tokens are not joined.
        // This could lead to false negatives in duplication detection.
        // However, this is only a problem, if the continuation character is added *within*
        // a token and not at token boundaries.
        // see also test #testContinuationIntraToken
        //assertEquals("ab", findByLine(8, tokens).get(2).toString());

        assertEquals("int", findByLine(12, tokens).get(0).toString());
        assertEquals("main", findByLine(12, tokens).get(1).toString());
        assertEquals("(", findByLine(12, tokens).get(2).toString());
        assertEquals(")", findByLine(12, tokens).get(3).toString());
        assertEquals("{", findByLine(13, tokens).get(0).toString());
        assertEquals("}", findByLine(29, tokens).get(0).toString());
    }

    @Test
    @Ignore
    public void testContinuationIntraToken() throws Exception {
        Tokens tokens = parse(load("cpp_continuation_intra_token.cpp"));
        assertEquals(7, tokens.size());
    }

    @Test
    public void testContinuationInterToken() throws Exception {
        Tokens tokens = parse(load("cpp_continuation_inter_token.cpp"));
        assertEquals(17, tokens.size());
    }

    private void printTokens(Tokens tokens) {
        for (TokenEntry entry : tokens.getTokens()) {
            System.out.printf("%02d: %s%s", entry.getBeginLine(), entry.toString(), PMD.EOL);
        }
    }

    private List<TokenEntry> findByLine(int line, Tokens tokens) {
        List<TokenEntry> result = new ArrayList<>();
        for (TokenEntry entry : tokens.getTokens()) {
            if (entry.getBeginLine() == line) {
                result.add(entry);
            }
        }
        if (result.isEmpty()) {
            fail("No tokens at line " + line + " found");
        }
        return result;
    }

    private String load(String name) throws Exception {
        return IOUtils.toString(CPPTokenizerContinuationTest.class
                .getResourceAsStream("cpp/" + name), StandardCharsets.UTF_8);
    }

    private Tokens parse(String code) {
        CPPTokenizer tokenizer = new CPPTokenizer();
        tokenizer.setProperties(new Properties());
        Tokens tokens = new Tokens();
        tokenizer.tokenize(new SourceCode(new StringCodeLoader(code)), tokens);
        return tokens;
    }
}
