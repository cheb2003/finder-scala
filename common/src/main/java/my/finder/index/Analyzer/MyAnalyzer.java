package my.finder.index.Analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;

/**
 *
 */
public class MyAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        final WhitespaceTokenizer src = new WhitespaceTokenizer(Version.LUCENE_40, reader);

        TokenStream tok = new LowerCaseFilter(Version.LUCENE_40, src);

        return new TokenStreamComponents(src, tok) {
            @Override
            protected void setReader(final Reader reader) throws IOException {
                super.setReader(reader);
            }
        };
    }
}
