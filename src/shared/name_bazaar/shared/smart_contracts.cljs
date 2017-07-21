(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:instant-buy-offering-factory
 {:name "InstantBuyOfferingFactory",
  :address "0x7737179a42e09de865cd82521c23ef0dee36ebbd"},
 :instant-buy-offering-library
 {:name "InstantBuyOfferingLibrary",
  :address "0xd4017d663fb53a1cdc756339e33e2b7330a4d4e1"},
 :english-auction-offering-library
 {:name "EnglishAuctionOfferingLibrary",
  :address "0x50d3a7eece7a79a5579a9bd9769de7c535922642"},
 :english-auction-offering-factory
 {:name "EnglishAuctionOfferingFactory",
  :address "0xd1d9e854720f47d204734b6c01fd4aeda0550b0b"},
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
 {:name "ENS", :address "0xf53cab410d3161d3287523ecc698d901a620a26c"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x6d7029de547160cf98efe38775ec7aecac45d7f6"},
 :test-one
 {:name "TestOne",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x1484dc56a8800bffb531ca4fd0d5a80c09e20e9c"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xaaec8c0450e896df0b4c753d505399772077cbc0"},
 :offering
 {:name "Offering",
  :address "0x0000000000000000000000000000000000000000"}})