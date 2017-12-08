(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x3a11344d66a72784dd440820ba6422a534a4206e"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x575262e80edf7d4b39d95422f86195eb4c21bb52"},
 :registrar
 {:name "Registrar",
  :address "0x6090A6e47849629b7245Dfa1Ca21D94cd15878Ef"},
 :reverse-registrar
 {:name "ReverseRegistrar",
  :address "0x9062C0A6Dbd6108336BcBe4593a3D1cE05512069"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x7f5dbcab54cb17cd494477d4f11a2b7ba470fb3a"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x314159265dD8dbb310642f98f50C066173C1259b"},
 :public-resolver
 {:name "PublicResolver", :address "0x5FfC014343cd971B7eb70732021E26C35B744cc4"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x34e400a8b4da8a23b5eaf81b46d3a887669a45b9"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x5065ef0724b76421aeaafa87ba752d6c5d5499b5"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x016bdfcf49ecdd9592e493cd4a75048ce09d6d75"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x6676f9a4be165daa756816f3234d5e019032728e"}})
