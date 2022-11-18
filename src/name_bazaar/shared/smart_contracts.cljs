(ns name-bazaar.shared.smart-contracts)
    (def smart-contracts
      {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
       :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x9C51161bA2FB02Cc0a403332B607117685f34831"}
       :offering-registry {:name "OfferingRegistry" :address "0xBe52e245b459C778D4a4877B5207E16Fa36A5195"}
       :buy-now-offering {:name "BuyNowOffering" :address "0x1F13bD63C0daE14c6119BdfE6f638Fc43c6be3d6"}
       :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0x5a5Fed02Cce002a1499d41fA4b81DdEd59C2f03E"}
       :auction-offering {:name "AuctionOffering" :address "0x0ddA158043afA545BA5F7bc1F5F9f91D42C5E28d"}
       :auction-offering-factory {:name "AuctionOfferingFactory" :address "0xf826506fA715a7609bcD4008F1029EaD6df36950"}
       :district0x-emails {:name "District0xEmails" :address "0x0A1df1Ca91FF4baCd8BDE4D999F75F195e7BDaF0"}
       :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x0000000000000000000000000000000000000000"}
       :public-resolver {:name "NamebazaarDevPublicResolver" :address "0xE264d5bb84bA3b8061ADC38D3D76e6674aB91852"}
       :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0xD5610A08E370051a01fdfe4bB3ddf5270af1aA48"}})