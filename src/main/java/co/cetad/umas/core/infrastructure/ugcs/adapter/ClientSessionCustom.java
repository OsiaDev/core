package co.cetad.umas.core.infrastructure.ugcs.adapter;

import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.client.ClientSession;
import com.ugcs.ucs.proto.DomainProto.EventSubscriptionWrapper;
import com.ugcs.ucs.proto.DomainProto.ObjectModificationSubscription;
import com.ugcs.ucs.proto.MessagesProto.SubscribeEventRequest;
import com.ugcs.ucs.proto.MessagesProto.SubscribeEventResponse;

public class ClientSessionCustom extends ClientSession {

    public ClientSessionCustom(Client client) {
        super(client);
    }

    public int getClientId() {
        return clientId;
    }

    public int subscribeToObjectModifications() throws Exception {
        SubscribeEventRequest request = SubscribeEventRequest.newBuilder()
                .setClientId(clientId)
                .setSubscription(EventSubscriptionWrapper.newBuilder()
                        .setObjectModificationSubscription(ObjectModificationSubscription.newBuilder()))
                .build();
        SubscribeEventResponse response = client.execute(request);
        return response.getSubscriptionId();
    }

}
