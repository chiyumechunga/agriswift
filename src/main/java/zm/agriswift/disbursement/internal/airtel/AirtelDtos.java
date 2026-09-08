package zm.agriswift.disbursement.internal.airtel;

public final class AirtelDtos {

    private AirtelDtos() {}

    public record DisbursementRequest(
            Payer payer,
            String reference,
            String pin,
            Transaction transaction
    ) {
        public record Payer(String msisdn, String walletType) {}
        public record Transaction(long amount, String id, String type) {}
    }

    public record DisbursementResponse(
            String reference,
            String id,
            String status,
            String message,
            String code
    ) {}
}