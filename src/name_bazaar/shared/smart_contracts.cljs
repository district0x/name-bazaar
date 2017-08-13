(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0xfb07323f115fecd3220dd7feb9358ab29e2d337f"},
 :buy-now-offering-library
 {:name "BuyNowOfferingLibrary",
  :address "0xe99a6f981c55d696b19276ec1d92c11d203afd59"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xd6915c0876be3afd34b577e88f5045e27bfd87b6"},
 :registrar
 {:name "MockRegistrar",
  :address "0x5b0e95d21a1e3756c61ad766d69f1a9f214d8451"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x0000000000000000000000000000000000000000"},
 :auction-offering-library
 {:name "AuctionOfferingLibrary",
  :address "0x7167eb3e31fc6c8ad7a990d5ea3cc2128d3ed397"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :ens
 {:name "ENS", :address "0x48fcaddee3e71cf799080032e68d990d5454bfbc"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xb1a2190418f231f46ca36030391cbb5f2eb86606"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0000000000000000000000000000000000000000"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0xba882b3a206a6cabfed3a0b4b95c44071085bae5"},
 :offering-library
 {:name "OfferingLibrary",
  :address "0xbc59338f94ebf5d04efe4ad717694a91a228d8ee"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0000000000000000000000000000000000000000"}})