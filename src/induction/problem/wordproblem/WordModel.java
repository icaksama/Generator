package induction.problem.wordproblem;

import fig.basic.Indexer;
import fig.exec.Execution;
import induction.Options;
import induction.Utils;
import induction.problem.AModel;

/**
 *
 * @author konstas
 */
public abstract class WordModel extends AModel
//                                <Widget extends AWidget,
//                                Params extends AParams,
////                                Performance extends APerformance<Widget>,
//                                Example extends WordExample<Widget>>//,
//                                //InferState extends AInferState<Widget, Example, Params> >
//                      extends AModel<Widget, Params, 
////                                     Performance, 
//                                     Example>//, InferState>
{

    protected Indexer<String> wordIndexer = new Indexer<String>();
    protected int[] wordFreqs = null; // Word frequencies
   
    public WordModel(Options opts)
    {
        super(opts);
    }

    /**
     *
     * @return number of words
     */
    public int W()
    {
        return wordIndexer.size();
    }

    public String wordToString(int w)
    {
        if(w > -1)
            return wordIndexer.getObject(w);
        else
//            return "N/A";
            return "";
    }
    
    public static String wordToString(Indexer<String> wordIndexer, int w, boolean stripPosTag, String tagDelimiter)
    {
        if(w > -1)
            return stripPosTag ? Utils.stripTag(wordIndexer.getObject(w), tagDelimiter) : 
                    wordIndexer.getObject(w);
        else
            return "";
    }

    public String[] wordsToStringArray()
    {
        String[] out = new String[wordIndexer.size()];
        return wordIndexer.getObjects().toArray(out);
    }

    public Indexer<String> getWordIndexer()
    {
        return wordIndexer;
    }
    
    @Override
    public void logStats()
    {
        super.logStats();
        Execution.putLogRec("numWords", W());
    }

    @Override
    public void readExamples()
    {
        super.readExamples();
//        wordFreqs = new int[W()];
////      examples.foreach { ex => ex.words.foreach(wordFreqs(_) += 1) }
//        for(Example ex : examples)
//        {
//            for(int i = 0; i < ex.words.length; i++)
//            {
//                // I assume we want to capture the global word frequencies,
//                // so I add the word frequencies of each example, provided
//                // that the words array corresponds to all the words in the corpus
//                // (bag of words).
//                wordFreqs[i] += ex.words[i];
//            }
//        } // for
    }
}
