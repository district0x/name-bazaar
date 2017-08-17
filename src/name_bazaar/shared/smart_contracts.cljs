(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x170adc3e8e900ac0118e8795c08b48cc24ae7e0b"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x43b21184e98dbf7d14141c971e6026e1b082095a"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xa381abb6137e1e3884512a455338c2d88c088464"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x0de2de4d24cd9e40d3f74412c1abbf1ab96c8223"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x39fdfbea45ffddabe41a27f73fca6e5dbd625c7f"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0x2a10e8d12ceb97096f60c4cc369eb2a050510eed"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xf612ae3cd8a0fc207590d8212195e9c1a486038f"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x38a60889eeaea7567331a2d173df0ad0fe629004"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xd0de9eaa0a0b1b14fc963c244aeb79368c50bb90"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})