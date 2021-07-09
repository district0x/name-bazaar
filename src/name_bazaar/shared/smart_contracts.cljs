(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      ;; Ropsten ENSRegistry
      ;; https://docs.ens.domains/ens-deployments
      ;; https://ropsten.etherscan.io/address/0x00000000000c2e074ec69a0dfb2997ba6c7d2e1e
      {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
       ;; Ropsten BaseRegistrarImplementation
       ;; https://ropsten.etherscan.io/address/0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85"}
       :offering-registry {:name "OfferingRegistry" :address "0xa3634fE2E2973d5d0cB7f2a1b2b0327A0B6e2d0D"}
       :buy-now-offering {:name "BuyNowOffering" :address "0xfF58F3De9319f37BF9b6F355694fe4431b58E6a4"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0x8dD4F32663771D73b387A983D1c40BdB431241fd"}
       :auction-offering {:name "AuctionOffering" :address "0xa9c221236b9c16Ef08bccd84F89cE5b525B817A9"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0x18eF935Cd12b962739538B35F94B0E9fE0e19D59"}
       :district0x-emails {:name "District0xEmails" :address "0x31BF9DB8b432b0d108491e2bc9423d6C6d419E20"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x0000000000000000000000000000000000000000"}
       ;; Ropsten PublicResolver
       ;; https://ropsten.etherscan.io/address/0x42d63ae25990889e35f215bc95884039ba354115
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0x42D63ae25990889E35F215bC95884039Ba354115"}
       ;; Ropsten ReverseRegistrar
       ;; https://ropsten.etherscan.io/address/0x6F628b68b30Dc3c17f345c9dbBb1E483c2b7aE5c
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0x6F628b68b30Dc3c17f345c9dbBb1E483c2b7aE5c"}
       })