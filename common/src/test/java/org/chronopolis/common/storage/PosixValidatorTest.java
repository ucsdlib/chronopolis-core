package org.chronopolis.common.storage;

import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;

import java.io.File;

public class PosixValidatorTest {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @After
    public void close() {
        context.close();
    }

    @Test
    public void bindValid() {
        context.register(SamplePropertyApplication.class);
        File tmp = Files.temporaryFolder();

        TestPropertyValues.of(
                "storage.preservation.posix[0].id:1",
                "storage.preservation.posix[0].path:" + tmp.toString()
        ).applyTo(context);
        context.refresh();

        PreservationProperties properties = context.getBean(PreservationProperties.class);
        Assert.notEmpty(properties.getPosix(), "preservation areas exist");
    }

    @Test(expected = BeanCreationException.class)
    public void bindInvalid() {
        context.register(SamplePropertyApplication.class);
        TestPropertyValues.of(
                "storage.preservation.posix[0].id:1",
                "storage.preservation.posix[0].path:/dne"
        ).applyTo(context);
        context.refresh();
    }

    @Test(expected = BeanCreationException.class)
    public void bindNoProperties() {
        context.register(SamplePropertyApplication.class);
        TestPropertyValues.of().applyTo(context);
        context.refresh();
    }

}