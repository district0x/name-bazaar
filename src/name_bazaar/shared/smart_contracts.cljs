(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xdbeed0aaa4982ce2bc246424bc8a37f9c0dde786"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0x130643ce0901fdb15d44e2f2f3e2e3b746180c6a"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x6ad5456b089d38f53ba6219302d56987dfe407cc"},
 :registrar
 {:name "MockRegistrar",
  :address "0x74c6b7b525d138fb97c7674569db956329f3a4ca"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0xb2860a7758fc0ef007720eee238e3befc6b161fd"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x6691f62646564519fa869de3ab1fd394630fef5f"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xc6aa8977fc240ab1d49de9aa6029f08e35eaa141"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x58ed27a464b6f1bc04d82d7c96f8c70e3b533e73"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xbec294bec362e1cac1e4b881d9acdd62acfa821c"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})