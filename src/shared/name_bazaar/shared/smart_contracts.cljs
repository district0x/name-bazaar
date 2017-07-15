(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0x5faa5049ff3f3ab70b85c22ecbe811aeeab0d6df"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0xa895f58cedcaf71cbe110da8caa4e124669193b1"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0x40a482dc3896385a8ec9f59fe0582d9b831741f0"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0x8a65886c5d808c7dabcc1146f48fbe83270b5af0"},
 :english-auction-offering
 {:name "EnglishAuctionOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :instant-buy-offering
 {:name "InstantBuyOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xd2138280b0bfaa40c3ee560ce34f0b05ae59cf9b"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xf4bc511dd535dcd27b28a0b2d8061ca2074b0960"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0xcbb0fef08f0d65cb7dd99af427ef544d5aeca9d3"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x7ce880c4f117a0c0463b6ece4bd396053d20755f"},
 :offering
 {:name "Offering",
  :address "0x0000000000000000000000000000000000000000"}})