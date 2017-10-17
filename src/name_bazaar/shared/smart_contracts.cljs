(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x3d6ff97d806335135e77897a094a60f4864c443a"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x367decc92cbc3054d597c8314475c2f9c9aab47a"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x6cc23b8aa5d2650625cab5a1dd8347fad87adc5c"},
 :registrar
 {:name "Registrar",
  :address "0x542d337975c2f9b436621024df371244bd53a0af"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x2df1206c4f6a47b8b94187f177e141267c8423e1"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x132e4c91077eca527a4c8136e2c51e13379fd741"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0xdce174ba4005b90ba87c8266184d3fd6a124978b"},
 :mock-registrar
 {:name "MockRegistrar",
  :address "0xcf05ff7cea2632b9d977dca08ff9a8149d909dc9"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x7e28999f97d38ee2e165e6a44eb7470e482737d1"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x255830634f13749804a105571e873a61d098e039"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x2760719f88666eceb5ad3b2b76de50140a9204c8"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0x8936ff910fc6244573d5b19683c5423e38cdfd49"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x7734f1067c48acd9ab836fd0c6d99b40ab9980c5"}})