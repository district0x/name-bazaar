(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0x300ecc4d315c8c6cc4e9a1cd3b196e51a0a7ae07"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0x47eb86b18bda271e8a6335f1cc382a2115717ac7"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0xacd5d34d2c0553bfe8e064776d84dd7185c376b1"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0x4f903f17b9f1927b401387106fbab58ea6a7c553"},
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
 {:name "ENS", :address "0xdec6ab98d62d45cf294e4ae6aedd11a35165982c"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x417cdd31017d8e7f88e9b8195e2589d67303164e"},
 :test-one
 {:name "TestOne",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0xbdaa96bef729859cb17ff32d28f0d6b009777f3c"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x69aa50d91de3aac8238d1e9a04341e80d326c3b3"}
 :district0x-emails
 {:name "District0xEmails"
  :address "0x0000000000000000000000000000000000000000"}})