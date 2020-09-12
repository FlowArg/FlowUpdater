package fr.flowarg.flowupdater.utils;

public interface IBuilder<T> 
{
	T build() throws BuilderArgumentException;
}
