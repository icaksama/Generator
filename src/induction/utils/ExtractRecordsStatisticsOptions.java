package induction.utils;

import fig.basic.Option;
import fig.basic.OptionSet;
import induction.Options;

/**
 *
 * @author sinantie
 */
public class ExtractRecordsStatisticsOptions
{
    public enum Type {record, recordType};
    public enum Direction {left, right};
    public enum TreeType {unlabelled, rst};
    
    @OptionSet(name="modelOpts") public Options modelOpts = new Options();
    
    @Option(required=true) public Type exportType = Type.recordType;    
    @Option(gloss="Split at every punctuation or just at '.'") public boolean splitClauses = false;
    @Option(gloss="Export event type names instead of ids") public boolean useEventTypeNames = false;
    @Option(gloss="Export (none) event type") public boolean extractNoneEvent = false;
    @Option(gloss="Write record (type) assignments as a flat file") public boolean writePermutations = false;
    @Option(gloss="Count the number of times each record (type) gets repeated across clauses") public boolean countRepeatedRecords = false;    
    @Option(gloss="Count the number of record (type) ngrams per sentence") public boolean countSentenceNgrams = false;    
    @Option(gloss="Count the number of record (type) sentence ngrams per document") public boolean countDocumentNgrams = false;    
    @Option(gloss="Output a delimiter '|' between sentences") public boolean delimitSentences = false;
    @Option(gloss="Write record (type) assignments as an mrg tree") public boolean extractRecordTrees  = false;
    @Option(gloss="Left binarize or right binarize") public Direction binarize = Direction.left;
    @Option(gloss="Markovization order") public int markovOrder = -1;
    @Option(gloss="Use modified binarization. Intermediate labels are generated from children labels.") public boolean modifiedBinarization = false;
    @Option(gloss="Export in event3 v.2 format.") public boolean exportEvent3 = false;
    @Option(gloss="Predicted input file") public String predInput;
    @Option(gloss="External input file containing trees (e.g. output of CCM)") public String externalTreesInput;
    @Option(gloss="External input tree type (e.g. CCM)") public TreeType externalTreesInputType = TreeType.unlabelled;
    @Option(gloss="Remove records with one words") public boolean removeRecordsWithOneWord  = false;
    @Option(gloss="Rule count threshold") public int ruleCountThreshold  = Integer.MIN_VALUE;
    @Option(gloss="Prefix of output filenames") public String prefix = "";
    @Option(gloss="Suffix of output filenames") public String suffix = "";
    @Option(gloss="Override cleaning heuristics on alignment input (useful for RST)") public boolean overrideCleaningHeuristics = false;
    @Option(gloss="Parent annotation of non-terminals with their parent label, as in Johnson (1998) (implemented for RST only)") public boolean parentAnnotation = false;
    @Option(gloss="Count non-terminals") public boolean countNonTerminals = false;
}