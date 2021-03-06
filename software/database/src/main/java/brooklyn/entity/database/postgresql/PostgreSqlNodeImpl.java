package brooklyn.entity.database.postgresql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.effector.EffectorBody;
import brooklyn.event.feed.ssh.SshFeed;
import brooklyn.event.feed.ssh.SshPollConfig;
import brooklyn.location.basic.Locations;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.config.ConfigBag;
import brooklyn.util.guava.Maybe;
import brooklyn.util.time.Duration;

public class PostgreSqlNodeImpl extends SoftwareProcessImpl implements PostgreSqlNode {

    private static final Logger LOG = LoggerFactory.getLogger(PostgreSqlNodeImpl.class);

    private SshFeed feed;

    public Class<?> getDriverInterface() {
        return PostgreSqlDriver.class;
    }
    @Override
    public PostgreSqlDriver getDriver() {
        return (PostgreSqlDriver) super.getDriver();
    }

    @Override
    public Integer getPostgreSqlPort() { return getAttribute(POSTGRESQL_PORT); }

    @Override
    public String getSharedMemory() { return getConfig(SHARED_MEMORY); }

    @Override
    public Integer getMaxConnections() { return getConfig(MAX_CONNECTIONS); }

    @Override
    public void init() {
        super.init();
        getMutableEntityType().addEffector(EXECUTE_SCRIPT, new EffectorBody<String>() {
            @Override
            public String call(ConfigBag parameters) {
                return executeScript((String) parameters.getStringKey("commands"));
            }
        });
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        setAttribute(DATASTORE_URL, String.format("postgresql://%s:%s/", getAttribute(HOSTNAME), getAttribute(POSTGRESQL_PORT)));

        Maybe<SshMachineLocation> machine = Locations.findUniqueSshMachineLocation(getLocations());

        if (machine.isPresent()) {
            String cmd = getDriver().getStatusCmd();

            feed = SshFeed.builder()
                    .entity(this)
                    .machine(machine.get())
                    .period(Duration.millis(getConfig(POLL_PERIOD)))
                    .poll(new SshPollConfig<Boolean>(SERVICE_UP)
                            .command(cmd)
                            .setOnSuccess(true)
                            .setOnFailureOrException(false))
                    .build();
        } else {
            LOG.warn("Location set {} does not an ssh-machine location, so not polling for status; setting serviceUp immediately", getLocations());
            setAttribute(SERVICE_UP, true);
        }
    }

    @Override
    protected void disconnectSensors() {
        if (feed != null) feed.stop();
        super.disconnectSensors();
    }
    
    @Override
    public String getShortName() {
        return "PostgreSQL";
    }

    @Override
    public String executeScript(String commands) {
        return getDriver()
                .executeScriptAsync(commands)
                .block()
                .getStdout();
    }
}
