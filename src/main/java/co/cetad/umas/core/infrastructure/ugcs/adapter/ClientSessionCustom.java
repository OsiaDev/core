package co.cetad.umas.core.infrastructure.ugcs.adapter;

import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.client.ClientSession;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.MessagesProto;

public class ClientSessionCustom extends ClientSession {

    public ClientSessionCustom(Client client) {
        super(client);
    }

    public int getClientId() {
        return clientId;
    }

    public int subscribeToObjectModifications() throws Exception {
        MessagesProto.SubscribeEventRequest request = MessagesProto.SubscribeEventRequest.newBuilder()
                .setClientId(clientId)
                .setSubscription(DomainProto.EventSubscriptionWrapper.newBuilder()
                        .setTelemetrySubscription(DomainProto.TelemetrySubscription.newBuilder()))
                .build();
        MessagesProto.SubscribeEventResponse response = client.execute(request);
        return response.getSubscriptionId();
    }

}
