(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x1685af12083a2ee8e378fc1da89280894122c192"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x713c33a6ac78cd42a871c1eea076d28b49633211"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x72f58da25afc808b6b9e3b950c4a93a5a0c3b452"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0xa761f23dbce95e4d05ee9fa1c29554486572edd7"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x79b153963f911faac5a2c08a0d44b5f45f026589"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0x8cc36bf5f581dc5f0e9ebdd4ad95cce7bec277e9"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x96f70f69d4c419b3cf0f2e88f6bae6dc1315a35c"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0xa34c5c7efe81af784905eea7a12ccf4aaaa0c920"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x06da6e4e92bfd4f6bb2c0fdd84e41b87b517d330"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})