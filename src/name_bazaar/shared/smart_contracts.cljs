(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xfd23b66fd49d6b58acb2095c93417666186e4164"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x367decc92cbc3054d597c8314475c2f9c9aab47a"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xc2b9e8b2338737d3996eb09c624f85a96639978e"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x132e4c91077eca527a4c8136e2c51e13379fd741"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x786e1831b0abb38f33f5d8486c21b012800a1940"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0x43ace52907c10b649cd0dee6bfa0f5d1fe1bc8c2"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x93153561a6b940475fc017a18c762825cef6916d"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x4e85d0419c663a6eb45a0e35c2195fd87d1ef4bb"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x0718093c17db4caee44ae87c0f91ba19c9a99258"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x8936ff910fc6244573d5b19683c5423e38cdfd49"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})