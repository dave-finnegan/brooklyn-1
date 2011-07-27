package brooklyn.test.entity

import java.util.Collection

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import brooklyn.entity.Entity
import brooklyn.entity.group.DynamicCluster
import brooklyn.entity.webapp.JavaWebApp
import brooklyn.location.Location
import brooklyn.location.basic.SshMachineLocation
import brooklyn.util.SshBasedAppSetup

/**
 * Mock web application server entity for testing.
 */
public class TestJavaWebAppEntity extends JavaWebApp {
	protected static final Logger LOG = LoggerFactory.getLogger(TestJavaWebAppEntity)

    public TestJavaWebAppEntity(Map properties=[:], Entity owner=null) {
        super(properties, owner)
    }

	public void waitForHttpPort() { }

	public SshBasedAppSetup getSshBasedSetup(SshMachineLocation loc) { null }

	public void initJmxSensors() { }

    @Override
	public void start(Collection<Location> loc) {
        LOG.trace "Starting {}", this
    }

    @Override
	void stop() {
        LOG.trace "Stopping {}", this
    }

    @Override
    public void restart() {
        throw new UnsupportedOperationException()
    }

	@Override
    String toString() {
        return "Entity["+id[-8..-1]+"]"
    }

	public synchronized void spoofRequest() {
		def rc = getAttribute(JavaWebApp.REQUEST_COUNT) ?: 0
		setAttribute(JavaWebApp.REQUEST_COUNT, rc+1)
	}
}

/**
 * Mock cluster entity for testing.
 */
public class TestCluster extends DynamicCluster {
	public int size
	        
	TestCluster(int initialSize) {
	    super(newEntity: {})
	    size = initialSize
	}
	        
	@Override
	public int getCurrentSize() {
	    return size
	}
}