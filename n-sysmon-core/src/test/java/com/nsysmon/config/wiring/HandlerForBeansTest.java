package com.nsysmon.config.wiring;

import com.ajjpj.afoundation.collection.immutable.AOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandlerForBeansTest extends ConfigPropsFileTestBase {

    private static final String IF = IFixtureBean.class.getName();

    @Test
    void beanIsCreatedByNoArgsConstructorFromFullyQualifiedName() {
        props.setProperty("k", NoArgBean.class.getName());

        assertEquals("no-arg", configFile().get("k", IFixtureBean.class).describe());
    }

    @Test
    void constructorParametersAreReadFromIndexedChildKeys() {
        props.setProperty("k", BeanWithTwoParams.class.getName());
        props.setProperty("k.0", "alpha");
        props.setProperty("k.1", "5");

        assertEquals("alpha=5", configFile().get("k", IFixtureBean.class).describe());
    }

    @Test
    void classNameMayBeIndirectedThroughAChildKey() {
        props.setProperty("k", "beanRef");
        props.setProperty("k.beanRef", BeanWithOneStringParam.class.getName());
        props.setProperty("k.beanRef.0", "indirect");

        assertEquals("indirect", configFile().get("k", IFixtureBean.class).describe());
    }

    @Test
    void constructorParameterMayItselfBeAnAlias() {
        props.setProperty("k", BeanWithOneStringParam.class.getName());
        props.setProperty("k.0.alias", "theName");
        props.setProperty("theName", "via-alias");

        assertEquals("via-alias", configFile().get("k", IFixtureBean.class).describe());
    }

    @Test
    void aliasMakesTheTargetKeysValueBeUsedForTheConfiguredType() {
        props.setProperty("k", "ignored");
        props.setProperty("k.alias", "target");
        props.setProperty("target", NoArgBean.class.getName());

        assertEquals("no-arg", configFile().get("k", IFixtureBean.class).describe());
    }

    @Test
    void beanFactoryAnnotationUsesDefaultFactoryMethodName() {
        props.setProperty("k", DefaultNameFactory.class.getName());

        assertEquals("no-arg", configFile().get("k", IFixtureBean.class).describe());
    }

    @Test
    void beanFactoryAnnotationHonoursCustomFactoryMethodName() {
        props.setProperty("k", CustomFactoryMethod.class.getName());
        props.setProperty("k.0", "from-factory");

        assertEquals("from-factory", configFile().get("k", IFixtureBean.class).describe());
    }

    @Test
    void valueWithDotsIsRejectedAsNotAFullyQualifiedClassName() {
        props.setProperty("k", "some.prefix.Value");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertEquals("config error @ k for type " + IF + ": value is not a fully qualified class name: some.prefix.Value", exc.getMessage());
    }

    @Test
    void childReferenceWithoutClassNameIsRejected() {
        props.setProperty("k", "missingChild");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertEquals("config error @ k for type " + IF + ": no class name specified", exc.getMessage());
    }

    @Test
    void childReferenceWithUnknownClassNameIsRejected() {
        props.setProperty("k", "child");
        props.setProperty("k.child", "com.does.not.Exist");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertTrue(exc.getMessage().contains("is not a valid class name"), exc.getMessage());
    }

    @Test
    void classNotAssignableToTheExpectedTypeIsRejected() {
        props.setProperty("k", NotAFixtureBean.class.getName());

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertEquals("Configured type " + NotAFixtureBean.class.getName()
                + " is not assignable to expected type " + IF + ".", exc.getMessage());
    }

    @Test
    void missingConstructorForTheNumberOfParametersIsRejected() {
        props.setProperty("k", BeanWithoutMatchingCtor.class.getName());
        props.setProperty("k.0", "a");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertEquals("config error @ k for type " + IF + ": no constructor for class "
                + BeanWithoutMatchingCtor.class.getName() + " with 1 parameters", exc.getMessage());
    }

    @Test
    void factoryWithIncompatibleReturnTypeIsRejected() {
        props.setProperty("k", FactoryWithIncompatibleReturnType.class.getName());

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertEquals("incompatible return type " + NotAFixtureBean.class.getName(), exc.getMessage());
    }

    @Test
    void factoryWithoutMatchingArityIsRejected() {
        props.setProperty("k", FactoryWithMismatchedArity.class.getName());

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertEquals("config error @ k for type " + IF + ": bean factory class " + FactoryWithMismatchedArity.class.getName()
                + " has no public static factory method getInstance with 0 parameters returning " + IF, exc.getMessage());
    }

    @Test
    void nonStaticFactoryMethodIsIgnored() {
        props.setProperty("k", FactoryWithOnlyInstanceMethods.class.getName());

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertTrue(exc.getMessage().contains("has no public static factory method"), exc.getMessage());
    }

    @Test
    void malformedConstructorParameterPropagatesTheParseFailure() {
        props.setProperty("k", BeanWithTwoParams.class.getName());
        props.setProperty("k.0", "alpha");
        props.setProperty("k.1", "not-a-number");

        assertThrows(NumberFormatException.class, () -> configFile().get("k", IFixtureBean.class));
    }

    @Test
    void blankConstructorParameterIsRejected() {
        props.setProperty("k", BeanWithTwoParams.class.getName());
        props.setProperty("k.0", "alpha");
        props.setProperty("k.1", "");

        assertThrows(NumberFormatException.class, () -> configFile().get("k", IFixtureBean.class));
    }

    @Test
    void beanListIsCreatedElementWise() {
        props.setProperty("k", NoArgBean.class.getName() + ", " + DefaultNameFactory.class.getName());

        assertEquals(2, configFile().getList("k", IFixtureBean.class).size());
    }

    @Test
    void emptyBeanListYieldsEmptyList() {
        props.setProperty("k", "");

        assertEquals(0, configFile().getList("k", IFixtureBean.class).size());
    }

    @Test
    void theNumberOfConstructorParametersIsCappedAtOneThousand() {
        props.setProperty("k", NoArgBean.class.getName());
        for (int i = 0; i < 1000; i++) {
            props.setProperty("k." + i, "x");
        }

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", IFixtureBean.class));

        assertEquals("config error @ k for type " + IF + ": no constructor for class "
                + NoArgBean.class.getName() + " with 1000 parameters", exc.getMessage());
    }

    @Test
    void defaultValueIsReturnedWithoutTouchingTheProperties() {
        final IFixtureBean fallback = new NoArgBean();

        assertSame(fallback, configFile().get("absent", AOption.some(fallback), IFixtureBean.class));
    }
}
