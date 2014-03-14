package brooklyn.entity.nosql.mongodb.sharding;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.nosql.mongodb.AbstractMongoDBDriver;
import brooklyn.entity.nosql.mongodb.MongoDBClientSupport;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBObject;

public class MongoDBRouterImpl extends SoftwareProcessImpl implements MongoDBRouter {
    
    private volatile FunctionFeed serviceUp;

    @Override
    public Class<?> getDriverInterface() {
        return MongoDBRouterDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        serviceUp = FunctionFeed.builder()
                .entity(this)
                .poll(new FunctionPollConfig<Boolean, Boolean>(SERVICE_UP)
                        .period(5, TimeUnit.SECONDS)
                        .callable(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                MongoDBClientSupport clientSupport = MongoDBClientSupport.forServer(MongoDBRouterImpl.this);
                                return clientSupport.getServerStatus().get("ok").equals(1.0);
                            }
                        })
                        .onException(Functions.<Boolean>constant(false)))
                .poll(new FunctionPollConfig<Integer, Integer>(SHARD_COUNT)
                        .period(5, TimeUnit.SECONDS)
                        .callable(new Callable<Integer>() {
                            public Integer call() throws Exception {
                                MongoDBClientSupport clientSupport = MongoDBClientSupport.forServer(MongoDBRouterImpl.this);
                                return (int) clientSupport.getShardCount();
                            }    
                        })
                        .onException(Functions.<Integer>constant(0)))
                .build();
    }
    
    @Override
    public void runScript(String scriptName) {
        ((AbstractMongoDBDriver)getDriver()).runScript(scriptName);
    }

}
