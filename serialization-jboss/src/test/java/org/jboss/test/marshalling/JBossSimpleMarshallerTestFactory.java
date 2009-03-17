package org.jboss.test.marshalling;


import org.testng.annotations.Factory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.marshalling.serial.SerialMarshallerFactory;
import org.jboss.marshalling.serialization.java.JavaSerializationMarshallerFactory;
import org.jboss.marshalling.serialization.jboss.JBossSerializationMarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import static org.jboss.test.marshalling.Pair.pair;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 *
 */
@Test
public final class JBossSimpleMarshallerTestFactory {

    @DataProvider(name = "baseProvider")
    @SuppressWarnings({ "ZeroLengthArrayAllocation" })
    public static Object[][] parameters() {

        final JBossSerializationMarshallerFactory jbossSerializationMarshallerFactory = new JBossSerializationMarshallerFactory();
        final TestMarshallerProvider jbossTestMarshallerProvider = new MarshallerFactoryTestMarshallerProvider(jbossSerializationMarshallerFactory);
        final TestUnmarshallerProvider jbossTestUnmarshallerProvider = new MarshallerFactoryTestUnmarshallerProvider(jbossSerializationMarshallerFactory);
        
        final TestMarshallerProvider jbossCompatibilityMarshallerProvider = new JBossCompatibilityMarshallerFactoryTestMarshallerProvider(jbossSerializationMarshallerFactory);
        final TestUnmarshallerProvider jbossCompatibilityUnmarshallerProvider = new JBossCompatibilityMarshallerFactoryTestUnmarshallerProvider(jbossSerializationMarshallerFactory);
        
        final TestMarshallerProvider jbosTestMarshallerProvider = new JBossObjectOutputStreamTestMarshallerProvider();
        final TestUnmarshallerProvider jbisTestUnmarshallerProvider = new JBossObjectInputStreamTestUnmarshallerProvider();
        
        @SuppressWarnings("unchecked")
        final List<Pair<TestMarshallerProvider, TestUnmarshallerProvider>> marshallerProviderPairs = Arrays.asList(

                // JBossSerialization
                pair(jbossTestMarshallerProvider, jbossTestUnmarshallerProvider),
                pair(jbossCompatibilityMarshallerProvider, jbisTestUnmarshallerProvider),
                pair(jbosTestMarshallerProvider, jbossCompatibilityUnmarshallerProvider)
        );

        final Collection<Object[]> c = new ArrayList<Object[]>();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setCreator(new SunReflectiveCreator());
        for (Pair<TestMarshallerProvider, TestUnmarshallerProvider> pair : marshallerProviderPairs) {
            // Add this combination
            c.add(new Object[] { pair.getA(), pair.getB(), configuration.clone() });
        }

        return c.toArray(new Object[c.size()][]);
    }


    @Factory(dataProvider = "baseProvider")
    public Object[] getTests(TestMarshallerProvider testMarshallerProvider, TestUnmarshallerProvider testUnmarshallerProvider, MarshallingConfiguration configuration) {
        return new Object[] { new JBossSimpleMarshallerTests(testMarshallerProvider, testUnmarshallerProvider, configuration) };
    }
}
