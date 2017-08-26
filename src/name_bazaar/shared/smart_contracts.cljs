(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xf26b4d3a3bd78de448f57e624cb6cfca7fd62167"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x727ab6b96050bba283ab8b8e6847b79dfdf242be"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xcd7f329b58ed112cf4a02317bbb948f342ad941d"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x6a8061033eec8d5b3b6aa569a3707e4ec8e7bd51"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x8b7ff30179734d27abd979c49f13dd65824c318f"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0xcdeab4c6f18cb95794affd6091031d0629405687"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xc26fee3754b399c5f45971bbbb31265b04baa6eb"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x4ce05ff22d2453d049b308cdfa9b6568b5e1d240"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x1dd976ff6e19e4527e7c985b9a0b1aea89672cd9"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})