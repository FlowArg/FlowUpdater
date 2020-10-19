package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import fr.flowarg.flowupdater.versions.VanillaVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Builder API
 * 
 * @author flow
 * @version 1.5
 * 
 * Used for {@link FlowUpdater} & {@link VanillaVersion} & {@link AbstractForgeVersion}
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
    private final List<BuilderArgument<?>> required = new ArrayList<>();
    
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
        if(this.isRequired)
        {
            if(this.object == null)
                throw new BuilderException("Argument" + this.objectName + " is null !");
            else if(this.object == this.badObject)
                throw new BuilderException("Argument" + this.objectName + " is a bad object !");
            else return this.object;
        }
        else
        {
            this.required.forEach(arg -> {
                try
                {
                    if((arg.get() == null || arg.get() == arg.badObject()) && this.object != null)
                        throw new BuilderException(arg.getObjectName() + " cannot be null/a bad object if you're using " + this.objectName + " argument !");
                }
                catch (BuilderException e)
                {
                    e.printStackTrace();
                }
            });
            return this.object;
        }
    }
    
    public void set(T object)
    {
        this.object = object;
    }
    
    public BuilderArgument<T> require(BuilderArgument<?>... required)
    {
        this.required.addAll(Arrays.asList(required));
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
}
