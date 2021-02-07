(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x98a220dcd296355bc63da5753a62f6baf11a1b6e"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x27892c87e769b4edb10c3319bc4fc59ddee73385"},
 :name-bazaar-registrar
 {:name "NameBazaarRegistrar",
  :address "0x1263d914ee2393ace43828ed5fd9b9a950aab1d9"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x84d3e0f27c0eccaa49b8ae51f8e4653f64abde62"},
 :reverse-registrar
 {:name "ReverseRegistrar",
  :address "0x9062C0A6Dbd6108336BcBe4593a3D1cE05512069"},
 :public-resolver
 {:name "PublicResolver",
  :address "0x5FfC014343cd971B7eb70732021E26C35B744cc4"},
 :ens
 {:name "ENSRegistry",
  :address "0xd4ae96cb146d20d85f998f0f24ce562801f50084"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xbeb20ec478bbab325eadc9790cc24362c8b6e9b0"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0xdd95175cad07f1a25bce5e54bfda4d02290302bf"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x2cfd99770f16cce3463fd376eab978632a501a58"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0xb02c63815fd3a90def33a4611070b2dd417a04f8"}})
