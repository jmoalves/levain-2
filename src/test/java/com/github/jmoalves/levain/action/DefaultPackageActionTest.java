package com.github.jmoalves.levain.action;

import com.github.jmoalves.levain.config.Config;
import com.github.jmoalves.levain.model.Recipe;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPackageActionTest {

    @Test
    void shouldSetDefaultPackageAndSave() {
        TestConfig config = new TestConfig();
        ActionContext context = new ActionContext(config, new Recipe(), Path.of("/tmp"), Path.of("/tmp"));

        DefaultPackageAction action = new DefaultPackageAction();
        action.execute(context, List.of("toolchain"));

        assertEquals("toolchain", config.getDefaultPackage());
        assertTrue(config.wasSaved);
    }

    @Test
    void shouldRejectMissingPackageName() {
        DefaultPackageAction action = new DefaultPackageAction();
        ActionContext context = new ActionContext(new TestConfig(), new Recipe(), Path.of("/tmp"), Path.of("/tmp"));

        assertThrows(IllegalArgumentException.class, () -> action.execute(context, List.of()));
    }

    private static class TestConfig extends Config {
        private boolean wasSaved;

        @Override
        public void save() {
            wasSaved = true;
        }
    }
}
