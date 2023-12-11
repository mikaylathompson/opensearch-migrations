package org.opensearch.migrations.replay;

import lombok.NonNull;
import org.opensearch.migrations.tracing.SimpleMeteringClosure;
import org.opensearch.migrations.replay.datatypes.ISourceTrafficChannelKey;
import org.opensearch.migrations.replay.datatypes.ITrafficStreamKey;
import org.opensearch.migrations.replay.datatypes.UniqueReplayerRequestKey;
import org.opensearch.migrations.replay.tracing.ChannelKeyContext;
import org.opensearch.migrations.replay.tracing.RequestContext;
import org.opensearch.migrations.trafficcapture.protos.TrafficStream;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Accumulation {
    public static final String TELEMETRY_SCOPE_NAME = "Accumulator";
    public static final SimpleMeteringClosure METERING_CLOSURE = new SimpleMeteringClosure(TELEMETRY_SCOPE_NAME);

    enum State {
        // Ignore all initial READs, the first EOM & the following WRITEs (if they or EOMs exist)
        IGNORING_LAST_REQUEST,
        // Finished scanning past initial READs.  The next request should be processed,
        // so be on the lookout for the next READ
        WAITING_FOR_NEXT_READ_CHUNK,
        ACCUMULATING_READS,
        ACCUMULATING_WRITES
    }

    public final ISourceTrafficChannelKey trafficChannelKey;
    public final ChannelKeyContext channelContext;
    private RequestResponsePacketPair rrPair;
    AtomicLong newestPacketTimestampInMillis;
    State state;
    AtomicInteger numberOfResets;
    int startingSourceRequestIndex;

    public Accumulation(ITrafficStreamKey key, TrafficStream ts) {
        this(key, ts.getPriorRequestsReceived()+(ts.hasLastObservationWasUnterminatedRead()?1:0),
                ts.getLastObservationWasUnterminatedRead());
    }

    public Accumulation(@NonNull ITrafficStreamKey trafficChannelKey, int startingSourceRequestIndex) {
        this(trafficChannelKey, startingSourceRequestIndex, false);
    }

    public Accumulation(@NonNull ITrafficStreamKey trafficChannelKey,
                        int startingSourceRequestIndex, boolean dropObservationsLeftoverFromPrevious) {
        this.trafficChannelKey = trafficChannelKey;
        numberOfResets = new AtomicInteger();
        this.newestPacketTimestampInMillis = new AtomicLong(0);
        this.startingSourceRequestIndex = startingSourceRequestIndex;
        this.state =
                dropObservationsLeftoverFromPrevious ? State.IGNORING_LAST_REQUEST : State.WAITING_FOR_NEXT_READ_CHUNK;
        channelContext = new ChannelKeyContext(trafficChannelKey,
                METERING_CLOSURE.makeSpanContinuation("processingChannel", null));
    }

    public RequestResponsePacketPair getOrCreateTransactionPair(ITrafficStreamKey forTrafficStreamKey) {
        if (rrPair != null) {
            return rrPair;
        }
        this.rrPair = new RequestResponsePacketPair(forTrafficStreamKey,
                new RequestContext(channelContext, getRequestKey(forTrafficStreamKey),
                        METERING_CLOSURE.makeSpanContinuation("accumulatingRequest")));
        return rrPair;
    }

    public UniqueReplayerRequestKey getRequestKey() {
        return getRequestKey(getRrPair().getBeginningTrafficStreamKey());
    }

    private UniqueReplayerRequestKey getRequestKey(@NonNull ITrafficStreamKey tsk) {
        return new UniqueReplayerRequestKey(tsk, startingSourceRequestIndex, getIndexOfCurrentRequest());
    }

    public boolean hasSignaledRequests() {
        return numberOfResets.get() > 0 || state == Accumulation.State.ACCUMULATING_WRITES;
    }

    public boolean hasRrPair() {
        return rrPair != null;
    }

    /**
     * It is illegal to call this when rrPair may be equal to null.  If the caller isn't sure,
     * hasRrPair() should be called to first check.
     * @return
     */
    public @NonNull RequestResponsePacketPair getRrPair() {
        assert rrPair != null;
        return rrPair;
    }

    public void rotateRequestGatheringToResponse() {
        var ctx = rrPair.requestContext;
        ctx.getCurrentSpan().end();
        rrPair.requestContext = new RequestContext(ctx.getEnclosingScope(),
                ctx.getReplayerRequestKey(),
                METERING_CLOSURE.makeSpanContinuation("accumulatingResponse"));
    }

    public Instant getLastTimestamp() {
        return Instant.ofEpochMilli(newestPacketTimestampInMillis.get());
    }

    public AtomicLong getNewestPacketTimestampInMillisReference() {
        return newestPacketTimestampInMillis;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Accumulation{");
        sb.append("rrPair=").append(rrPair);
        sb.append(", state=").append(state);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Accumulations are reset for each new HttpRequest that is discovered.  This value indicates how
     * many times the object has been reset, indicating in a logical sequence of requests against this
     * Accumulation, what index would the current data be a part of?  Calling resetForNextRequest()
     * will increase this value by 1.
     * @return
     */
    public int getIndexOfCurrentRequest() {
        return numberOfResets.get();
    }

    public void resetForNextRequest() {
        numberOfResets.incrementAndGet();
        this.state = State.ACCUMULATING_READS;
        this.rrPair = null;
    }

    public void resetToIgnoreAndForgetCurrentRequest() {
        if (state == State.IGNORING_LAST_REQUEST) {
            --startingSourceRequestIndex;
        }
        this.state = State.WAITING_FOR_NEXT_READ_CHUNK;
        this.rrPair = null;
    }
}
