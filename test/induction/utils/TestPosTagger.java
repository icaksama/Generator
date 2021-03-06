package induction.utils;

import induction.utils.postagger.PosTagger;
import induction.utils.postagger.PosTaggerOptions;
import fig.exec.Execution;
import junit.framework.TestCase;
import org.junit.Test;

/**
 *
 * @author konstas
 */
public class TestPosTagger extends TestCase
{

    PosTagger posTagger;

    public TestPosTagger(String name)
    {
        super(name);
    }

//    @Test
//    public void testAtisPosTagger()
//    {               
//        posTagger = new PosTagger("data/atis/train/atis5000.sents.full", 
//                                  PosTagger.TypeOfPath.file, 
//                                  PosTagger.TypeOfInput.events, 
//                                 "/home/konstas/EDI/candc/candc-1.00/atis_tagged_manual_disambiguated.out_sorted",
//                                 false,
//                                 false,
//                                 "");
//        posTagger.execute();
//    }
    @Test
    public void testWinHelpPosTagger()
    {                      
        String args =
//                  "-inputPath data/branavan/winHelpHLA/folds/sents.newAnnotation/winHelpFold10Train "
                  "-inputPath data/branavan/winHelpHLA/folds/docs.newAnnotation/winHelpFold1Train "
//                  "-inputPath data/branavan/winHelpHLA/winHelpRL.cleaned.objType.norm.sents.all.newAnnotation "
                + "-typeOfPath file "
                + "-typeOfInput events "
//                + "-posDictionaryPath data/branavan/winHelpHLA/winHelpRL.sents.all.vocabulary "
                + "-outputExampleFreq 100 "
                + "-extension text "
                + "-tagDelimiter _ ";
//                + "-forceTagger";
        PosTaggerOptions opts = new PosTaggerOptions();
        Execution.init(args.split(" "), new Object[]{opts}); // parse input params
        posTagger = new PosTagger(opts);        
        posTagger.execute();   
    }
    
//    @Test
//    public void testWeatherGovPosTagger()
//    {
//        String args =
//                  "-inputPath test/testWeatherGovEvents "
//                + "-typeOfPath list "
//                + "-typeOfInput raw "
//                + "-posDictionaryPath gaborLists/trainListPathsGabor_vocabulary_manual "
//                + "-extension text "
////                + "-replaceNumbers "
//                + "-forceTagger";
//        PosTaggerOptions opts = new PosTaggerOptions();
//        Execution.init(args.split(" "), new Object[]{opts}); // parse input params
//        posTagger = new PosTagger(opts);        
//        posTagger.execute();   
//    }
    
//    @Test
//    public void testRobocupPosTagger()
//    {
//        String args =
//                  "-inputPath robocupLists/robocupAllPathsTrain "
////                  "-inputPath robocupLists/error_tagged "
//                + "-typeOfPath list "
//                + "-typeOfInput raw "
//                + "-posDictionaryPath robocupLists/robocup_vocabulary_manual "
//                + "-extension text "
////                + "-replaceNumbers "
//                + "-forceTagger";
//        PosTaggerOptions opts = new PosTaggerOptions();
//        Execution.init(args.split(" "), new Object[]{opts}); // parse input params
//        posTagger = new PosTagger(opts);        
//        posTagger.execute();   
//    }
    
//    @Test
//    public void testDistributionOfFiles()
//    {
////        String list = "gaborLists/genDevListPathsGabor";
//        String list = "gaborLists/genEvalListPathsGabor";
//        for(String filename : Utils.readLines(list))
//        {
//            filename = filename.replaceAll("events", "text");
//            try {
//                System.out.println(Utils.readFileAsString(filename).split("\\p{Space}").length);
//            }
//            catch(IOException ioe) {}
//        }
//    }
}
