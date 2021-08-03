(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85"}
       :offering-registry {:name "OfferingRegistry" :address "0x5B5B2B386a84546Fa75E9472e3096C26576B6d24"}
       :buy-now-offering {:name "BuyNowOffering" :address "0x45141Ef2E09412eAd94793617507A0F487249fB0"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0xF9Bc1b1bBC9C91864e68D861038282ebdcf2449A"}
       :auction-offering {:name "AuctionOffering" :address "0x9cf308c1d04AeD7eEA994f029cA2E39517C6372f"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0xF787200012f8d3EA6Ee686577aA73Ed4a3e3ee0C"}
       :district0x-emails {:name "District0xEmails" :address "0x02441d5dd828CccB3F81ae702eeEccc2142a192e"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0xA2C122BE93b0074270ebeE7f6b7292C7deB45047"}
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0x4976fb03C32e5B8cfe2b6cCB31c09Ba78EBaBa41"}
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0x084b1c3C81545d370f3634392De611CaaBFf8148"}
       })