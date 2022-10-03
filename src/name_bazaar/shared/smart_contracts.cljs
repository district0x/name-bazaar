(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x2375bbB61957806adb48d51b9F1a280d95D11144"}
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0xAE65329828CDDc44a753f6A8Dce6E32E1524b886"}
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0xe592779bEA0553B0Ae48036F86eBfe7e054B5e4d"}
       :offering-registry {:name "OfferingRegistry" :address "0xB0F0A12E08670b44dB5127894aa4e7bf6F597132"}
       :buy-now-offering {:name "BuyNowOffering" :address "0x7597637C2902c278ea5166e44D3fBa9B0bFFb577"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0x5d498e434cCb6d7959812e797cF605b6A946c67c"}
       :auction-offering {:name "AuctionOffering" :address "0x94275f01c2b6722175C7da9714b4d9e6547BA830"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0x7573122Fd401Ee4348407BbCC34ebD080cf8a2C7"}
       :district0x-emails {:name "District0xEmails" :address "0x2881C6ca42B25845382CB90D7c1672cd5DA4bdb4"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x8258FdA61b2a0F56C7518959ba3c78AfF4b16f7f"}})