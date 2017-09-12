(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x068a60caef545e4a30fa3e53f383540a951abe18"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x624f2bcf51b57bf1bb37ba3e64eaa333aa0cf852"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x89051acf0b75d8a4cbfb3f9dd1c6ea65acbcf7c1"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0xf4220e40a00e55ea1cbc33550fc1805a7e545480"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xdf2ad8f81394f96372e821ffe86070dcdfe943b5"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0x9b5e67a9d562258ac038d9da17d41f8d72c63b4f"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x00cf608127541d69506e4814d2b73adbb8a87636"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x99953ea688e7817db908b160638c3d8032c2fd83"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xed7035ec3d0225fdc00317e658df91570f68a8cc"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})