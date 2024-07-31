package fr.flowarg.flowupdater.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Version implements Comparable<Version>
{
    private final List<Integer> version;

    public Version(List<Integer> version)
    {
        this.version = version;
    }

    @Contract("_ -> new")
    public static @NotNull Version gen(@NotNull String version)
    {
        if(version.isEmpty())
            throw new IllegalArgumentException("Version cannot be empty.");
        final String[] parts = version.split("\\.");
        final List<Integer> versionList = new ArrayList<>();
        for (String part : parts)
            versionList.add(Integer.parseInt(part));
        return new Version(versionList);
    }

    @Override
    public int compareTo(@NotNull Version o)
    {
        final int thisSize = this.version.size();
        final int oSize = o.version.size();

        for (int i = 0; i < Math.min(thisSize, oSize); i++)
            if (!Objects.equals(this.version.get(i), o.version.get(i)))
                return Integer.compare(this.version.get(i), o.version.get(i));

        return Integer.compare(thisSize, oSize);
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.version.size(); i++)
        {
            builder.append(this.version.get(i));
            if (i < this.version.size() - 1)
                builder.append(".");
        }
        return builder.toString();
    }

    public boolean isNewerThan(@NotNull Version o)
    {
        return this.compareTo(o) > 0;
    }

    public boolean isNewerOrEqualTo(@NotNull Version o)
    {
        return this.compareTo(o) >= 0;
    }

    public boolean isOlderThan(@NotNull Version o)
    {
        return this.compareTo(o) < 0;
    }

    public boolean isOlderOrEqualTo(@NotNull Version o)
    {
        return this.compareTo(o) <= 0;
    }

    public boolean isEqualTo(@NotNull Version o)
    {
        return this.compareTo(o) == 0;
    }

    public boolean isBetweenOrEqual(@NotNull Version min, @NotNull Version max)
    {
        return this.isNewerOrEqualTo(min) && this.isOlderOrEqualTo(max);
    }
}
