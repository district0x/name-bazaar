(ns name-bazaar.shared.smart-contracts)
(def smart-contracts
  {:ens {:name "ENSRegistry" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
   :eth-registrar {:name "NameBazaarDevRegistrar" :address "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85"}
   :offering-registry {:name "OfferingRegistry" :address "0x3Ba421F5Dc39c4451B250590153B7c02C5fE3898"}
   :buy-now-offering {:name "BuyNowOffering" :address "0xFbcB7dba96375383AAAa1Dd2C85B79Bf2f61a6AC"}
   :buy-now-offering-factory {:name "BuyNowOfferingFactory" :address "0xAe22111f72a884b3942CC2755b9E43AD90C97785"}
   :auction-offering {:name "AuctionOffering" :address "0x2D4CBa7458dBF71cdd91a872af7c929E0A49E783"}
   :auction-offering-factory {:name "AuctionOfferingFactory" :address "0xe22A8f173C27a21Ae73f8Fb68565A70Aae963336"}
   :district0x-emails {:name "District0xEmails" :address "0x6142077FC24BC86ef7F8624CFE8D9235eBFc5b2F"}
   :reverse-name-resolver {:name "NamebazaarDevNameResolver" :address "0x0000000000000000000000000000000000000000"}
   :public-resolver {:name "NamebazaarDevPublicResolver" :address "0xE264d5bb84bA3b8061ADC38D3D76e6674aB91852"}
   :reverse-registrar {:name "NamebazaarDevReverseRegistrar" :address "0xD5610A08E370051a01fdfe4bB3ddf5270af1aA48"}})