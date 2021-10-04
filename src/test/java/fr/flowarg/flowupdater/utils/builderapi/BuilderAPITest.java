package fr.flowarg.flowupdater.utils.builderapi;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuilderAPITest
{
    @Test
    public void shouldFailBecauseMissingRequiredArgument()
    {
        assertThrows(BuilderException.class, () -> new TestBuilder().build());
    }

    @Test
    public void shouldWorkBecauseRequiredArgumentIsFilled()
    {
        final TestObject object = new TestBuilder().withAnArgument("AnArgument").build();
        assertEquals("AnArgument", object.str);
    }

    @Test
    public void shouldFailBecauseOfBadObject()
    {
        assertThrows(BuilderException.class, () -> new TestBuilder().withAnArgument("AnArgument").withAnInt(-1).build());
    }

    @Test
    public void shouldFailBecauseUndefinedParentArgument()
    {
        assertThrows(BuilderException.class, () -> new AnotherTestBuilder().withAnotherBoolean(true).build());
    }

    @Test
    public void shouldWorkBecauseDefinedParentArgument()
    {
        final AnotherTestObject anotherTestObject = new AnotherTestBuilder().withAnotherBoolean(true).withABoolean(false).build();
        assertTrue(anotherTestObject.anotherBoolean);
        assertFalse(anotherTestObject.aBoolean);
    }

    private static class TestObject
    {
        public final String str;
        public final int anInt;

        public TestObject(String str, int anInt)
        {
            this.str = str;
            this.anInt = anInt;
        }
    }

    private static class AnotherTestObject
    {
        public final boolean aBoolean;
        public final boolean anotherBoolean;

        public AnotherTestObject(boolean aBoolean, boolean anotherBoolean)
        {
            this.aBoolean = aBoolean;
            this.anotherBoolean = anotherBoolean;
        }
    }

    private static class TestBuilder implements IBuilder<TestObject>
    {
        private final BuilderArgument<String> anArgument = new BuilderArgument<String>("AnArgument").required();
        private final BuilderArgument<Integer> anIntArgument = new BuilderArgument<>("AnIntArgument", () -> 0, () -> -1).optional();

        public TestBuilder withAnArgument(String anArgument)
        {
            this.anArgument.set(anArgument);
            return this;
        }

        public TestBuilder withAnInt(int anInt)
        {
            this.anIntArgument.set(anInt);
            return this;
        }

        @Contract(" -> new")
        @Override
        public @NotNull TestObject build() throws BuilderException
        {
            return new TestObject(
                    this.anArgument.get(),
                    this.anIntArgument.get()
            );
        }
    }

    private static class AnotherTestBuilder implements IBuilder<AnotherTestObject>
    {
        private final BuilderArgument<Boolean> aBooleanArgument = new BuilderArgument<Boolean>("ABooleanArgument").optional();
        private final BuilderArgument<Boolean> anotherBooleanArgument = new BuilderArgument<Boolean>("AnotherBooleanArgument").require(this.aBooleanArgument).optional();

        public AnotherTestBuilder withABoolean(boolean aBoolean)
        {
            this.aBooleanArgument.set(aBoolean);
            return this;
        }

        public AnotherTestBuilder withAnotherBoolean(boolean anotherBoolean)
        {
            this.anotherBooleanArgument.set(anotherBoolean);
            return this;
        }

        @Contract(" -> new")
        @Override
        public @NotNull AnotherTestObject build() throws BuilderException
        {
            return new AnotherTestObject(
                    this.aBooleanArgument.get(),
                    this.anotherBooleanArgument.get()
            );
        }
    }
}
