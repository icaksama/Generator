package induction.utils;

import induction.Options;
import induction.Utils;
import induction.problem.event3.Event;
import induction.problem.event3.Event3Model;
import induction.problem.event3.Field;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author konstas
 */
public class ExportScenariosToWebExp2
{
    private final String HUMAN = "human",
                         MODEL = "model",
                         BASELINE = "baseline",
                         GABOR = "gabor";
    private final String scenariosPath, imagesPathUrl, outputPath, goldPath;
    private boolean nullifyOrder, outputAllFields;
    private final Properties properties;
    private String modelPath, baselinePath, gaborPath;
    private Event3Model model;
    private List<Scenario> scenariosList;
    private enum Type {model, baseline};
    private List<String> filterFieldsList;

    public ExportScenariosToWebExp2(String scenariosPath, String imagesPathUrl,
                                    String outputPath, String propertiesPath,
                                    String goldPath)
    {
        this.scenariosPath = scenariosPath;
        this.imagesPathUrl = imagesPathUrl;
        this.outputPath = outputPath;
        this.properties = new Properties();
        this.goldPath = goldPath;
        try
        {
            this.properties.load(new FileInputStream(propertiesPath));
        }
        catch(IOException ioe)
        {
            System.err.println("Cannot load properties file");
            System.exit(1);
        }
        Options opts = new Options();
        opts.useGoldStandardOnly = true;
        model = new Event3Model(opts);
        scenariosList = new ArrayList<Scenario>();
        filterFieldsList = new ArrayList<String>();
    }

    public void setBaselinePath(String baselinePath)
    {
        this.baselinePath = baselinePath;
    }

    public void setGaborPath(String gaborPath)
    {
        this.gaborPath = gaborPath;
    }

    public void setModelPath(String modelPath)
    {
        this.modelPath = modelPath;
    }

    public void setNullifyOrder(boolean nullifyOrder)
    {
        this.nullifyOrder = nullifyOrder;
    }

    public void setOutputAllFields(boolean outputAllFields)
    {
        this.outputAllFields = outputAllFields;
    }

