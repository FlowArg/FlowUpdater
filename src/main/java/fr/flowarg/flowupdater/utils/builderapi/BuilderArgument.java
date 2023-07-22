package fr.flowarg.flowupdater.utils.builderapi;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Builder API; Represent an argument for a Builder implementation.
 * @version 1.6
 * @author flow
 * 
 * @param <T> Object Argument
 */
public class BuilderArgument<T>
{
    private final String objectName;
    private T badObject = null;
    private T object = null;
    private boolean isRequired;

    /**
     * Construct a new BuilderArgument.
     * @param objectName The name of the object.
     * @param initialValue The initial value's wrapper.
     */
    public BuilderArgument(String objectName, @NotNull Supplier<T> initialValue)
    {
        this.objectName = objectName;
        this.object = initialValue.get();
    }

    /**
     * Construct a new basic BuilderArgument.
     * @param objectName The name of the object.
     */
    public BuilderArgument(String objectName)
    {
        this.objectName = objectName;
    }

    /**
     * Construct a new BuilderArgument.
     * @param objectName The name of the object.
     * @param initialValue The initial value's wrapper.
     * @param badObject The initial bad value's wrapper.
     */
    public BuilderArgument(String objectName, @NotNull Supplier<T> initialValue, @NotNull Supplier<T> badObject)
    {
        this.objectName = objectName;
        this.object = initialValue.get();
        this.badObject = badObject.get();
    }

    /**
     * Construct a new BuilderArgument.
     * @param badObject The initial bad value's wrapper.
     * @param objectName The name of the object.
     */
    public BuilderArgument(@NotNull Supplier<T> badObject, String objectName)
    {
        this.objectName = objectName;
        this.badObject = badObject.get();
    }

    /**
     * Check and get the wrapped object.
     * @return the wrapper object.
     * @throws BuilderException it the builder configuration is invalid.
     */
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

    /**
     * Define the new wrapped object.
     * @param object the new wrapper object to define.
     */
    public void set(T object)
    {
        this.object = object;
    }

    /**
     * Indicate that provided arguments are required if this argument is built.
     * @param required required arguments.
     * @return this.
     */
    public BuilderArgument<T> require(BuilderArgument<?> @NotNull ... required)
    {
        for (BuilderArgument<?> arg : required)
            arg.isRequired = true;
        return this;
    }

    /**
     * Indicate that argument is required.
     * @return this.
     */
    public BuilderArgument<T> required()
    {
        this.isRequired = true;
        return this;
    }

    /**
     * Indicate that argument is optional.
     * @return this.
     */
    public BuilderArgument<T> optional()
    {
        this.isRequired = false;
        return this;
    }

    /**
     * Get the name of the current object's name.
     * @return the object's name.
     */
    public String getObjectName()
    {
        return this.objectName;
    }

    /**
     * Get the bad object.
     * @return the bad object.
     */
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
