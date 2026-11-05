package consumer;

import com.alibaba.smart.framework.engine.SmartEngine;
import com.alibaba.smart.framework.engine.configuration.impl.DefaultProcessEngineConfiguration;
import com.alibaba.smart.framework.engine.configuration.impl.DefaultSmartEngine;
import com.alibaba.smart.framework.engine.model.instance.InstanceStatus;
import com.alibaba.smart.framework.engine.persister.custom.session.PersisterSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StandaloneEngineTest {
    @Test
    void installedJarsCanDeployAndCompleteProcess() throws Exception {
        // Verify that this consumer loads the installed JAR, not reactor target/classes.
        assertTrue(SmartEngine.class.getProtectionDomain().getCodeSource()
                .getLocation().toString().endsWith(".jar"));
        var engine = new DefaultSmartEngine();
        PersisterSession.create();
        try {
            engine.init(new DefaultProcessEngineConfiguration());
            var definition = engine.getRepositoryCommandService()
                    .deploy("smoke.bpmn20.xml").getFirstProcessDefinition();
            var instance = engine.getProcessCommandService()
                    .start(definition.getId(), definition.getVersion());
            assertNotNull(instance.getInstanceId());
            assertEquals(InstanceStatus.completed, instance.getStatus());
            assertNotNull(instance.getCompleteTime());
        } finally {
            engine.destroy();
            PersisterSession.destroySession();
        }
    }
}
