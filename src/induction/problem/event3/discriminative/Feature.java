package induction.problem.event3.discriminative;

import induction.problem.Vec;

/**
 *
 * @author konstas
 */
public class Feature
{
    private Vec vec;
    private int index;

    public Feature(Vec probVec, int index)
    {
        this.vec = probVec;
        this.index = index;
    }
    
    public double getValue()
    {
        return vec.getCount(index);
    }
    
    public void setValue(double value)
    {
        vec.set(index, value);
    }
    
    public void increment(double value)
    {
        setValue(getValue() + value);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Feature other = (Feature) obj;
        if (this.vec != other.vec && (this.vec == null || !this.vec.equals(other.vec)))
        {
            return false;
        }
        if (this.index != other.index)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 83 * hash + (this.vec != null ? this.vec.hashCode() : 0);
        hash = 83 * hash + this.index;
        return hash;
    }
    
    
}
