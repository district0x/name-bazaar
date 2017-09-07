(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x8d201acf483749d0361e8ccee00e0bfa5f262fe7"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x424e05348cc8be381cdae8cf119a4f49e1e380d5"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x1f35e2a74aa533952892136b8a7dec5aa4d21afd"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0xbfdd11654fbca97979b182cf0e3934a214fcb0ea"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x22599a293757c398e9ff7647f903bf5a0a6077cc"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0xc96b5172d7906e6ad04ac5e360952d9818e5b818"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x5bd77c334dc4ec4956998a10ac605259d76341cd"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x74893a4851a4c57fdd2d6bf07bbd177664c0b5a5"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x5634ac61890bd92c4843d954a739ab813b13ea82"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})