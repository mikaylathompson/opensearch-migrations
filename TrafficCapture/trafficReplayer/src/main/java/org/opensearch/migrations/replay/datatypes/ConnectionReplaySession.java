package org.opensearch.migrations.replay.datatypes;

import java.util.function.BiFunction;

import org.opensearch.migrations.replay.tracing.IReplayContexts;
import org.opensearch.migrations.utils.OnlineRadixSorter;
import org.opensearch.migrations.utils.TextTrackedFuture;
import org.opensearch.migrations.utils.TrackedFuture;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * This class contains everything that is needed to replay packets to a specific channel.
 * ConnectionClientPool and RequestSenderOrchestrator manage the data within these objects.
 * The ConnectionClientPool manages lifecycles, caching, and the underlying connection.  The
 * RequestSenderOrchestrator handles scheduling writes and requisite activities (prep, close)
 * that will go out on the channel.
 */
@Slf4j
public class ConnectionReplaySession {

    /**
     * We need to store this separately from the channelFuture because the channelFuture itself is
     * vended by a CompletableFuture (e.g. possibly a rate limiter).  If the ChannelFuture hasn't
     * been created yet, there's nothing to hold the channel, nor the eventLoop.  We _need_ the
     * EventLoop so that we can route all calls for this object into that loop/thread.
     */
    public final EventLoop eventLoop;
    public final OnlineRadixSorter scheduleSequencer;
    @Getter
    private final BiFunction<EventLoop, IReplayContexts.ITargetRequestContext, TrackedFuture<String, ChannelFuture>> channelFutureFutureFactory;
    private ChannelFuture cachedChannel; // only can be accessed from the eventLoop thread
    public final TimeToResponseFulfillmentFutureMap schedule;
    @Getter
    private final IReplayContexts.IChannelKeyContext channelKeyContext;

    @SneakyThrows
    public ConnectionReplaySession(
        EventLoop eventLoop,
        IReplayContexts.IChannelKeyContext channelKeyContext,
        BiFunction<EventLoop, IReplayContexts.ITargetRequestContext, TrackedFuture<String, ChannelFuture>> channelFutureFutureFactory
    ) {
        this.eventLoop = eventLoop;
        this.channelKeyContext = channelKeyContext;
        this.scheduleSequencer = new OnlineRadixSorter(0);
        this.schedule = new TimeToResponseFulfillmentFutureMap();
        this.channelFutureFutureFactory = channelFutureFutureFactory;
    }

    public TrackedFuture<String, ChannelFuture> getChannelFutureInAnyState() {
        TextTrackedFuture<ChannelFuture> trigger = new TextTrackedFuture<>("procuring a connection");
        eventLoop.submit(() -> trigger.future.complete(cachedChannel));
        return trigger;
    }

    public TrackedFuture<String, ChannelFuture>
    getChannelFutureInActiveState(IReplayContexts.ITargetRequestContext ctx)
    {
        TextTrackedFuture<ChannelFuture> trigger = new TextTrackedFuture<>("procuring a connection");
        eventLoop.submit(() -> {
            if (cachedChannel != null && cachedChannel.channel().isActive()) {
                trigger.future.complete(cachedChannel);
            } else {
                channelFutureFutureFactory.apply(eventLoop, ctx)
                    .whenComplete((v, t) -> {
                        if (t == null) {
                            trigger.future.complete(v);
                        } else {
                            trigger.future.completeExceptionally(TrackedFuture.unwindPossibleCompletionException(t));
                        }
                    }, () -> "working to signal back to an event loop trigger")
                    .whenComplete((v,t) -> cachedChannel = v, () -> "Setting cached channel");
            }
        });
        return trigger;
    }

    public boolean hasWorkRemaining() {
        return !scheduleSequencer.isEmpty() || schedule.hasPendingTransmissions();
    }

    public long calculateSizeSlowly() {
        return (long) schedule.timeToRunnableMap.size() + scheduleSequencer.size();
    }
}
