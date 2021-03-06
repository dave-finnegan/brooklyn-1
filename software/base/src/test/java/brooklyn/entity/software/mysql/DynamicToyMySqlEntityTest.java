package brooklyn.entity.software.mysql;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import brooklyn.entity.Entity;
import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;


public class DynamicToyMySqlEntityTest extends AbstractToyMySqlEntityTest {

    private static final Logger log = LoggerFactory.getLogger(DynamicToyMySqlEntityTest.class);
    
    protected Entity createMysql() {
        Entity mysql = app.createAndManageChild(DynamicToyMySqlEntityBuilder.spec());
        log.debug("created "+mysql);
        return mysql;
    }

    // put right group on test (also help Eclipse IDE pick it up)
    @Override
    @Test(groups = "Integration")
    public void testMySqlOnProvisioningLocation() throws NoMachinesAvailableException {
        super.testMySqlOnProvisioningLocation();
    }
    
    @Test(groups="Integration")
    public void testMySqlOnMachineLocation() throws NoMachinesAvailableException {
        Entity mysql = createMysql();
        SshMachineLocation lh = targetLocation.obtain(MutableMap.of());
        app.start(Arrays.asList(lh));
        checkStartsRunning(mysql);
        checkIsRunningAndStops(mysql, lh);
    }

}
