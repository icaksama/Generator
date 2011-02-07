package induction.problem.event3;

import induction.problem.event3.params.Parameters;
import induction.problem.AParams;
import induction.problem.ProbVec;

/**
 *
 * @author konstas
 */
public class EventTypeParams extends AParams
{
    private Event3Model model;
    private EventType eventType;
    private final int W, F, FS, F2, onlyabsent_efs;
    protected final int dontcare_efs;
    private final int EFS_ABSENT = 1; // 01
    private final int EFS_PRESENT = 2; // 10
    private final int EFS_DONTCARE = 3; // 11
    private String[] fieldToString, fieldSetToString;
    private String typeToString;
    protected int[] allowed_fs;
    public int none_f, boundary_f;

    public  ProbVec[] fieldChoices, genChoices, fieldNameEmissions;
    public AParams[] fieldParams;
    public ProbVec fieldSetChoices, noneFieldEmissions, filters;

    public EventTypeParams(Event3Model model, EventType eventType)
    {
        super();
        this.model = model;
        this.eventType = eventType;
        none_f = eventType.none_f;
        boundary_f = eventType.boundary_f;
        this.W = model.W();
        this.F = eventType.F;
        this.typeToString = eventType.name;
        this.fieldToString = new String[F + 2];
        for(int i = 0; i < F + 2; i++)
        {
            fieldToString[i] = eventType.fieldToString(i);
        }
        // Field sets
        FS = 1 << F;
        this.fieldSetToString = new String[FS];
        for(int i = 0; i < FS; i++)
        {
            fieldSetToString[i] = Constants.setstr(F, i);
        }
        // fs is a binary mask of what fields can be present (1) or not (0)
        // efs is a binary mask of whether each field's presence and
        // absence is allowed (1) or not (0)
        F2 = F + F; // Maximum number of elements in the efs set

        int efs = 0;
        for(int f = 0; f < F; f++)
        {
            efs = efs_addAbsent(efs, f);
        }
        onlyabsent_efs = efs;
        allowed_fs = new int[FS];
        for(int fs = 0; fs < FS; fs++)
        {
            int size = Constants.setSize(fs);
            if(model.getOpts().minFieldSetSize <= size &&
               size <= model.getOpts().maxFieldSetSize)
            {
                allowed_fs[fs] = fs;
            }
        }
        dontcare_efs = (1 << (F2)) - 1; // All 11s

         // f0, f -> choose field f given previous field f_0 (in event type t)
        fieldChoices = ProbVec.zeros2(F+2, F+2);
        addVec(fieldChoices);
        // Distribution over field sets
        fieldSetChoices = ProbVec.zeros(FS);
        addVec(fieldSetChoices);
        // w -> directly use word w (for none_f)
        noneFieldEmissions = ProbVec.zeros(W);
        addVec(noneFieldEmissions);
        // f, g -> how to generate (g) a word in event f
        genChoices = ProbVec.zeros2(F, Parameters.G);
        addVec(genChoices);
        // f, w -> express field f with word w (G_FIELD_NAME)
        fieldNameEmissions = ProbVec.zeros2(F, W);
        addVec(fieldNameEmissions);
        fieldParams = new AParams[F];
        for(int f = 0; f < F; f++)
        {
            fieldParams[f] = eventType.fields[f].newParams(typeToString + " " + fieldToString[f]);
            addVec(fieldParams[f].getVecs());
        }
        // whether this type should be generated or not
        filters = ProbVec.zeros(Parameters.B);
        addVec(filters);
    }

    public String efsstr(int efs)
    {
        return Constants.setstr(F2, efs);
    }
    public boolean efs_canBeAbesent(int efs, int f)
    {
        return Constants.setContains(efs, f+f);
    }
    public boolean efs_canBePresent(int efs, int f)
    {
        return Constants.setContains(efs, f+f+1);
    }
    public boolean efs_canBeEmpty(int efs)
    {
        return Constants.setContainsSet(efs, onlyabsent_efs);
    }
    public int efs_addAbsent(int efs, int f)
    {
        return Constants.setAdd(efs, f+f);
    }
    public int efs_addPresent(int efs, int f)
    {
        return Constants.setAdd(efs, f+f+1);
    }
    public int fs2efs(int fs)
    {
        int efs = 0;
        for(int i = 0; i < F; i++)
        {
            if(Constants.setContains(fs, i))
            {
                efs = efs_addAbsent(efs, i);
            }
            else
            {
                efs = efs_addAbsent(efs, i);
            }
        } // for
        return efs;
    }

    @Override
    public String output()
    {
        String out = "";
        String[][] labels = getLabels(F+2, F+2, "fieldC " + typeToString + " ",
                          fieldToString, fieldToString);
        int i = 0;
        for(ProbVec v : fieldChoices)
        {
            out += forEachProb(v, labels[i++]);
        }
        out += forEachProb(fieldSetChoices,
               getLabels(FS, "fieldSetC " + typeToString + " ", fieldSetToString)) +
               forEachProb(noneFieldEmissions,
               getLabels(W, "noneFieldE " + typeToString + " ",
                          model.wordsToStringArray()));
        String[][] labelsGen = getLabels(F, Parameters.G, "genC " + typeToString + " ",
                          fieldToString, Parameters.generateToString);
        String[][] labelsEm = getLabels(F, W, "fieldNameE " + typeToString + " " ,
                          fieldToString, model.wordsToStringArray());
        for(int f = 0; f < F; f++)
        {
            out += forEachProb(genChoices[f], labelsGen[f]) +
                   forEachProb(fieldNameEmissions[f], labelsEm[f]) +
                   fieldParams[f].output() + "\n";
        }
        out += forEachProb(filters,
               getLabels(Parameters.B, "filter " + typeToString + " ",
                          Parameters.booleanToString));
        return out;
    }   
}