package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import fr.flowarg.flowupdater.versions.VanillaVersion;

import java.util.function.Supplier;

/**
 * Builder API
 * 
 * @author flow
 * @version 1.6
 * 
 * Used for {@link FlowUpdater} and {@link VanillaVersion} and {@link AbstractForgeVersion}
 * @param <T> Object Argument
 * 
 * Represent an argument for a Builder implementation.
 */
public class BuilderArgument<T>
{
    private final String objectName;
    private T badObject = null;
    private T object = null;
    private boolean isRequired;

    public BuilderArgument(String objectName, Supplier<T> initialValue)
    {
        this.objectName = objectName;
        this.object = initialValue.get();
    }
    
    public BuilderArgument(String objectName)
    {
        this.objectName = objectName;
    }
    
    public BuilderArgument(String objectName, Supplier<T> initialValue, Supplier<T> badObject)
    {
        this.objectName = objectName;
        this.object = initialValue.get();
        this.badObject = badObject.get();
    }
    
    public BuilderArgument(Supplier<T> badObject, String objectName)
    {
        this.objectName = objectName;
        this.badObject = badObject.get();
    }
    
    public T get() throws BuilderException
    {
        if(this.object == this.badObject && this.badObject != null)
            throw new BuilderException("Argument" + this.objectName + " is a bad object!");

        if(this.isRequired)
        {
            if(this.object == null)
                throw new BuilderException("Argument" + this.objectName + " is null!");
            else return this.object;
        }
        else return this.object;
    }
    
    public void set(T object)
    {
        this.object = object;
    }
    
    public BuilderArgument<T> require(BuilderArgument<?>... required)
    {
        for (BuilderArgument<?> arg : required)
            arg.isRequired = true;
        return this;
    }
    
    public BuilderArgument<T> required()
    {
        this.isRequired = true;
        return this;
    }
    
    public BuilderArgument<T> optional()
    {
        this.isRequired = false;
        return this;
    }
    
    public String getObjectName()
    {
        return this.objectName;
    }
    
    public T badObject()
    {
        return this.badObject;
    }

    @Override
    public String toString()
    {
        return "BuilderArgument{" + "objectName='" + this.objectName + '\'' + ", isRequired=" + this.isRequired + '}';
    }
}
