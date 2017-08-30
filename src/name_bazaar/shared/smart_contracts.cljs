(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x9b20da8b18762618a508a757dd65f2c01792ba19"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x004d973234339ccba513fa913e335ae3bf1c163f"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x34c134015689c498a7996d061b662fb27f6781a2"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x8cc436d80304887169dd4b31ba05781fd14713b9"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xf3dd9c67bc26673828716bf426d47cb8abff4ea2"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0x376d9f19a9e255ff84f14ef5c0833d00880b6666"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x4e5474de2e6d6d3ca85c285832df4b781e1cb55b"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x0b2f39fb0448f5e7bc6d23d8bdbcead356291938"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xae79319eb700def8a7bd8a5eed0709401bed8953"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})