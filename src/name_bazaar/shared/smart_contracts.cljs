(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85"}
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0x42D63ae25990889E35F215bC95884039Ba354115"}
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0x6F628b68b30Dc3c17f345c9dbBb1E483c2b7aE5c"}
       :offering-registry {:name "OfferingRegistry" :address "0x3641e0B4C08406F4B7bFB27506bb4580D4ad48f5"}
       :buy-now-offering {:name "BuyNowOffering" :address "0xA7563327b99f8BB4B7A27125c5c2c895270f931f"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0xf9915120D879C667b56B3a46394454601b7E9e74"}
       :auction-offering {:name "AuctionOffering" :address "0x409A35905596E4D2F5AC34D7D413b938039fe21c"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0x0774ac3FDcb5a490bB15277f0589dcfF31c605e7"}
       :district0x-emails {:name "District0xEmails" :address "0xEfD19B05eb4698F0C21ca8182da61fb8a5EFE7A6"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x0000000000000000000000000000000000000000"}})