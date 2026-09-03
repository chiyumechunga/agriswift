package zm.agriswift.disbursement;

import java.util.UUID;

/** Inbound payload shape from the NFS switch / MNO async status callback. */
public record StatusCallbackRequest(UUID uetr, String status, String failureReason, String signature) {
}
