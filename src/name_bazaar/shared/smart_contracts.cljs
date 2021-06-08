(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      ;; Ropsten ENSRegistry
      ;; https://docs.ens.domains/ens-deployments
      ;; https://ropsten.etherscan.io/address/0x00000000000c2e074ec69a0dfb2997ba6c7d2e1e
      {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
       ;; Ropsten BaseRegistrarImplementation
       ;; https://ropsten.etherscan.io/address/0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85"}
       :offering-registry {:name "OfferingRegistry" :address "0xcf94DD1Efe4bf21eb7E9435b77eB27E5657CCE3d"}
       :buy-now-offering {:name "BuyNowOffering" :address "0xcf56BEfA85Dc6c5dD433a09CbD3FD584E8A172ef"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0x02ae694b714B3307E368213E75f08567BaaBBf4a"}
       :auction-offering {:name "AuctionOffering" :address "0xBc32f7375aF7b9F4FCc5be5b76ea7f84f91dbee8"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0x35cc9F93f81bf6Ce63f15008Ec925BE09fF46F9A"}
       :district0x-emails {:name "District0xEmails" :address "0x2A33bC3710164Cf85b6c61c9Ec6edc685Fc13022"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x0000000000000000000000000000000000000000"}
       ;; Ropsten PublicResolver
       ;; https://ropsten.etherscan.io/address/0x42d63ae25990889e35f215bc95884039ba354115
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0x42D63ae25990889E35F215bC95884039Ba354115"}
       ;; Ropsten ReverseRegistrar
       ;; https://ropsten.etherscan.io/address/0x6F628b68b30Dc3c17f345c9dbBb1E483c2b7aE5c
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0x6F628b68b30Dc3c17f345c9dbBb1E483c2b7aE5c"}})