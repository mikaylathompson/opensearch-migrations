package org.opensearch.migrations.replay.datahandlers.http;

import java.util.Map;

import org.opensearch.migrations.transform.JsonKeysForHttpMessage;

public class HttpJsonRequestWithFaultingPayload extends HttpJsonMessageWithFaultingPayload {

    public HttpJsonRequestWithFaultingPayload() {
        super();
    }

    public HttpJsonRequestWithFaultingPayload(Map<String, ?> m) {
        super(m);
        put(JsonKeysForHttpMessage.HTTP_MESSAGE_SCHEMA_VERSION_KEY, MESSAGE_SCHEMA_VERSION);
    }

    public String method() {
        return (String) this.get(JsonKeysForHttpMessage.METHOD_KEY);
    }

    public void setMethod(String value) {
        this.put(JsonKeysForHttpMessage.METHOD_KEY, value);
    }

    public String path() {
        return (String) this.get(JsonKeysForHttpMessage.URI_KEY);
    }

    public void setPath(String value) {
        this.put(JsonKeysForHttpMessage.URI_KEY, value);
    }

    public String protocol() {
        return (String) this.get(JsonKeysForHttpMessage.PROTOCOL_KEY);
    }

    public void setProtocol(String value) {
        this.put(JsonKeysForHttpMessage.PROTOCOL_KEY, value);
    }
}
