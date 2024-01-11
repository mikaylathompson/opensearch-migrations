package org.opensearch.migrations.tracing;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;

public interface IInstrumentConstructor {
    Span buildSpan(IInstrumentationAttributes enclosingScope, String scopeName, String spanName, Span linkedSpan,
                   AttributesBuilder attributesBuilder);
}
