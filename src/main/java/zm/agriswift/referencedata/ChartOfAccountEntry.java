package zm.agriswift.referencedata;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "chart_of_accounts")
public class ChartOfAccountEntry {

    @Getter
    @Id
    @Column(name = "account_code", length = 20)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Getter
    @Column(name = "account_category", nullable = false, length = 40)
    private String accountCategory;

    protected ChartOfAccountEntry() {
        // JPA
    }

}

