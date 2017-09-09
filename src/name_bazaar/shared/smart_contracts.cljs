(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x254614f653381e915ff1c6fe16fd0b1558720a0a"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x0e05d3744019c715aaedcc9a36b25d27b171c428"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x4d7ef61d2228ef8e13e114bc3240093bd016212b"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0xa77fa789cd9ebbd297f2651d841737c1045ebeb8"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x7785e9050435cc103d6b5b663561facef0c86a67"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0x39865916a2b92b248e3e185d4e5298f1bd428678"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x31d465eefa9d0e44f5546e13ba02e4f9513e0561"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0xcde86338d0ae5c0ca163e0274bd5ea62b1af936e"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x9d26068b59a9777c326f4653d5e481aaec7fde1a"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})