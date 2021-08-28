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
        this.required.forEach(arg -> {
            if(arg == this)
                throw new BuilderException(String.format("This (%s) is required by the same argument !", this.objectName));

            if((arg.get() == null || arg.get() == arg.badObject()) && this.object != null)
                throw new BuilderException(String.format("%s cannot be null/a bad object if you're using %s argument!", arg.getObjectName(), this.objectName));
        });

        if(this.isRequired)
        {
            if(this.object == null)
                throw new BuilderException(String.format("Argument %s is null!", this.objectName));
            else if(this.object == this.badObject)
                throw new BuilderException(String.format("Argument %s is a bad object!", this.objectName));
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
        final List<BuilderArgument<?>> toAdd = Arrays.asList(required);
        toAdd.forEach(builderArgument -> {
            if(this.required.contains(builderArgument))
                throw new BuilderException(String.format("%s argument already added as a requirement of %s!", builderArgument.getObjectName(), this.objectName));
        });
        this.required.addAll(toAdd);
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
        return "BuilderArgument{" + "objectName='" + this.objectName + '\'' + ", isRequired=" + this.isRequired + ", required=" + this.required + '}';
    }
}
