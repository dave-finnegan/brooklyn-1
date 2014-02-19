package brooklyn.entity.nosql.mongodb.sharding;

import java.util.Iterator;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.nosql.mongodb.MongoDBReplicaSet;
import brooklyn.entity.nosql.mongodb.MongoDBTestHelper;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.basic.LocalhostMachineProvisioningLocation;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.entity.TestApplication;

import com.google.common.collect.ImmutableList;
import com.mongodb.DBObject;

public class MongoDBShardedDeploymentIntegrationTest {
    
    private static final Integer ROUTER_CLUSTER_SIZE = 2;
    private static final Integer REPLICASET_SIZE = 2;
    private static final Integer SHARD_CLUSTER_SIZE = 3;
    
    private TestApplication app;
    private LocalhostMachineProvisioningLocation localhostProvisioningLocation;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        localhostProvisioningLocation = new LocalhostMachineProvisioningLocation();
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app.getManagementContext());
    }
    
    private MongoDBShardedDeployment makeAndStartDeployment() {
        final MongoDBShardedDeployment deployment = app.createAndManageChild(EntitySpec.create(MongoDBShardedDeployment.class)
                .configure(MongoDBShardedDeployment.INITIAL_ROUTER_CLUSTER_SIZE, ROUTER_CLUSTER_SIZE)
                .configure(MongoDBShardedDeployment.SHARD_REPLICASET_SIZE, REPLICASET_SIZE)
                .configure(MongoDBShardedDeployment.INITIAL_SHARD_CLUSTER_SIZE, SHARD_CLUSTER_SIZE));
        app.start(ImmutableList.of(localhostProvisioningLocation));
        EntityTestUtils.assertAttributeEqualsEventually(deployment, Startable.SERVICE_UP, true);
        return deployment;
    }
    
    @Test(groups = "Integration")
    public void testCanStartAndStopDeployment() {
        MongoDBShardedDeployment deployment = makeAndStartDeployment();
        deployment.stop();
        Assert.assertFalse(deployment.getAttribute(Startable.SERVICE_UP));
    }
    
    @Test(groups = "Integration")
    public void testDeployedStructure() {
        MongoDBShardedDeployment deployment = makeAndStartDeployment();
        MongoDBConfigServerCluster configServers = deployment.getConfigCluster();
        MongoDBRouterCluster routers = deployment.getRouterCluster();
        MongoDBShardCluster shards = deployment.getShardCluster();
        Assert.assertNotNull(configServers);
        Assert.assertNotNull(routers);
        Assert.assertNotNull(shards);
        Assert.assertEquals(configServers.getCurrentSize(), MongoDBShardedDeployment.CONFIG_CLUSTER_SIZE.getDefaultValue());
        Assert.assertEquals(routers.getCurrentSize(), ROUTER_CLUSTER_SIZE);
        Assert.assertEquals(shards.getCurrentSize(), SHARD_CLUSTER_SIZE);
        for (Entity entity : deployment.getShardCluster().getMembers()) {
            Assert.assertEquals(((MongoDBReplicaSet)entity).getCurrentSize(), REPLICASET_SIZE);
        }
        for (Entity entity : configServers.getMembers()) {
            checkEntityTypeAndServiceUp(entity, MongoDBConfigServer.class);
        }
        for (Entity entity : routers.getMembers()) {
            checkEntityTypeAndServiceUp(entity, MongoDBRouter.class);
        }
        for (Entity entity : shards.getMembers()) {
            checkEntityTypeAndServiceUp(entity, MongoDBReplicaSet.class);
        }
    }
    
    @Test(groups = "Integration")
    private void testReadAndWriteDifferentRouters() {
        MongoDBShardedDeployment deployment = makeAndStartDeployment();
        Iterator<Entity> routerIterator = deployment.getRouterCluster().getMembers().iterator();
        MongoDBRouter router1 = (MongoDBRouter)routerIterator.next();
        MongoDBRouter router2 = (MongoDBRouter)routerIterator.next();
        String documentId = MongoDBTestHelper.insert(router1, "meaning-of-life", 42);
        DBObject docOut = MongoDBTestHelper.getById(router2, documentId);
        Assert.assertEquals(docOut.get("meaning-of-life"), 42);
    }
    
    private void checkEntityTypeAndServiceUp(Entity entity, Class<? extends Entity> expectedClass) {
        Assert.assertNotNull(entity);
        Assert.assertTrue(expectedClass.isAssignableFrom(entity.getClass()), "expected: " + expectedClass 
                + " on interfaces, found: " + entity.getClass().getInterfaces());
        EntityTestUtils.assertAttributeEquals(entity, Startable.SERVICE_UP, true);
    }

}