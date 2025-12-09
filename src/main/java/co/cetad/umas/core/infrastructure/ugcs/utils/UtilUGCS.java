package co.cetad.umas.core.infrastructure.ugcs.utils;

import com.ugcs.ucs.proto.DomainProto.Failsafe;
import com.ugcs.ucs.proto.DomainProto.FailsafeReason;
import com.ugcs.ucs.proto.DomainProto.FailsafeAction;

import java.util.List;

public class UtilUGCS {

    private static final List<Failsafe> DEFAULT_FAILSAFES = List.of(
            Failsafe.newBuilder()
                    .setReason(FailsafeReason.FR_RC_LOST)
                    .setAction(FailsafeAction.FA_GO_HOME)
                    .build(),

            Failsafe.newBuilder()
                    .setReason(FailsafeReason.FR_GPS_LOST)
                    .setAction(FailsafeAction.FA_WAIT)
                    .build(),

            Failsafe.newBuilder()
                    .setReason(FailsafeReason.FR_LOW_BATTERY)
                    .setAction(FailsafeAction.FA_LAND)
                    .build(),

            Failsafe.newBuilder()
                    .setReason(FailsafeReason.FR_DATALINK_LOST)
                    .setAction(FailsafeAction.FA_GO_HOME)
                    .build()
    );

    public static List<Failsafe> getDefaultFailsafes() {
        return DEFAULT_FAILSAFES;
    }

}
