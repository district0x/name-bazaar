(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xb6ead31aa344d6275d3deff72b61a2d18ce4234e"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x367decc92cbc3054d597c8314475c2f9c9aab47a"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xa18620eb0561ffdb5141811eab763ef042c48c1e"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x5d6bf2fdbbac66afcf9472d2b4d0d4f3bb828992"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x132e4c91077eca527a4c8136e2c51e13379fd741"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xce53d7f8ac021f718f729f01342ccf71fb61669d"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0xcbe76522ee0026fe6fa27e4658ab8f45faa3198b"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xd7dd68ab5ef45cbb2065c15d54cd94ec21529971"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x5eea16adfb327aee322220b2f3924e6c96cb6e85"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x4a2e9d9d05e0799429da5431367ec6b20a3e9d5d"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x8936ff910fc6244573d5b19683c5423e38cdfd49"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0xa1b542a0b2a91e988819f7b128eefbaae3f3a6e3"}})
