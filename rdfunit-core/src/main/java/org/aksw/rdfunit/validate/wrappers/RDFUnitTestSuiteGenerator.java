package org.aksw.rdfunit.validate.wrappers;

import org.aksw.rdfunit.RDFUnit;
import org.aksw.rdfunit.io.reader.RDFFirstSuccessReader;
import org.aksw.rdfunit.io.reader.RDFReader;
import org.aksw.rdfunit.io.reader.RDFReaderException;
import org.aksw.rdfunit.io.reader.RDFReaderFactory;
import org.aksw.rdfunit.sources.SchemaSource;
import org.aksw.rdfunit.sources.SchemaSourceFactory;
import org.aksw.rdfunit.sources.TestSource;
import org.aksw.rdfunit.sources.TestSourceFactory;
import org.aksw.rdfunit.tests.TestSuite;
import org.aksw.rdfunit.tests.generators.TestGeneratorExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Description
 *
 * @author Dimitris Kontokostas
 * @since 9/7/15 10:13 AM
 */
public final class RDFUnitTestSuiteGenerator {

    private final Collection<SchemaSource> schemas;

    private final boolean enableAutoTests;
    private final boolean enableManualTests;

    private TestSuite testSuite = null;

    private RDFUnitTestSuiteGenerator(Collection<SchemaSource> schemas, boolean enableAutoTests, boolean enableManualTests) {
        this.enableAutoTests = enableAutoTests;
        this.enableManualTests = enableManualTests;
        this.schemas = Collections.unmodifiableCollection(checkNotNull(schemas));
    }

    public TestSuite getTestSuite() {
        if (testSuite == null) {
            synchronized (RDFUnitStaticValidator.class) {
                if (testSuite == null) {

                    // Generate test cases from ontology (do this every time in case ontology changes)
                    RDFUnit rdfunit = new RDFUnit();
                    try {
                        rdfunit.init();
                    } catch (RDFReaderException e) {
                        // fatal error
                    }

                    TestSource dummyTestSource = TestSourceFactory.createDumpTestSource("dummy", "dummy", null, schemas);

                    TestGeneratorExecutor testGeneratorExecutor = new TestGeneratorExecutor(
                            enableAutoTests,
                            false,
                            enableManualTests);
                    testSuite = testGeneratorExecutor.generateTestSuite("", dummyTestSource, rdfunit.getAutoGenerators());

                }
            }
        }
        return testSuite;
    }

    public Collection<SchemaSource> getSchemas() {
        return schemas;
    }


    public static class Builder {
        private Collection<SchemaSource> schemas = new ArrayList<>();

        private boolean enableAutoTests = true;
        private boolean enableManualTests = true;

        public Builder enableAutotests() {
            enableAutoTests = true;
            return this;
        }

        public Builder disableAutotests() {
            enableAutoTests = false;
            return this;
        }
        public Builder enableManualtests() {
            enableManualTests = true;
            return this;
        }
        public Builder disableManualtests() {
            enableManualTests = false;
            return this;
        }

        public Builder addSchemaURI(String schemaUri) {
            checkNotNull(schemaUri);

            SchemaSource schemaSource = createSource(schemaUri, RDFReaderFactory.createResourceReader(schemaUri));
            schemas.add(schemaSource);

            return this;
        }

        public Builder addLocalResource(String localResource) {
            checkNotNull(localResource);

            SchemaSource schemaSource = createSource(localResource, RDFReaderFactory.createResourceReader(localResource));
            schemas.add(schemaSource);

            return this;
        }

        public Builder addLocalResourceOrSchemaURI(String localResource, String schemaUri) {
            checkNotNull(localResource);
            checkNotNull(schemaUri);

            RDFReader schemaReader = new RDFFirstSuccessReader(
                    Arrays.asList(
                            RDFReaderFactory.createResourceReader(localResource),
                            RDFReaderFactory.createDereferenceReader(schemaUri)
                    ));

            SchemaSource schemaSource = createSource(schemaUri, schemaReader);
            schemas.add(schemaSource);

            return this;
        }

        public RDFUnitTestSuiteGenerator build() {
            return new RDFUnitTestSuiteGenerator(schemas, enableAutoTests, enableManualTests);
        }

        private SchemaSource createSource(String schemaUri, RDFReader schemaReader) {
            return SchemaSourceFactory.createSchemaSourceSimple(schemaUri, schemaReader);
        }


    }

}