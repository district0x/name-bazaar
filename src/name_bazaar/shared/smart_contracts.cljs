(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x7a5b55cfda09c90f91c415e18296a7dd8ffffcce"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0xfebb05b53ff330016803445c4e6f90fb38dcb0a7"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x3b3eef4c6d736036dd32f6ee10cb0c6bea7021bd"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0xf52ec19f4072b1954d742243ab75696f232d7dc3"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xc43e6fa04c774c46ef78c6644bdbe4a0ef69857c"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0xc2002d7ba143e5fdcfca9d6783a6e02b4b664faa"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xf218058af8ae63bebd62da05d179f371c73cb007"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x999b6bcce0ccfdd689834959f02cd30177260187"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x05ec0e9ca082c28e14f9e1413efe735a908e4d47"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x7aabe7b26f2800c0192a183d6678d6c963a83e77"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})