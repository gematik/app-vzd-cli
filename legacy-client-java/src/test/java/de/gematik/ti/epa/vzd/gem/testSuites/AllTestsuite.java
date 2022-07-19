package de.gematik.ti.epa.vzd.gem.testSuites;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({IntegrationTestsuite.class, UnitTestsuite.class})
public class AllTestsuite {

}
