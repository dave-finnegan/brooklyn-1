package brooklyn.entity.basic;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import brooklyn.entity.software.MachineLifecycleEffectorTasks;
import brooklyn.location.Location;
import brooklyn.management.Task;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.task.Tasks;

public class SameServerEntityImpl extends AbstractEntity implements SameServerEntity {

    private static final MachineLifecycleEffectorTasks LIFECYCLE_TASKS = new SameServerDriverLifecycleEffectorTasks();

    /**
     * Restarts the entity and its children.
     * <p/>
     * Subclasses should override {@link #doRestart} to customise behaviour. */
    @Override
    public final void restart() {
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doRestart();
        } else {
            Task<?> task = Tasks.builder().name("restart").body(new Runnable() { public void run() { doRestart(); } }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    /**
     * Starts the entity and its children in the given locations.
     * <p/>
     * Subclasses should override {@link #doStart} to customise behaviour.
     */
    @Override
    public final void start(final Collection<? extends Location> locations) {
        checkNotNull(locations, "locations");
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doStart(locations);
        } else {
            Task<?> task = Tasks.builder().name("start").body(new Runnable() { public void run() { doStart(locations); } }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    /**
     * Stops the entity and its children.
     * <p/>
     * Subclasses should override {@link #doStop} to customise behaviour.
     */
    @Override
    public final void stop() {
        if (DynamicTasks.getTaskQueuingContext() != null) {
            doStop();
        } else {
            Task<?> task = Tasks.builder().name("stop").body(new Runnable() { public void run() { doStop(); } }).build();
            Entities.submit(this, task).getUnchecked();
        }
    }

    /**
     * To be overridden instead of {@link #start(Collection)}; sub-classes should call
     * {@code super.doStart(locations)} and should add do additional work via tasks,
     * executed using {@link DynamicTasks#queue(String, java.util.concurrent.Callable)}.
     */
    protected void doStart(Collection<? extends Location> locations) {
        LIFECYCLE_TASKS.start(locations);
    }

    /**
     * To be overridden instead of {@link #stop()}; sub-classes should call {@code
     * super.doStop()} and should add do additional work via tasks, executed using
     * {@link DynamicTasks#queue(String, java.util.concurrent.Callable)}.
     */
    protected void doStop() {
        LIFECYCLE_TASKS.stop();
    }

    /**
     * To be overridden instead of {@link #restart()}; sub-classes should call {@code
     * super.doRestart()} and should add do additional work via tasks, executed using
     * {@link DynamicTasks#queue(String, java.util.concurrent.Callable)}.
     */
    protected void doRestart() {
        LIFECYCLE_TASKS.restart();
    }

}
