# agriswift

AgriSwift is a permissioned, blockchain-powered fintech orchestration platform designed as an alternative disbursement channel for the Food Reserve Agency (FRA) of Zambia.
It addresses the structural "liquidity trap" inherent in traditional bank-based crop payout models by facilitating instant, 
multi-party digital settlements to smallholder farmers through mobile money and integrated banking gateways.

## Tech stack 
Backend: Spring Boot (Java) 
Frontend: React, CSS3 
Smart Contracts (Chaincode): Go (Golang) hosted on Hyperledger Fabric 
Orchestration Layer: Hyperledger FireFly   
Integration Standards: ISO 20022 XML Messaging via National Financial Switch (NFS)

## Payout Workflow
Farmer Registration & e-KYC: Onboarding via  Application with biometric/NRC checks against INRIS.
Entitlement Validation: Grain delivery at the FRA depot triggers entitlement cross-checks via ZIAMIS.
Channel Optimization: System routes payout according to the farmers preference  (Mobile Money vs. Direct Bank Account). 
Fund Routing Execution: ISO 20022 XML payload containing rich metadata is routed to the National Financial Switch (NFS).
Confirmation & Ledger Commitment: Upon authorization via OTP prompt, state changes are written to the Hyperledger Fabric ledger.
Treasury Synchronization: Dashboards reflect real-time settlement states instantly for government auditing.

## Regulatory & Data Protection Compliance
AgriSwift will comply with the Zambia Data Protection Act of 2021:  
Data Sovereignty: All Fabric peer nodes, IPFS stores, and databases are to be hosted within domestic national data centers.

**Author** : **CHIYUME CHUNGA** 
**ZICTA ICT INNOVATION PROGRAMME 2026 COHORT**