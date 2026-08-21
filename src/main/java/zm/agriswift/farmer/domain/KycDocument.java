package zm.agriswift.farmer.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="Kyc_documents")
@Getter
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "ipfs_cid", length = 100, nullable = false)
    private String ipfsCid;

    @Column(name = "sha256_hash", columnDefinition = "BYTEA", nullable = false)
    private byte[] sha256Hash;

    @Column(name = "captured_by_agent_id")
    private UUID capturedByAgentId;

    @Column(name = "captured_via_network")
    private String capturedViaNetwork;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private OffsetDateTime capturedAt = OffsetDateTime.now();

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    public enum DocumentType { NRC_SCAN, FACIAL_PHOTO, FINGERPRINT, OTHER }

}
