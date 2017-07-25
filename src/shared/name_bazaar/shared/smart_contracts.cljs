(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0x5a398db783811e360745778d534dc45cb438b417"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0xa4289256ced51fd09c460195ec34f55b815a9bbb"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0xfc86b7520e4599fd3a42fe832b873afc34933726"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0x716ed0211dd19ea881af33d5ec485f96c767a3f2"},
 :test-two
 {:name "TestTwo",
  :address "0x0000000000000000000000000000000000000000"},
 :english-auction-offering
 {:name "EnglishAuctionOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :instant-buy-offering
 {:name "InstantBuyOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xad8b90c94aa309caab02ad4d09d574f92fc26d61"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xbb3d87e0d8c91a18d9e1f8322758ff50e17372bc"},
 :test-one
 {:name "TestOne",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x0dbc389a8d18574ff946e245bd8145ae28cb2bdb"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x8736f5d8af20bee18f9add477d65bb7b6ec70eb5"}})