    public void execute()
    {
        try
        {
           BufferedReader scenariosReader = new BufferedReader(new FileReader(scenariosPath));
           String[] humanLines = null;
           if(goldPath != null)
               humanLines = Utils.readLines(goldPath);
           String[] modelLines = null;
           if(modelPath != null)
               modelLines = Utils.readLines(modelPath);
           String[] baselineLines = null;
           if(baselinePath != null)
               baselineLines = Utils.readLines(baselinePath);
           HashMap<String, GaborEntry> gaborEntries = null;
           if(gaborPath != null) // parse guess, gold and guess_events in gabor file
           {
               gaborEntries = new HashMap();
               String[] lines = Utils.readLines(gaborPath);
               for(int i = 0; i < lines.length; i++)
               {
                   if(lines[i].contains("<guess>"))
                   {
                       GaborEntry ge = new GaborEntry(lines[i],
                               lines[i + 1], lines[i + 4]);
                       gaborEntries.put(ge.gold, ge);
                   }
               }
           } // if           
           for(String path : Utils.readLines(scenariosPath))
           {
               if(path.equals(""))
                   continue;
                if(processHuman(path, humanLines))
                    System.out.println(path + " human system processed succesfully");
                else
                    System.out.println(path + " human system error!");
                if(modelPath != null)
                {
                    if(processModel(path, modelLines, Type.model))
                        System.out.println(path + " model system processed succesfully");
                    else
                        System.out.println(path + " model system error!");
                }
                if(baselinePath != null)
                {
                    if(processModel(path, baselineLines, Type.baseline))
                        System.out.println(path + " baseline system processed succesfully");
                    else
                        System.out.println(path + " baseline system error!");
                }
                if(gaborPath != null)
                {
                    if(processGabor(gaborEntries))
                        System.out.println(path + " gabor system processed succesfully");
                    else
                        System.out.println(path + " gabor system error!");
                }
           }
           scenariosReader.close();
           // output in webExp2 format
           if(outputPath != null)
           {
               FileOutputStream fos = new FileOutputStream(outputPath);
               // read list of unwanted fields
               String[] ar = properties.getProperty("filter.fields.list").split(",");
               filterFieldsList.addAll(Arrays.asList(ar));
//               writeOutputWebExp(fos);
               writeOutputHumanEval(fos);
               fos.close();
           }
           
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    /**
     * Process the gold standard text, events and alignments. Read all events
     * and a new Scenario instance to the <code>scenariosList</code>
     * @param basename the stripped basename path of the scenario to be processed
     * @param lines  the lines of the file that contains the gold input
     * @return true if all corresponding files exist 
     */
    private boolean processHuman(String basename, String[] lines)
    {
        String[] eventInput = null, textInput = null, alignInput = null;
        // sanity check: all corresponding files exist (if in seperate files)
        if(lines == null) // examples in seperate files
        {
            boolean sanity = new File(basename + ".events").exists() &&
                             new File(basename + ".align").exists() &&
                             new File(basename + ".text").exists();
            if(!sanity)
                return false;
            eventInput = Utils.readLines(basename + ".events");
            alignInput = Utils.readLines(basename + ".align");
            textInput  = Utils.readLines(basename + ".text");
        } // if
        else // examples in single file
        {
            String[] res = Event3Model.extractExampleFromString(fetchExampleFromGold(basename, lines));
            textInput = res[1].split("\n");
            eventInput = res[2].split("\n");
            alignInput = res[3].split("\n");
        }
        // read all the events from the .events file
        Scenario scn = new Scenario(basename,
                                    model.readEvents(eventInput,
                                    new HashSet(), new HashSet()));
        // read the gold-standard human events
        for(String eventLine : alignInput)
        {
            // each line holds line number and aligned event(s)
            String []lineEvents = eventLine.split(" ");
            for(int i = 1; i < lineEvents.length; i++)
            {
                // soft alignment: add duplicate events only once (adding to a set)
                scn.getEventIndices(HUMAN).add(Integer.valueOf(lineEvents[i]));
            }
        }
        // read the gold-standard text
        String text = "";
        for(String line : textInput)
            text += line + " ";
        scn.setText(HUMAN, text.trim().toUpperCase());
        return scenariosList.add(scn);
    }

    /**
     * Find the excerpt in the gold standard file (not the best solution for many searches!)
     * @param basename the name-id of the example to look at
     * @param lines the input lines
     * @return the excerpt
     */
    private String fetchExampleFromGold(String basename, String[] lines)
    {
        String key = null;
        StringBuilder str = new StringBuilder();
        for(String line : lines)
        {
            if(line.startsWith("Example_"))
            {
                if(key != null) // only for the first example
                {
                    if(key.equals(basename))
                        return str.toString();
                    str = new StringBuilder();
                }
                key = line;
            } // if
            str.append(line).append("\n");
        }  // for
        if(key.equals(basename)) // don't forget last example
            return str.toString();
        return null;
    }

    /**
     * Process the standard model/baseline output using the basename path as the key.
     * @param keyBasename the basename path
     * @param lines  the lines of the file of the model output
     * @return true if the entry was found in the file
     */
    private boolean processModel(String keyBasename, String[] lines, Type modelType)
    {
        for(int i = 0; i < lines.length; i++)
        {
            // try to find the corresponding entry in the model's output file
            // using the scenarios basename as a key
            if(lines[i].equals(keyBasename + ".text") || lines[i].equals(keyBasename))
            {                
                // get current scenario
                Scenario scn = scenariosList.get(scenariosList.size() - 1);
                // next line is the text
                if(modelType == Type.model)
                {
                    scn.setText(MODEL, lines[i + 1].trim().toUpperCase());
                    // after two lines we have the generated events
                    scn.getEventIndices(MODEL).addAll(processEventsLine(lines[i + 3],
                                                  scn.getEventTypeNames(),
                                                  scn, false));
                }
                else if(modelType == Type.baseline)
                {
                    scn.setText(BASELINE, lines[i + 1].trim().toUpperCase());
                    // after two lines we have the generated events
                    scn.getEventIndices(BASELINE).addAll(processEventsLine(lines[i + 3],
                                                  scn.getEventTypeNames(),
                                                  scn, false));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * capture events id from the model/baseline output. The input contains event
     * instances of the type eventType(eventId).
     * @param line the input line
     * @param eventTypeNames a list of the names of the eventTypes found in the scenario
     * @param ignoreTrueId if true then don't use ids in the line, but rather
     * match the event's name to the id captured earlier in the Scenario
     * @return a set of events id
     */
    private Collection<Integer> processEventsLine(String line, Set<String> eventTypeNames,
            Scenario scn, boolean ignoreTrueId)
    {
        Set<Integer> ids = new TreeSet();
        // we are interested only in the eventId to cross match with the
        // events array already captured. Event instances are of the type
        // eventType(eventId), so we split on the eventId parentheses
        String[] tokens = line.split("[\\(\\) ]");
        for(int i = 0; i < tokens.length; i++)
        {
            // double check we already know the eventType
            if(eventTypeNames.contains(tokens[i]))
            {                
                ids.add(ignoreTrueId ? scn.getIdOfEvent(tokens[i]) :
                                       Integer.valueOf(tokens[i + 1]));
            }
        }
        return ids;
    }

    /**
     * Process the output of Gabor's system using the text of the scenario as the key.
     * @param gaborEntries a map containing entries from Gabor's input. The key is
     * the gold-standard text.
     * @return true if the entry was found in the file
     */
    private boolean processGabor(HashMap<String, GaborEntry> gaborEntries)
    {
        // get current scenario
        Scenario scn = scenariosList.get(scenariosList.size() - 1);
        GaborEntry ge = gaborEntries.get(scn.getText(HUMAN));
        if(ge == null)
            return false;
        scn.setText(GABOR, ge.guess);
        scn.getEventIndices(GABOR).addAll(processEventsLine(ge.guessEvents,
            scn.getEventTypeNames(), scn, nullifyOrder));
        return true;
    }

    private void writeOutputWebExp(FileOutputStream fos) throws IOException
    {
        // write header
        writeLine(fos, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<resources>");

        // write each scenario-system combination on one block.
        // The permutation order is given from the properties file
        String[] permutationOrder = properties.getProperty("permutationOrder").split(",");
        int perm = 0;
        for(Scenario scn : scenariosList)
        {
            // we cycle through all the systems in the permutationOrder array
            // and pick the corresponding scenario. We should output
            // scneariosList.size() in the end
            String system = permutationOrder[(perm++) % permutationOrder.length];
            // write block header
            writeLine(fos, String.format("<block id=\"%s_%s\">", scn.getPath(), system));
            // write resource header
            writeLine(fos, String.format("<resource id=\"%s_%s\">", scn.getPath(), system));
            // write resource body
            String events = "<html><head>"
                    + "<style type=\"text/css\">"+ properties.getProperty("style")+"</style></head>\n"
                    + "<center>\n<h2>Categories</h2><table>\n";
            events += "<tr><th colspan=\"1\">Category"
                    + "</th><th colspan=\"2\">Fields - Values</th></tr>\n";
            for(Integer id : scn.getEventIndices(system))
            {
                events += eventToHtml(scn.getEvents().get(id));
            }
            events += "</table>\n";            
            // write text
            String text = "<h2>Translation</h2>\n" +
                          "<table id=\"text\"><tr><td>" +
                          scn.getText(system) +
                          "</td></tr></table></center>\n</html>";
// fix-weatherX.sh script only
//            String id = String.format("<id>%s_%s</id>", scn.getPath(), system);
//            System.out.println(outputReplaceString(
//                    htmlEncode(text) + "<\\/content>",
//                    htmlEncode(text) + "<\\/content>\\n\\n"+htmlEncode(id)
//                    ));
            writeLine(fos, events+text);
//            writeLine(fos, htmlEncode(events+text));
            // write resource footer
            writeLine(fos, "</resource>");
            // write block footer
            writeLine(fos, "</block>");
            
        }
        // write footer
        writeLine(fos, "\n\n</resources>");
    }

    private void writeOutputHumanEval(FileOutputStream fos) throws IOException
    {
        // write header
        writeLine(fos, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<resources>");

        // write each scenario-system combination on one block.
        // The permutation order is given from the properties file
        String[] permutationOrder = properties.getProperty("permutationOrder").split(",");
        int perm = 0;
        for(Scenario scn : scenariosList)
        {
            // we cycle through all the systems in the permutationOrder array
            // and pick the corresponding scenario. We should output
            // write block header
            writeLine(fos, String.format("<block id=\"%s\">", (perm+1)));
            // scneariosList.size() in the end
            writeLine(fos, "<![CDATA["); // escape html embedded in xml
            String system = permutationOrder[(perm++) % permutationOrder.length];            
            // write div header
            writeLine(fos, String.format("<div class=\"resource\" id=\"%s_%s\">", scn.getPath(), system));
            // write resource body
            String events = "\n<h2>Categories</h2><table>\n"
                          + "<tr><th class=\"events\" colspan=\"2\">Category</th>"
                          + "<th class=\"events\" colspan=\"5\">Fields - Values</th></tr>\n"; // make colspan sth big to make sure it spans all cols
            for(Integer id : scn.getEventIndices(system))
            {
                events += eventToHtml(scn.getEvents().get(id));
            }
            events += "</table>\n";
            // write text
            String text = "<h2>Translation</h2>\n" +
                          "<table class=\"text\" id=\"text\"><tr><td>" +
                          scn.getText(system) +
                          "</td></tr></table>";
            writeLine(fos, events+text);
//            writeLine(fos, htmlEncode(events+text));
            // write div footer
            writeLine(fos, "</div>");
            writeLine(fos, "]]>"); // close escaping escape html embedded in xml
            // write block footer
            writeLine(fos, "</block>");            
        } // for
        // write footer
        writeLine(fos, "\n\n</resources>");
    }

    private void writeLine(FileOutputStream fos, String str) throws IOException
    {
        fos.write((str + "\n").getBytes());
    }

    private String eventToHtml(Event event)
    {
        // write formatted eventType
        String image = "";
        String out = "<tr>" +
                    // image if it exists
                    ((image =
                    properties.getProperty("eventType."+event.getEventTypeName()+".icon")) == null
                    ? "" : 
                    String.format("<td><img src=\"%s%s\" width=\"45\" height=\"45\" /></td>",imagesPathUrl, image)) +
                    // formatted eventType
                    "<td id=\"eventType\">"+properties.getProperty("eventType."+event.getEventTypeName()+".name") +
                    "</td>\n";
        // write formatted field name and value
        Field[] fields = event.getFields();
        String fieldName, formattedName, value, formattedValue;
        for(int i = 0; i < fields.length; i++)
        {
            fieldName = event.fieldToString(i);
            value = fields[i].valueToString(event.getValues().get(i));
            // don't output fields in the filtered fields' list or fields that are empty and outputAllFields=false
            if(filterFieldsList.contains(fieldName) || (!outputAllFields && value.equals("--")))
                continue;
            // check whether the fieldName needs formatting
            out += ((i == fields.length-1) ? "<td colspan=\"1\">" : "<td>") + // make sure we span the whole line
                    "<b>" +
                    ((formattedName =
                    properties.getProperty("field."+fieldName)) == null
                    ? fieldName : formattedName) +
                    ":</b> ";
            
            // check whether the value needs formatting
            out += ((formattedValue =
                    properties.getProperty("value."+fieldName+"."+value)) == null
                    ? value : formattedValue) +
                   "</td>\n";
        }
        return out + "</tr>\n";
    }

    private String htmlEncode(String str)
    {
        return str.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        // fix-weatherX.sh script only
//        return str.replaceAll("\n", "").replaceAll("/", "\\\\/").replaceAll("\"", "\\\\\"");
                
    }

    private String outputReplaceString(String from, String to)
    {
        return String.format("perl -pi -w -e 's/%s/%s/g;' *.xml",from, to);
    }

    public String exportTextFromSystems()
    {
        StringBuilder str = new StringBuilder();
        for(Scenario scn: scenariosList)
        {
            str.append(scn.getPath()).append("\n");
            str.append(HUMAN).append(": ").append(scn.getText(HUMAN)).append("\n");
            str.append(MODEL).append(": ").append(scn.getText(MODEL)).append("\n");
            str.append(GABOR).append(": ").append(scn.getText(GABOR)).append("\n");
            str.append(BASELINE).append(": ").append(scn.getText(BASELINE)).append("\n");
        }
        return str.toString();
    }

    public static void main(String[] args)
    {
        String outputPath = null;
        String imagesPathUrl = null;
        String goldPath = null;
        boolean nullifyOrder = false, outputAllFields = true;
        // we assume that the input path has a scenario on each seperate line
        // and does not have a file extension

        // weatherGov
//        String scenariosPath = "gaborLists/weatherEvalScenariosRandomBest12";
////        String imagesPathUrl = "file:///home/konstas/EDI/webexp2/statgen/resources/icons/";
//        String imagesPathUrl = "http://fordyce.inf.ed.ac.uk/users/s0793019/statgen/resources/icons/";
////        outputPath = "../webexp2/mturk/weatherGov/data/statgen/bgm-weather1/weatherResources1";
//        outputPath = "../webexp2/data/statgen/erase.txt";
//        String propertiesPath = "../webexp2/statgen/resources/weatherGov.properties";
//        String modelPath = "results/output/generation/abs/all/gabor/"
//                           + "15-best_reordered_eventTypes_user_eval_only/2.exec" +
//                           "/stage1.test.full-pred-gen";
//        String gaborPath = "../gaborFiles/2010emnlp-generation/results-weather.xml";
//        String baselinePath = "results/output/generation/abs/all/gabor/"
//                           + "1-best_reordered_eventTypes_user_eval_only/2.exec" +
//                           "/stage1.test.full-pred-gen";
        // roboCup
//        String scenariosPath = "robocupLists/robocupEvalScenariosRandomBest12";
////        String imagesPathUrl = "file:///home/konstas/EDI/webexp2/statgen/resources/icons/";
////        String imagesPathUrl = "http://fordyce.inf.ed.ac.uk/users/s0793019/statgen/resources/icons/";
//        outputPath = "../webexp2/data/statgen/bgm-robocup4/robocupResources4";
//        String propertiesPath = "../webexp2/statgen/resources/robocup.properties";
//        String modelPath = "results/output/robocup/generation/all/fold4/" +
//                           "NO_NULL_LM2_gold_perfect.exec/stage1.test.full-pred-gen";
//        String gaborPath = "../gaborFiles/2010emnlp-generation/results-robocup2004.xml";
//        String baselinePath = "results/output/robocup/generation/all/fold4/" +
//                           "NO_NULL_LM2_gold_perfect_baseline.exec/stage1.test.full-pred-gen";

        // atis
        String scenariosPath = "data/atis/test/atisEvalScenariosRandomBest12";
        imagesPathUrl = "resources/icons/";
        outputPath = "../../Public/humaneval/data/atisExperiment4";
        String propertiesPath = "../../Public/humaneval/data/atis.properties";
        String modelPath = "results/output/atis/generation/" +
                           "model_3_40-best_no_null_no_smooth_STOP_predLength/stage1.test.full-pred-gen";
        String gaborPath = "../Gabor/generation/outs/atis/1.exec/results-test.xml.tree";
        String baselinePath = "results/output/atis/generation/" +
                           "model_3_1-best_no_null_no_smooth_STOP_predLength/stage1.test.full-pred-gen";
        goldPath = "data/atis/test/atis-test.txt";
        nullifyOrder = true; // in the case of Gabor's system, all events are identified
                                     // by their position in the file, rather than their id.
                                     // As such, these ids may not correspond to the real event ids,
                                     // assert in the case of ATIS. Therefore, we ignore this order
                                     // and instead match the true event's id based on the eventType's
                                     // name. WARNING: this will not work correctly in the case of
                                     // examples with more than one events of the same event type.
        outputAllFields = false; // don't output fields that are emtpy, i.e. have value='--'
        if(args.length > 1)
        {
            scenariosPath = args[0];
            imagesPathUrl = args[1];
        }
        ExportScenariosToWebExp2 estw = new ExportScenariosToWebExp2(scenariosPath, 
                imagesPathUrl, outputPath, propertiesPath, goldPath);
        estw.setModelPath(modelPath);
        estw.setGaborPath(gaborPath);
        estw.setBaselinePath(baselinePath);
        estw.setNullifyOrder(nullifyOrder);
        estw.setOutputAllFields(outputAllFields);
        estw.execute();
//        System.out.println(estw.exportTextFromSystems());
    }

    class GaborEntry
    {
        String guess, gold, guessEvents;

        public GaborEntry(String guess, String gold, String guessEvents)
        {
            this.guess = stripLine(guess, "guess").toUpperCase();
            this.gold = stripLine(gold, "gold").toUpperCase();
            this.guessEvents = stripLine(guessEvents, "guess_events");
        }

        private String stripLine(String line, String token)
        {
            return line.replace("<"+token+">", "").replace("</"+token+">", "").trim();
        }
    }
}
